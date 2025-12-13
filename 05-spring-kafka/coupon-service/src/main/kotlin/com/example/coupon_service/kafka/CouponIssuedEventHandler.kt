package com.example.coupon_service.kafka

import com.example.coupon_service.dto.CouponIssuedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CouponIssuedEventHandler(
    private val couponIssuedEventProducer: CouponIssuedEventProducer
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: CouponIssuedEvent) {
        couponIssuedEventProducer.sendEvent(event)
    }
}