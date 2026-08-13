package dev.alexeev.order_service.mapper;

import dev.alexeev.order_service.dto.response.OrderItemResponse;
import dev.alexeev.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

  @Mapping(target = "itemId", source = "item.id")
  @Mapping(target = "itemName", source = "item.name")
  @Mapping(target = "itemPrice", source = "item.price")
  OrderItemResponse toResponse(OrderItem orderItem);
}