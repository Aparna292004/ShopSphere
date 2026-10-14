package com.shopsphere.shopsphere.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

   @NotEmpty(message = "Order must contain at least one item")

    private List<@Valid OrderItemRequest> items;
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
}