package com.example.coupon_service.controller

import com.example.coupon_service.com.example.coupon_service.ApiResponse
import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.service.CouponIssueService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponIssueController(
    private val couponIssueService: CouponIssueService
) {
    // 쿠폰발급
    @PostMapping("/{userId}/issue")
    fun issueCoupon(@PathVariable userId: Long): ApiResponse<Coupon?> {
        val coupon = couponIssueService.issueCoupon(userId)
        return ApiResponse.success(coupon)
    }
}