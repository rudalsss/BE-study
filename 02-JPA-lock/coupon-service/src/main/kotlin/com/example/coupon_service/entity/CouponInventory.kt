package com.example.coupon_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import jakarta.persistence.Version
import java.time.LocalDateTime

@Entity
@Table(name = "coupon_inventory")
data class CouponInventory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, unique = true)
    val name: String,

    @Column(name = "total_coupons", nullable = false)
    val totalCouponCount: Int,

    @Column(name = "distributed", nullable = false)
    var assignedCouponCount: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0 // 버전 필드
)