package dev.alexeev.order_service.controller;

import dev.alexeev.order_service.dto.request.OrderCreateRequest;
import dev.alexeev.order_service.dto.request.OrderFilterRequest;
import dev.alexeev.order_service.dto.request.OrderUpdateRequest;
import dev.alexeev.order_service.dto.response.OrderResponse;
import dev.alexeev.order_service.entity.OrderStatus;
import dev.alexeev.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
    OrderResponse created = orderService.createOrder(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
    return ResponseEntity.ok(orderService.getOrderById(id));
  }

  @GetMapping
  public ResponseEntity<Page<OrderResponse>> getOrders(
          @RequestParam(required = false) LocalDateTime createdFrom,
          @RequestParam(required = false) LocalDateTime createdTo,
          @RequestParam(required = false) List<OrderStatus> statuses,
          Pageable pageable) {
    OrderFilterRequest filter = OrderFilterRequest.builder()
            .createdFrom(createdFrom)
            .createdTo(createdTo)
            .statuses(statuses)
            .build();
    return ResponseEntity.ok(orderService.getOrders(filter, pageable));
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<Page<OrderResponse>> getOrdersByUserId(@PathVariable Long userId,
                                                               Pageable pageable) {
    return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id,
                                                   @Valid @RequestBody OrderUpdateRequest request) {
    return ResponseEntity.ok(orderService.updateOrder(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
    orderService.deleteOrder(id);
    return ResponseEntity.noContent().build();
  }
}