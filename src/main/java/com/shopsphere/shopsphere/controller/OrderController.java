package com.shopsphere.shopsphere.controller;

import com.shopsphere.shopsphere.dto.request.CreateOrderRequest;
import com.shopsphere.shopsphere.dto.request.UpdateOrderStatusRequest;
import com.shopsphere.shopsphere.dto.response.OrderResponse;
import com.shopsphere.shopsphere.security.UserPrincipal;
import com.shopsphere.shopsphere.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {

        return ResponseEntity.ok(orderService.getMyOrders(principal.getId(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(orderService.getById(id, principal.getId(), isAdmin));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}