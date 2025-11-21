package com.example.coupon_service.service

import com.example.coupon_service.MockConvenienceStoreUtil
import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.repository.CouponRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class CouponIssueService(
    private val redisTemplate: StringRedisTemplate,
    private val couponRepository: CouponRepository
) {

    private val USER_COUPON_KEY = "rudals:user:coupon:"
    private val INVENTORY_KEY = "rudals:coupon:available"

    fun issueCoupon(userId: Long): Coupon? {
        val userKey = "$USER_COUPON_KEY$userId"

        // Already check : 캐시확인후 존재하면 그대로 반환
        val get = redisTemplate.opsForValue().get(userKey)
        if( get != null ) return couponRepository.findByCode(get)
            ?: error("쿠폰 정보가 Redis에는 있는데 DB에는 없습니다.")

        // Valid check : 재고체크(LPOP)
        val couponCode = redisTemplate.opsForList().leftPop(INVENTORY_KEY)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰 재고가 모두 소진되었습니다.") // invalid 404

        // 쿠폰발급 -> redis에 저장
        redisTemplate.opsForValue().set(userKey, couponCode)

        // 반환
        return couponRepository.findByCode(couponCode)
            ?: error("DB에서 해당 쿠폰을 찾을 수 없습니다.")
    }
}