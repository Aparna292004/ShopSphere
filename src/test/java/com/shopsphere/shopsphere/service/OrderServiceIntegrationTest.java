
package com.shopsphere.shopsphere.service;

import com.shopsphere.shopsphere.dto.request.CreateOrderRequest;
import com.shopsphere.shopsphere.dto.request.OrderItemRequest;
import com.shopsphere.shopsphere.entity.Category;
import com.shopsphere.shopsphere.entity.Order;
import com.shopsphere.shopsphere.entity.Product;
import com.shopsphere.shopsphere.entity.Role;
import com.shopsphere.shopsphere.entity.User;
import com.shopsphere.shopsphere.repository.CategoryRepository;
import com.shopsphere.shopsphere.repository.OrderRepository;
import com.shopsphere.shopsphere.repository.ProductRepository;
import com.shopsphere.shopsphere.repository.UserRepository;
import com.shopsphere.shopsphere.exception.InsufficientStockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createOrderShouldSaveOrderAndReduceStock() {

        // Create a test user.
        User user = userRepository.save(
                User.builder()
                        .fullName("Integration Test User")
                        .email("integration@test.com")
                        .password("test-password")
                        .roles(Set.of(Role.ROLE_CUSTOMER))
                        .enabled(true)
                        .build()
        );

        // Create a test category.
        Category category = categoryRepository.save(
                Category.builder()
                        .name("Integration Category")
                        .description("Category for integration testing")
                        .build()
        );

        // Create a product with 5 units of stock.
        Product product = productRepository.save(
                Product.builder()
                        .name("Integration Product")
                        .description("Product for integration testing")
                        .price(new BigDecimal("100.00"))
                        .stockQuantity(5)
                        .category(category)
                        .build()
        );

        // Request to purchase 2 units.
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest));
        request.setShippingAddress("Bangalore");

        // Call the real service.
        var response = orderService.createOrder(user.getId(), request);

        // Verify that an order was created.
        assertNotNull(response);
        assertNotNull(response.getId());

        // Verify the order total.
        assertEquals(
                new BigDecimal("200.00"),
                response.getTotalAmount()
        );

        // Verify that the stock was reduced from 5 to 3.
        Product updatedProduct =
                productRepository.findById(product.getId()).orElseThrow();

        assertEquals(3, updatedProduct.getStockQuantity());

        // Verify that the order exists in the database.
        Order savedOrder =
                orderRepository.findById(response.getId()).orElseThrow();

        assertEquals(user.getId(), savedOrder.getUser().getId());
        assertEquals(
                new BigDecimal("200.00"),
                savedOrder.getTotalAmount()
        );
    }

    @Test
    void createOrderShouldRejectWhenStockIsInsufficient() {

        User user = userRepository.save(
                User.builder()
                        .fullName("Stock Test User")
                        .email("stock@test.com")
                        .password("test-password")
                        .roles(Set.of(Role.ROLE_CUSTOMER))
                        .enabled(true)
                        .build()
        );

        Category category = categoryRepository.save(
                Category.builder()
                        .name("Stock Test Category")
                        .description("Category for stock testing")
                        .build()
        );

        Product product = productRepository.save(
                Product.builder()
                        .name("Limited Stock Product")
                        .description("Product with limited stock")
                        .price(new BigDecimal("50.00"))
                        .stockQuantity(1)
                        .category(category)
                        .build()
        );

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest));
        request.setShippingAddress("Bangalore");

        // The order should fail because only 1 item is available.
        org.junit.jupiter.api.Assertions.assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(user.getId(), request)
        );

        // Stock should remain unchanged.
        Product unchangedProduct =
                productRepository.findById(product.getId()).orElseThrow();

        assertEquals(1, unchangedProduct.getStockQuantity());

        // No order should have been created.
        assertEquals(0, orderRepository.count());
    }
}
