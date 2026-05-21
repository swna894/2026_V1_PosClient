package com.swna.javafx.pos.dto.response;

import java.math.BigDecimal;

public record SaleResponse(
        Long id,
        String receiptNo,
        String status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        BigDecimal costAmount
) {}