package com.example.coupon_service.service

import com.example.coupon_service.dto.CouponIssuedEvent
import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.repository.CouponRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun issueCoupon(code: String): Coupon {
        try {
            val coupon = Coupon(code = code)
            val saved = couponRepository.save(coupon)

            // 발급성공후 kafka 이벤트 발행을 예약
            applicationEventPublisher.publishEvent(
                CouponIssuedEvent(
                    couponId = saved.id,
                    code = saved.code
                )
            )

            return saved

        } catch (e: DataIntegrityViolationException) { // 중복코드 방지
            throw IllegalArgumentException("이미 존재하는 쿠폰 코드입니다: $code")
        }
    }

    fun getCoupon(code: String): Coupon? {
        return couponRepository.findByCode(code)
    }

    fun getAllCoupons(): List<Coupon> {
        return couponRepository.findAll()
    }
}