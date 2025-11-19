package com.example.coupon_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication

@SpringBootApplication
class CouponServiceApplication

fun main(args: Array<String>) {
	runApplication<CouponServiceApplication>(*args)
}
