package com.swna.javafx.pos.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaleItemRequest(

    @NotNull(message = "Product ID is required.")
    Long id,

    @NotBlank(message = "Barcode cannot be empty.")
    String barcode,

    @Min(value = 1, message = "Quantity must be at least 1.")
    int quantity,

    @NotNull(message = "Discount value is required. (Use 0 if none)")
    BigDecimal discountValue,

    @NotNull(message = "Discount type is required. (e.g., AMOUNT or PERCENT)")
    DiscountType discountType, //[cite: 2]

    @Size(max = 255, message = "Comment must not exceed 255 characters.")
    String comment
    
) {}
