package com.example.coupon_service.repository

import com.example.coupon_service.entity.CouponInventory
import org.springframework.data.jpa.repository.JpaRepository

interface CouponInventoryRepository : JpaRepository<CouponInventory, Long> {
}