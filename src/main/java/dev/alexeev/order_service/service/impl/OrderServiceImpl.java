package dev.alexeev.order_service.service.impl;

import dev.alexeev.order_service.dto.request.OrderCreateRequest;
import dev.alexeev.order_service.dto.request.OrderFilterRequest;
import dev.alexeev.order_service.dto.request.OrderItemRequest;
import dev.alexeev.order_service.dto.request.OrderUpdateRequest;
import dev.alexeev.order_service.dto.response.OrderResponse;
import dev.alexeev.order_service.dto.response.UserInfoResponse;
import dev.alexeev.order_service.entity.Item;
import dev.alexeev.order_service.entity.Order;
import dev.alexeev.order_service.entity.OrderItem;
import dev.alexeev.order_service.entity.OrderStatus;
import dev.alexeev.order_service.exception.ItemNotFoundException;
import dev.alexeev.order_service.exception.OrderNotFoundException;
import dev.alexeev.order_service.mapper.OrderMapper;
import dev.alexeev.order_service.repository.ItemRepository;
import dev.alexeev.order_service.repository.OrderRepository;
import dev.alexeev.order_service.repository.specification.OrderSpecification;
import dev.alexeev.order_service.service.OrderService;
import dev.alexeev.order_service.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final OrderMapper orderMapper;
  private final UserServiceClient userServiceClient;

  @Override
  @Transactional
  public OrderResponse createOrder(OrderCreateRequest request) {
    Order order = Order.builder()
            .userId(request.getUserId())
            .status(OrderStatus.NEW)
            .deleted(false)
            .build();

    attachItems(order, request.getItems());
    order.setTotalPrice(calculateTotalPrice(order));

    Order saved = orderRepository.save(order);
    OrderResponse response = orderMapper.toResponse(saved);
    response.setUserInfo(userServiceClient.getUserInfo(saved.getUserId()).orElse(null));
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public OrderResponse getOrderById(Long id) {
    Order order = orderRepository.findWithItemsById(id)
            .filter(o -> !o.getDeleted())
            .orElseThrow(() -> new OrderNotFoundException(id));

    OrderResponse response = orderMapper.toResponse(order);
    response.setUserInfo(userServiceClient.getUserInfo(order.getUserId()).orElse(null));
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OrderResponse> getOrders(OrderFilterRequest filter, Pageable pageable) {
    var spec = OrderSpecification.withFilters(
            null,
            filter.getCreatedFrom(),
            filter.getCreatedTo(),
            filter.getStatuses()
    );
    Page<OrderResponse> responses = orderRepository.findAll(spec, pageable)
            .map(orderMapper::toResponse);
    enrichWithUserInfo(responses.getContent());
    return responses;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
    var spec = OrderSpecification.withFilters(userId, null, null, null);
    Page<OrderResponse> responses = orderRepository.findAll(spec, pageable)
            .map(orderMapper::toResponse);
    enrichWithUserInfo(responses.getContent());
    return responses;
  }

  @Override
  @Transactional
  public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
    Order order = orderRepository.findWithItemsById(id)
            .filter(o -> !o.getDeleted())
            .orElseThrow(() -> new OrderNotFoundException(id));

    if (request.getStatus() != null) {
      order.setStatus(request.getStatus());
    }

    if (request.getItems() != null) {
      order.getOrderItems().clear();
      attachItems(order, request.getItems());
      order.setTotalPrice(calculateTotalPrice(order));
    }

    Order updated = orderRepository.save(order);
    OrderResponse response = orderMapper.toResponse(updated);
    response.setUserInfo(userServiceClient.getUserInfo(updated.getUserId()).orElse(null));
    return response;
  }

  @Override
  @Transactional
  public void deleteOrder(Long id) {
    Order order = orderRepository.findById(id)
            .filter(o -> !o.getDeleted())
            .orElseThrow(() -> new OrderNotFoundException(id));

    order.setDeleted(true);
    orderRepository.save(order);
  }

  private void enrichWithUserInfo(List<OrderResponse> orders) {
    if (orders.isEmpty()) {
      return;
    }
    List<Long> userIds = orders.stream()
            .map(OrderResponse::getUserId)
            .distinct()
            .toList();

    Map<Long, UserInfoResponse> usersById = userServiceClient.getUsersInfo(userIds);

    for (OrderResponse order : orders) {
      order.setUserInfo(usersById.get(order.getUserId()));
    }
  }

  private void attachItems(Order order, List<OrderItemRequest> itemRequests) {
    List<Long> itemIds = itemRequests.stream()
            .map(OrderItemRequest::getItemId)
            .toList();

    Map<Long, Item> itemsById = itemRepository.findAllById(itemIds).stream()
            .collect(Collectors.toMap(Item::getId, item -> item));

    for (OrderItemRequest itemRequest : itemRequests) {
      Item item = itemsById.get(itemRequest.getItemId());
      if (item == null) {
        throw new ItemNotFoundException(itemRequest.getItemId());
      }

      OrderItem orderItem = OrderItem.builder()
              .item(item)
              .quantity(itemRequest.getQuantity())
              .build();

      order.addOrderItem(orderItem);
    }
  }

  private BigDecimal calculateTotalPrice(Order order) {
    return order.getOrderItems().stream()
            .map(oi -> oi.getItem().getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}