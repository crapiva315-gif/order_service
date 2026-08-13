package dev.alexeev.order_service.client;

import dev.alexeev.order_service.dto.response.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "userService", url = "${user-service.url}")
public interface UserServiceFeignClient {

  @GetMapping("/api/v1/users/{id}")
  UserInfoResponse getUserById(@PathVariable("id") Long id);

  @GetMapping("/api/v1/users/batch")
  List<UserInfoResponse> getUsersByIds(@RequestParam("ids") List<Long> ids);
}