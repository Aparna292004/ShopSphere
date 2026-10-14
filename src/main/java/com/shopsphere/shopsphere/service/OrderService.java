package com.shopsphere.shopsphere.service;

import com.shopsphere.shopsphere.dto.request.CreateOrderRequest;
import com.shopsphere.shopsphere.dto.request.OrderItemRequest;
import com.shopsphere.shopsphere.dto.request.UpdateOrderStatusRequest;
import com.shopsphere.shopsphere.dto.response.OrderItemResponse;
import com.shopsphere.shopsphere.dto.response.OrderResponse;
import com.shopsphere.shopsphere.entity.*;
import com.shopsphere.shopsphere.repository.OrderRepository;
import com.shopsphere.shopsphere.repository.ProductRepository;
import com.shopsphere.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shopsphere.shopsphere.exception.InsufficientStockException;
import com.shopsphere.shopsphere.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            // Pessimistic lock: physically locks this product's row until the
            // transaction commits, forcing any concurrent order for the same
            // product to wait rather than racing on the stock count.
            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + itemRequest.getProductId()));

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                            "Insufficient stock for product '" + product.getName() + "'. "
                                + "Available: " + product.getStockQuantity()
                                + ", requested: " + itemRequest.getQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPriceAtPurchase(product.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .shippingAddress(request.getShippingAddress())
                .build();

        // Link each item back to the parent order before saving, since the
        // foreign key lives on the OrderItem side of the relationship.
        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    public OrderResponse getById(Long id, Long requestingUserId, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (!isAdmin && !order.getUser().getId().equals(requestingUserId)) {
            throw new IllegalStateException("You do not have access to this order.");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        order.setStatus(request.getStatus());
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPriceAtPurchase(item.getUnitPriceAtPurchase())
                        .lineTotal(item.getUnitPriceAtPurchase()
                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }
}