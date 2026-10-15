package dev.alexeev.order_service.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String paymentId,
        String orderId,
        String userId,
        String status,
        BigDecimal paymentAmount
) {
    public static final String CREATE_PAYMENT = "CREATE_PAYMENT";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}