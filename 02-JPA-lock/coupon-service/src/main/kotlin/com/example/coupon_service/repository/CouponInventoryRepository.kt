package com.example.coupon_service.repository

import com.example.coupon_service.entity.CouponInventory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface CouponInventoryRepository : JpaRepository<CouponInventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CouponInventory i WHERE i.id = :couponId")
    fun findForUpdate(@Param("couponId") couponId: Long): Optional<CouponInventory>
}