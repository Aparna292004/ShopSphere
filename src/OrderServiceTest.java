package com.shopsphere.shopsphere.service;

import com.shopsphere.shopsphere.entity.Order;
import com.shopsphere.shopsphere.entity.OrderItem;
import com.shopsphere.shopsphere.entity.Product;
import com.shopsphere.shopsphere.entity.User;
import com.shopsphere.shopsphere.repository.OrderRepository;
import com.shopsphere.shopsphere.repository.ProductRepository;
import com.shopsphere.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void getAllOrdersShouldReturnOrders() {

        // Create mock objects instead of using the real database.
        User user = org.mockito.Mockito.mock(User.class);
        Order order = org.mockito.Mockito.mock(Order.class);
        OrderItem orderItem = org.mockito.Mockito.mock(OrderItem.class);
        Product product = org.mockito.Mockito.mock(Product.class);

        // Prepare the relationships required by OrderService.toResponse().
        when(user.getId()).thenReturn(1L);

        when(order.getId()).thenReturn(100L);
        when(order.getUser()).thenReturn(user);
        when(order.getItems()).thenReturn(List.of(orderItem));
        when(order.getTotalAmount()).thenReturn(new BigDecimal("500.00"));
        when(order.getShippingAddress()).thenReturn("Bangalore");

        when(orderItem.getProduct()).thenReturn(product);
        when(orderItem.getQuantity()).thenReturn(2);
        when(orderItem.getUnitPriceAtPurchase())
                .thenReturn(new BigDecimal("250.00"));

        when(product.getId()).thenReturn(10L);
        when(product.getName()).thenReturn("Laptop");

        Page<Order> orderPage =
                new PageImpl<>(List.of(order));

        PageRequest pageable = PageRequest.of(0, 10);

        // Tell the mocked repository what it should return.
        when(orderRepository.findAll(pageable))
                .thenReturn(orderPage);

        // Call the actual service method.
        Page<?> result = orderService.getAllOrders(pageable);

        // Verify the result.
        assertEquals(1, result.getTotalElements());
    }
}

