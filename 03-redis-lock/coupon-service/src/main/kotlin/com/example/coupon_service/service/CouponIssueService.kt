package com.example.coupon_service.service

import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.repository.CouponRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class CouponIssueService(
    private val redisTemplate: StringRedisTemplate,
    private val couponService: CouponService,
    private val couponRepository: CouponRepository
) {

    private val USER_COUPON_KEY = "rudals:user:coupon:"

    fun issueCoupon(userId: Long): Coupon? {
        val key = "$USER_COUPON_KEY$userId"

        // Already check : 캐시확인후 존재하면 그대로 반환
        val get = redisTemplate.opsForValue().get(key)
        if( get != null ) return couponRepository.findByCode(get)
            ?: error("쿠폰 정보가 Redis에는 있는데 DB에는 없습니다.")

        // valid : 쿠폰발급 -> redis, db에 저장
        val newCouponCode = couponService.generateValidCouponCode()
        val newCoupon = couponRepository.save(Coupon(code = newCouponCode))
        redisTemplate.opsForValue().set(key, newCoupon.code)

        // 반환
        return newCoupon
    }
}