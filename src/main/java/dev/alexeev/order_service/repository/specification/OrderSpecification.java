package dev.alexeev.order_service.repository.specification;

import dev.alexeev.order_service.entity.Order;
import dev.alexeev.order_service.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public final class OrderSpecification {

  private OrderSpecification() {
  }

  public static Specification<Order> notDeleted() {
    return (root, query, cb) -> cb.isFalse(root.get("deleted"));
  }

  public static Specification<Order> hasUserId(Long userId) {
    return (root, query, cb) ->
            userId == null ? null : cb.equal(root.get("userId"), userId);
  }

  public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
    return (root, query, cb) -> {
      if (from != null && to != null) {
        return cb.between(root.get("createdAt"), from, to);
      }
      if (from != null) {
        return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
      }
      if (to != null) {
        return cb.lessThanOrEqualTo(root.get("createdAt"), to);
      }
      return null;
    };
  }

  public static Specification<Order> hasStatuses(List<OrderStatus> statuses) {
    return (root, query, cb) ->
            (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
  }

  public static Specification<Order> withFilters(Long userId, LocalDateTime from,
                                                 LocalDateTime to, List<OrderStatus> statuses) {
    return Specification.allOf(
            notDeleted(),
            hasUserId(userId),
            createdBetween(from, to),
            hasStatuses(statuses)
    );
  }
}