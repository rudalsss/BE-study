package com.example.coupon_service.dto

data class CouponIssuedEvent(
    val couponId: Long,
    val code: String
)