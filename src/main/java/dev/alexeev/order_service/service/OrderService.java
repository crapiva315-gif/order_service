package dev.alexeev.order_service.service;

import dev.alexeev.order_service.dto.request.OrderCreateRequest;
import dev.alexeev.order_service.dto.request.OrderFilterRequest;
import dev.alexeev.order_service.dto.request.OrderUpdateRequest;
import dev.alexeev.order_service.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderResponse createOrder(OrderCreateRequest request);

  OrderResponse getOrderById(Long id);

  Page<OrderResponse> getOrders(OrderFilterRequest filter, Pageable pageable);

  Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable);

  OrderResponse updateOrder(Long id, OrderUpdateRequest request);

  void deleteOrder(Long id);
}