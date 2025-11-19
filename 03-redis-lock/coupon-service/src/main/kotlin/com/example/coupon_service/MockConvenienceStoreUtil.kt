package com.example.coupon_service

object MockConvenienceStoreUtil {
    private val random = java.util.Random()

    /**
     * Simulates sending a coupon code to the convenience store.
     * Throws an exception if the operation fails.
     */
    fun sendCouponCodeToStore(couponCode: String) {
        // Simulate 10% failure rate
        if (random.nextInt(100) < 10) {
            throw IllegalStateException("Convenience store API failed for coupon code $couponCode")
        }
    }
}