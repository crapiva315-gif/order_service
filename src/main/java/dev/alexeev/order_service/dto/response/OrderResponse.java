package dev.alexeev.order_service.dto.response;

import dev.alexeev.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

  private Long id;
  private Long userId;
  private OrderStatus status;
  private BigDecimal totalPrice;
  private List<OrderItemResponse> items;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // может быть null, если user-service недоступен (circuit breaker fallback)
  private UserInfoResponse userInfo;
}