package dev.alexeev.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
  private Long id;
  private Long itemId;
  private String itemName;
  private BigDecimal itemPrice;
  private Integer quantity;
}