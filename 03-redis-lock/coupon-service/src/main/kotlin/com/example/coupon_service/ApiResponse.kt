package com.example.coupon_service.com.example.coupon_service

class ApiResponse<T>(
    val success: Boolean,
    val message: String?= null,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }

        fun <T> error(message: String, data: T? = null): ApiResponse<T> {
            return ApiResponse(success = false, message = message, data = data)
        }
    }
}