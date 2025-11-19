package com.example.coupon_service.controller

import com.example.coupon_service.com.example.coupon_service.ApiResponse
import com.example.coupon_service.entity.Coupon
import com.example.coupon_service.service.CouponService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponControlle(
    private val couponService: CouponService
) {
    // 쿠폰발급
    @PostMapping("/add")
    fun addCoupons(@RequestBody request: AddCouponsRequest): ApiResponse<Boolean> {
        couponService.addCoupons(request.couponCodes)
        return ApiResponse.success(true)
    }

    // 쿠폰수정
    @PutMapping("/{couponId}")
    fun updateCoupon(
        @PathVariable couponId: Long,
        @RequestParam newCouponCode: String
    ): ApiResponse<Boolean> {
        couponService.updateCoupon(couponId, newCouponCode)
        return ApiResponse.success(true)
    }

    // 대규모 쿠폰 세팅
    @PostMapping("/generate")
    fun generate() : ApiResponse<Boolean> {
        couponService.generateInitialCoupons(1000)
        return ApiResponse.success(true)
    }

    data class AddCouponsRequest(
        val couponCodes: List<String>
    )
}