package com.example.coupon_service.controller

import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.service.CouponService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val couponService: CouponService
) {
    // 쿠폰 발급
    @PostMapping
    fun issueCoupon(@RequestParam code: String): Coupon {
        return couponService.issueCoupon(code)
    }

    // 쿠폰 단건 조회
    @GetMapping("/{code}")
    fun getCoupon(@PathVariable code: String): Coupon? {
        return couponService.getCoupon(code)
    }

    // 모든 쿠폰 조회
    @GetMapping
    fun getAllCoupons(): List<Coupon> {
        return couponService.getAllCoupons()
    }
}