package dev.alexeev.order_service.entity;

public enum OrderStatus {
  NEW,
  PAID,
  PROCESSING,
  SHIPPED,
  DELIVERED,
  CANCELLED
}