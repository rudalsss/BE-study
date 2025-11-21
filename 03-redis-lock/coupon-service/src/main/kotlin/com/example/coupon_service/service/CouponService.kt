package com.example.coupon_service.service

import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.repository.CouponRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val redisTemplate: StringRedisTemplate
) {
    private val INVENTORY_KEY = "rudals:coupon:available"

    @Transactional
    fun addCoupons(couponCodes: List<String>) {
        for (code in couponCodes) {
            if (couponRepository.existsByCode(code)) {
                throw IllegalArgumentException("중복된 코드입니다")
            }
        }
        couponRepository.saveAll(couponCodes.map { Coupon(code = it) })
        redisTemplate.opsForList().rightPushAll(INVENTORY_KEY, couponCodes)
    }

    @Transactional
    fun updateCoupon(couponId: Long, newCouponCode: String) {
        // 중복코드검사
        if(couponRepository.existsByCode(newCouponCode)) {
            throw IllegalArgumentException("이미 존재하는 쿠폰 코드입니다")
        }

        // DB 수정
        val coupon = couponRepository.findById(couponId)
            .orElseThrow{ IllegalArgumentException("해당 쿠폰을 찾을 수 없습니다.") }
        coupon.code = newCouponCode
        couponRepository.save(coupon)
    }

    @Transactional
    fun generateInitialCoupons(count: Int) {
        val codes = (1..count).map { generateValidCouponCode() }
        val coupons = codes.map { Coupon(code = it) }
        couponRepository.saveAll(coupons)
    }

    // 랜덤 10자리 쿠폰코드 생성 + DB중복검증
    private fun generateValidCouponCode(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        var couponCode: String

        do {
            couponCode = (1..10)
                .map { characters.random() }
                .joinToString("")
        } while (couponRepository.existsByCode(couponCode))

        return couponCode
    }

    fun pushAllToCouponRedis() {
        val codes = couponRepository.findAll().map { it.code } // 저장된 모든 쿠폰코드들
        redisTemplate.delete(INVENTORY_KEY) // 기존 인벤토리 삭제 (중복 방지)
        redisTemplate.opsForList().rightPushAll(INVENTORY_KEY, codes)
    }
}