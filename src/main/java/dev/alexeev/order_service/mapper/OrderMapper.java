package dev.alexeev.order_service.mapper;

import  dev.alexeev.order_service.dto.response.OrderResponse;
import  dev.alexeev.order_service.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

  @Mapping(target = "userInfo", ignore = true)
  @Mapping(target = "items", source = "orderItems")
  OrderResponse toResponse(Order order);
}