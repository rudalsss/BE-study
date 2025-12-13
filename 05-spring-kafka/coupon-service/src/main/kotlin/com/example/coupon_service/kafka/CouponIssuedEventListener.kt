package com.example.coupon_service.kafka

import com.example.coupon_service.dto.CouponIssuedEvent
import com.example.coupon_service.repository.CouponRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CouponIssuedEventListener(
    private val couponRepository: CouponRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @KafkaListener(
        topics = ["coupon-issued"],
        groupId = "coupon-service"
    )
    fun onMessage(message: String) {
        // JSON -> DTO 역직렬화
        val event = objectMapper.readValue(message, CouponIssuedEvent::class.java)

        // 저장된 쿠폰 개수 조회
        val count = couponRepository.count()

        logger.info("[이벤트 수신] 쿠폰등록:coupontId={}, code={} & 쿠폰수={}", event.couponId, event.code, count)
    }
}