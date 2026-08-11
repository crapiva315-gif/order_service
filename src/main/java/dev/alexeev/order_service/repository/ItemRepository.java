package dev.alexeev.order_service.repository;

import dev.alexeev.order_service.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}