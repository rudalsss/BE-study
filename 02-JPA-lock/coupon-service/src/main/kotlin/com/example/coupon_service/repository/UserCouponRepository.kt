package com.example.coupon_service.repository

import com.example.coupon_service.entity.UserCoupon
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserCouponRepository : JpaRepository<UserCoupon, Long> {
    fun findByUserId(userId: Long): UserCoupon?
    fun findByCouponCode(couponCode: String): Optional<UserCoupon>
}