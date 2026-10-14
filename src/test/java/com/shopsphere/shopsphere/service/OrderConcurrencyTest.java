package com.shopsphere.shopsphere.service;

import com.shopsphere.shopsphere.dto.request.CreateOrderRequest;
import com.shopsphere.shopsphere.dto.request.OrderItemRequest;
import com.shopsphere.shopsphere.entity.Category;
import com.shopsphere.shopsphere.entity.Product;
import com.shopsphere.shopsphere.entity.Role;
import com.shopsphere.shopsphere.entity.User;
import com.shopsphere.shopsphere.exception.InsufficientStockException;
import com.shopsphere.shopsphere.repository.CategoryRepository;
import com.shopsphere.shopsphere.repository.OrderItemRepository;
import com.shopsphere.shopsphere.repository.OrderRepository;
import com.shopsphere.shopsphere.repository.ProductRepository;
import com.shopsphere.shopsphere.repository.UserRepository;
import com.shopsphere.shopsphere.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyTest {

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

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void cleanDatabase() {

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void onlyOneOrderShouldSucceedWhenOnlyOneUnitIsAvailable()
            throws Exception {

        User user1 = userRepository.save(
                User.builder()
                        .fullName("Customer One")
                        .email("customer1@test.com")
                        .password("test-password")
                        .roles(Set.of(Role.ROLE_CUSTOMER))
                        .enabled(true)
                        .build()
        );

        User user2 = userRepository.save(
                User.builder()
                        .fullName("Customer Two")
                        .email("customer2@test.com")
                        .password("test-password")
                        .roles(Set.of(Role.ROLE_CUSTOMER))
                        .enabled(true)
                        .build()
        );

        Category category = categoryRepository.save(
                Category.builder()
                        .name("Concurrency Test Category")
                        .description("Used for concurrency testing")
                        .build()
        );

        Product product = productRepository.save(
                Product.builder()
                        .name("Concurrency Test Product")
                        .description("Product used for locking test")
                        .price(new BigDecimal("100.00"))
                        .stockQuantity(1)
                        .category(category)
                        .build()
        );

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest));
        request.setShippingAddress("Test Address");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> firstTask = () -> {

            ready.countDown();
            start.await();

            try {
                orderService.createOrder(user1.getId(), request);
                return true;

            } catch (InsufficientStockException exception) {
                return false;
            }
        };

        Callable<Boolean> secondTask = () -> {

            ready.countDown();
            start.await();

            try {
                orderService.createOrder(user2.getId(), request);
                return true;

            } catch (InsufficientStockException exception) {
                return false;
            }
        };

        Future<Boolean> firstOrder = executor.submit(firstTask);
        Future<Boolean> secondOrder = executor.submit(secondTask);

        ready.await();

        start.countDown();

        boolean firstSucceeded = firstOrder.get();
        boolean secondSucceeded = secondOrder.get();

        executor.shutdown();

        // Exactly one order must succeed.
        assertTrue(
                firstSucceeded ^ secondSucceeded,
                "Exactly one order should succeed"
        );

        // Only one item was available, so stock must now be zero.
        Product updatedProduct =
                productRepository.findById(product.getId()).orElseThrow();

        assertEquals(0, updatedProduct.getStockQuantity());

        // Only one order should have been created.
        assertEquals(1, orderRepository.count());
    }
}