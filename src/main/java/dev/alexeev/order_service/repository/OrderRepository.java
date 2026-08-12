package dev.alexeev.order_service.repository;

import dev.alexeev.order_service.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
  @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
  Optional<Order> findWithItemsById(Long id);
}