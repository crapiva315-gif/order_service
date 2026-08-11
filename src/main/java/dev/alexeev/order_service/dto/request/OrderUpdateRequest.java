package dev.alexeev.order_service.dto.request;

import dev.alexeev.order_service.entity.OrderStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderUpdateRequest {

  // статус можно менять отдельно от позиций - оба поля опциональны,
  // сервис сам решает, что обновлять, если значение null
  private OrderStatus status;

  @Valid
  private List<OrderItemRequest> items;
}