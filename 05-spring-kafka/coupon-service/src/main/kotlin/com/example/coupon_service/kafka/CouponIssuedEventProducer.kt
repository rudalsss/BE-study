package com.example.coupon_service.kafka

import com.example.coupon_service.dto.CouponIssuedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class CouponIssuedEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun sendEvent(event: CouponIssuedEvent) {
        // DTO -> JSON문자열로 직렬화
        val json = objectMapper.writeValueAsString(event)

        // kafka 전송
        kafkaTemplate.send(
            "coupon-issued",
            event.couponId.toString(),
            json
        )

        logger.info("[이벤트 발행] payload={}", json)
    }
}