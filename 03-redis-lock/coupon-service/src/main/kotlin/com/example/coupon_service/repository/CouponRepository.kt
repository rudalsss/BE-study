package com.example.coupon_service.repository

import com.example.coupon_service.entity.Coupon
import org.springframework.data.jpa.repository.JpaRepository

interface CouponRepository : JpaRepository<Coupon, Long> {
    fun existsByCode(code: String): Boolean
}