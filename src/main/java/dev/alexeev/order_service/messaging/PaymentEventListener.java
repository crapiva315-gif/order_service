package dev.alexeev.order_service.messaging;

import dev.alexeev.order_service.event.PaymentEvent;
import dev.alexeev.order_service.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final ObjectMapper objectMapper;
    private final OrderPaymentService orderPaymentService;

    @KafkaListener(topics = "${app.kafka.topics.payment-events}")
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        PaymentEvent event;
        try {
            event = objectMapper.readValue(record.value(), PaymentEvent.class);
        } catch (RuntimeException e) {
            log.error("Skipping unreadable payment event (partition={}, offset={}): {}",
                    record.partition(), record.offset(), e.getMessage());
            return;
        }

        if (!PaymentEvent.CREATE_PAYMENT.equals(event.eventType())) {
            log.debug("Ignoring event of type {} (eventId={})", event.eventType(), event.eventId());
            return;
        }

        log.info("Received {} eventId={} paymentId={} orderId={} status={}",
                event.eventType(), event.eventId(), event.paymentId(), event.orderId(), event.status());
        orderPaymentService.handlePaymentCreated(event); 
    }
}