package dev.alexeev.order_service.entity;

public enum OrderStatus {
  NEW,
  PAYMENT_FAILED,
  PAID,
  PROCESSING,
  SHIPPED,
  DELIVERED,
  CANCELLED
}