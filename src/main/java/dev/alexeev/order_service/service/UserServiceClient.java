package dev.alexeev.order_service.service;

import dev.alexeev.order_service.dto.response.UserInfoResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserServiceClient {

  Optional<UserInfoResponse> getUserInfo(Long userId);

  Map<Long, UserInfoResponse> getUsersInfo(List<Long> userIds);
}
