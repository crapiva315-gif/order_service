package dev.alexeev.order_service.service;

import dev.alexeev.order_service.entity.Order;
import dev.alexeev.order_service.entity.OrderStatus;
import dev.alexeev.order_service.event.PaymentEvent;
import dev.alexeev.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderRepository orderRepository;

    @Transactional
    public void handlePaymentCreated(PaymentEvent event) {
        parseOrderId(event.orderId()).ifPresent(orderId ->
                orderRepository.findById(orderId).ifPresentOrElse(
                        order -> updateOrderState(order, event),
                        () -> log.warn("Order {} from payment {} not found, event ignored", event.orderId(), event.paymentId())
                )
        );
    }

    private void updateOrderState(Order order, PaymentEvent event) {
        OrderStatus current = order.getStatus();

        if (PaymentEvent.STATUS_SUCCESS.equals(event.status())) {
            if (current == OrderStatus.NEW || current == OrderStatus.PAYMENT_FAILED) {
                order.setStatus(OrderStatus.PAID);
                log.info("Order {}: {} -> PAID (payment {})", order.getId(), current, event.paymentId());
            }

        } else if (PaymentEvent.STATUS_FAILED.equals(event.status())) {
            if (current == OrderStatus.NEW) {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                log.info("Order {}: NEW -> PAYMENT_FAILED (payment {})", order.getId(), event.paymentId());
            }
        } else {
            log.warn("Unknown payment status '{}' in event {}, ignored", event.status(), event.eventId());
        }
    }

    private Optional<Long> parseOrderId(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            log.warn("Order id '{}' is not a number, event ignored", raw);
            return Optional.empty();
        }
    }
}