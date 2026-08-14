package dev.alexeev.order_service.service.impl;

import dev.alexeev.order_service.dto.request.OrderCreateRequest;
import dev.alexeev.order_service.dto.request.OrderFilterRequest;
import dev.alexeev.order_service.dto.request.OrderItemRequest;
import dev.alexeev.order_service.dto.request.OrderUpdateRequest;
import dev.alexeev.order_service.dto.response.OrderItemResponse;
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
import dev.alexeev.order_service.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private OrderMapper orderMapper;

  @Mock
  private UserServiceClient userServiceClient;

  @InjectMocks
  private OrderServiceImpl orderService;

  private Item item;
  private Order order;
  private OrderResponse orderResponse;
  private UserInfoResponse userInfoResponse;

  @BeforeEach
  void setUp() {
    item = Item.builder()
            .id(1L)
            .name("Test Item")
            .price(BigDecimal.valueOf(9.99))
            .build();

    OrderItem orderItem = OrderItem.builder()
            .id(1L)
            .item(item)
            .quantity(2)
            .build();

    order = Order.builder()
            .id(1L)
            .userId(1L)
            .status(OrderStatus.NEW)
            .totalPrice(BigDecimal.valueOf(19.98))
            .deleted(false)
            .build();
    order.addOrderItem(orderItem);

    OrderItemResponse orderItemResponse = OrderItemResponse.builder()
            .id(1L)
            .itemId(1L)
            .itemName("Test Item")
            .itemPrice(BigDecimal.valueOf(9.99))
            .quantity(2)
            .build();

    orderResponse = OrderResponse.builder()
            .id(1L)
            .userId(1L)
            .status(OrderStatus.NEW)
            .totalPrice(BigDecimal.valueOf(19.98))
            .items(List.of(orderItemResponse))
            .build();

    userInfoResponse = UserInfoResponse.builder()
            .id(1L)
            .email("alexey@example.com")
            .name("Alexey")
            .surname("Petrov")
            .build();
  }

  // ---------- createOrder ----------

  @Test
  void createOrder_shouldSaveOrderAndEnrichWithUserInfo() {
    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(1L)
            .quantity(2)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(List.of(itemRequest))
            .build();

    when(itemRepository.findAllById(List.of(1L))).thenReturn(List.of(item));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUserInfo(1L)).thenReturn(Optional.of(userInfoResponse));

    OrderResponse result = orderService.createOrder(request);

    assertThat(result.getUserInfo()).isEqualTo(userInfoResponse);
    assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.valueOf(19.98));
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  void createOrder_shouldThrowException_whenItemNotFound() {
    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(999L)
            .quantity(1)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(List.of(itemRequest))
            .build();

    when(itemRepository.findAllById(List.of(999L))).thenReturn(List.of());

    assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(ItemNotFoundException.class);

    verify(orderRepository, never()).save(any());
  }

  @Test
  void createOrder_shouldSetUserInfoNull_whenUserServiceUnavailable() {
    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(1L)
            .quantity(2)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(List.of(itemRequest))
            .build();

    when(itemRepository.findAllById(List.of(1L))).thenReturn(List.of(item));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUserInfo(1L)).thenReturn(Optional.empty());

    OrderResponse result = orderService.createOrder(request);

    assertThat(result.getUserInfo()).isNull();
  }

  // ---------- getOrderById ----------

  @Test
  void getOrderById_shouldReturnOrder_whenExistsAndNotDeleted() {
    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUserInfo(1L)).thenReturn(Optional.of(userInfoResponse));

    OrderResponse result = orderService.getOrderById(1L);

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getUserInfo()).isEqualTo(userInfoResponse);
  }

  @Test
  void getOrderById_shouldThrowException_whenNotFound() {
    when(orderRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getOrderById(999L))
            .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void getOrderById_shouldThrowException_whenSoftDeleted() {
    order.setDeleted(true);
    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.getOrderById(1L))
            .isInstanceOf(OrderNotFoundException.class);
  }

  // ---------- getOrders ----------

  @Test
  void getOrders_shouldReturnPageAndEnrichWithBatchUserInfo() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
    OrderFilterRequest filter = OrderFilterRequest.builder().build();

    when(orderRepository.findAll(ArgumentMatchers.<Specification<Order>>any(), eq(pageable)))
            .thenReturn(orderPage);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUsersInfo(anyList())).thenReturn(Map.of(1L, userInfoResponse));

    Page<OrderResponse> result = orderService.getOrders(filter, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUserInfo()).isEqualTo(userInfoResponse);
    verify(userServiceClient).getUsersInfo(List.of(1L));
  }

  @Test
  void getOrders_shouldNotCallUserService_whenPageIsEmpty() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);
    OrderFilterRequest filter = OrderFilterRequest.builder().build();

    when(orderRepository.findAll(ArgumentMatchers.<Specification<Order>>any(), eq(pageable)))
            .thenReturn(emptyPage);

    Page<OrderResponse> result = orderService.getOrders(filter, pageable);

    assertThat(result.getContent()).isEmpty();
    verify(userServiceClient, never()).getUsersInfo(anyList());
  }

  // ---------- getOrdersByUserId ----------

  @Test
  void getOrdersByUserId_shouldReturnPageAndEnrichWithBatchUserInfo() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

    when(orderRepository.findAll(ArgumentMatchers.<Specification<Order>>any(), eq(pageable)))
            .thenReturn(orderPage);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUsersInfo(List.of(1L))).thenReturn(Map.of(1L, userInfoResponse));

    Page<OrderResponse> result = orderService.getOrdersByUserId(1L, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUserInfo()).isEqualTo(userInfoResponse);
  }

  // ---------- updateOrder ----------

  @Test
  void updateOrder_shouldUpdateStatusOnly_whenItemsNotProvided() {
    OrderUpdateRequest request = OrderUpdateRequest.builder()
            .status(OrderStatus.PAID)
            .build();

    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUserInfo(1L)).thenReturn(Optional.of(userInfoResponse));

    orderService.updateOrder(1L, request);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    verify(itemRepository, never()).findAllById(anyList());
  }

  @Test
  void updateOrder_shouldReplaceItems_whenItemsProvided() {
    Item newItem = Item.builder().id(2L).name("New Item").price(BigDecimal.valueOf(5.0)).build();
    OrderItemRequest newItemRequest = OrderItemRequest.builder().itemId(2L).quantity(3).build();
    OrderUpdateRequest request = OrderUpdateRequest.builder()
            .items(List.of(newItemRequest))
            .build();

    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));
    when(itemRepository.findAllById(List.of(2L))).thenReturn(List.of(newItem));
    when(orderRepository.save(order)).thenReturn(order);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userServiceClient.getUserInfo(1L)).thenReturn(Optional.of(userInfoResponse));

    orderService.updateOrder(1L, request);

    assertThat(order.getOrderItems()).hasSize(1);
    assertThat(order.getOrderItems().get(0).getItem().getId()).isEqualTo(2L);
    assertThat(order.getTotalPrice()).isEqualTo(BigDecimal.valueOf(15.0));
  }

  @Test
  void updateOrder_shouldThrowException_whenOrderNotFound() {
    OrderUpdateRequest request = OrderUpdateRequest.builder().status(OrderStatus.PAID).build();
    when(orderRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.updateOrder(999L, request))
            .isInstanceOf(OrderNotFoundException.class);

    verify(orderRepository, never()).save(any());
  }

  @Test
  void updateOrder_shouldThrowException_whenOrderIsSoftDeleted() {
    order.setDeleted(true);
    OrderUpdateRequest request = OrderUpdateRequest.builder().status(OrderStatus.PAID).build();
    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.updateOrder(1L, request))
            .isInstanceOf(OrderNotFoundException.class);
  }

  // ---------- deleteOrder ----------

  @Test
  void deleteOrder_shouldSetDeletedFlag_whenOrderExists() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    orderService.deleteOrder(1L);

    assertThat(order.getDeleted()).isTrue();
    verify(orderRepository).save(order);
  }

  @Test
  void deleteOrder_shouldThrowException_whenOrderNotFound() {
    when(orderRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.deleteOrder(999L))
            .isInstanceOf(OrderNotFoundException.class);

    verify(orderRepository, never()).save(any());
  }

  @Test
  void deleteOrder_shouldThrowException_whenAlreadyDeleted() {
    order.setDeleted(true);
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.deleteOrder(1L))
            .isInstanceOf(OrderNotFoundException.class);
  }
}