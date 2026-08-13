package dev.alexeev.order_service.service.impl;

import dev.alexeev.order_service.client.UserServiceFeignClient;
import dev.alexeev.order_service.dto.response.UserInfoResponse;
import dev.alexeev.order_service.service.UserServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {

  private final UserServiceFeignClient feignClient;

  @Override
  @CircuitBreaker(name = "userService", fallbackMethod = "getUserInfoFallback")
  public Optional<UserInfoResponse> getUserInfo(Long userId) {
    return Optional.ofNullable(feignClient.getUserById(userId));
  }

  @Override
  @CircuitBreaker(name = "userService", fallbackMethod = "getUsersInfoFallback")
  public Map<Long, UserInfoResponse> getUsersInfo(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return feignClient.getUsersByIds(userIds).stream()
            .collect(Collectors.toMap(UserInfoResponse::getId, u -> u));
  }

  private Optional<UserInfoResponse> getUserInfoFallback(Long userId, Throwable throwable) {
    log.warn("user-service unavailable for userId={}, falling back. Reason: {}",
            userId, throwable.getMessage());
    return Optional.empty();
  }

  private Map<Long, UserInfoResponse> getUsersInfoFallback(List<Long> userIds, Throwable throwable) {
    log.warn("user-service unavailable for batch userIds={}, falling back. Reason: {}",
            userIds, throwable.getMessage());
    return Collections.emptyMap();
  }
}