package com.example.coupon_service.controller

import com.example.coupon_service.ApiResponse
import com.example.coupon_service.entity.UserCoupon
import com.example.coupon_service.service.CouponService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val couponService: CouponService
){
    // 쿠폰발급
    @PostMapping("/{userId}/issue")
    fun issueCoupon(@PathVariable userId: Long): ApiResponse<UserCoupon> {
        val userCoupon = couponService.issueCoupon(userId, 1)
        return ApiResponse.success(userCoupon)
    }

    // 발급받은 쿠폰 조회
    @GetMapping("/{userId}")
    fun getUserCoupon(@PathVariable userId: Long): ApiResponse<UserCoupon> {
        val userCoupon = couponService.findUserCouponByUserId(userId)
        return ApiResponse.success(userCoupon);
    }
}