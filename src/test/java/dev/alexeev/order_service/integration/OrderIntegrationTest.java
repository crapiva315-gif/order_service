package dev.alexeev.order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.alexeev.order_service.dto.request.OrderCreateRequest;
import dev.alexeev.order_service.dto.request.OrderItemRequest;
import dev.alexeev.order_service.entity.Item;
import dev.alexeev.order_service.repository.ItemRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OrderIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  static WireMockServer wireMockServer = new WireMockServer(0);

  @BeforeAll
  static void startWireMock() {
    wireMockServer.start();
    WireMock.configureFor("localhost", wireMockServer.port());
  }

  @AfterAll
  static void stopWireMock() {
    wireMockServer.stop();
  }

  @AfterEach
  void resetWireMock() {
    wireMockServer.resetAll();
  }

  @DynamicPropertySource
  static void registerUserServiceUrl(DynamicPropertyRegistry registry) {
    registry.add("user-service.url", () -> "http://localhost:" + wireMockServer.port());
  }

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

  @Autowired
  private ItemRepository itemRepository;

  @Test
  void createOrder_shouldReturnOrderWithUserInfo_whenUserServiceRespondsSuccessfully() throws Exception {
    Item item = itemRepository.save(Item.builder()
            .name("Integration Test Item")
            .price(BigDecimal.valueOf(15.50))
            .build());

    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/users/1"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {"id":1,"email":"alexey@example.com","name":"Alexey","surname":"Petrov"}
                            """)));

    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(item.getId())
            .quantity(2)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(java.util.List.of(itemRequest))
            .build();

    mockMvc.perform(post("/api/v1/orders")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.totalPrice").value(31.00))
            .andExpect(jsonPath("$.userInfo.email").value("alexey@example.com"))
            .andExpect(jsonPath("$.userInfo.name").value("Alexey"));
  }

  @Test
  void createOrder_shouldReturnOrderWithNullUserInfo_whenUserServiceUnavailable() throws Exception {
    Item item = itemRepository.save(Item.builder()
            .name("Fallback Test Item")
            .price(BigDecimal.valueOf(10.00))
            .build());

    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/users/2"))
            .willReturn(aResponse().withStatus(500)));

    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(item.getId())
            .quantity(1)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(2L)
            .items(java.util.List.of(itemRequest))
            .build();

    mockMvc.perform(post("/api/v1/orders")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userInfo").doesNotExist());
  }

  @Test
  void createOrder_shouldReturnNotFound_whenItemDoesNotExist() throws Exception {
    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(999999L)
            .quantity(1)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(java.util.List.of(itemRequest))
            .build();

    mockMvc.perform(post("/api/v1/orders")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
  }

  @Test
  void getOrderById_shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {
    mockMvc.perform(get("/api/v1/orders/{id}", 999999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Order not found with id: 999999"));
  }

  @Test
  void createOrder_shouldReturnBadRequest_whenItemsEmpty() throws Exception {
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(java.util.List.of())
            .build();

    mockMvc.perform(post("/api/v1/orders")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void deleteOrder_shouldSoftDeleteOrder_andSubsequentGetReturnsNotFound() throws Exception {
    Item item = itemRepository.save(Item.builder()
            .name("Delete Test Item")
            .price(BigDecimal.valueOf(20.00))
            .build());

    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/users/3"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {"id":3,"email":"test@example.com","name":"Test","surname":"User"}
                            """)));

    OrderItemRequest itemRequest = OrderItemRequest.builder()
            .itemId(item.getId())
            .quantity(1)
            .build();
    OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(3L)
            .items(java.util.List.of(itemRequest))
            .build();

    String response = mockMvc.perform(post("/api/v1/orders")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

    Long orderId = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/orders/{id}", orderId))
            .andExpect(status().isNotFound());
  }
}