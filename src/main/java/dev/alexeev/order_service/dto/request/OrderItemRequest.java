package dev.alexeev.order_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

  @NotNull(message = "itemId is required")
  private Long itemId;

  @NotNull(message = "quantity is required")
  @Positive(message = "quantity must be positive")
  private Integer quantity;
}