package com.shopsphere.shopsphere.repository;
import com.shopsphere.shopsphere.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}