package com.swna.javafx.pos.dto;

import java.math.BigDecimal;

/**
 * Refactored SaleItemRequest for Client-Server synchronization.
 */
public record SaleItemRequestDto(
        Long id,
        String barcode,
        BigDecimal sellingPrice,
        BigDecimal unitDiscount,
        int quantity,
        String comment
) {}