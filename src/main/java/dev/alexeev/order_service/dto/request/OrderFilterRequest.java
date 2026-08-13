package dev.alexeev.order_service.dto.request;

import  dev.alexeev.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilterRequest {

  private LocalDateTime createdFrom;
  private LocalDateTime createdTo;
  private List<OrderStatus> statuses;
}