package com.swna.javafx.admin.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record InvoentoryResponse(
        Long id,
        String code,
        String barcode,
        String description,
        String comment,
        BigDecimal price,        // 판매가
        BigDecimal cost,         // 원가
        BigDecimal priceOld,
        BigDecimal costOld,
        int quantity,            // 현재 재고 수량
        int minStock,            // 적정/최소 재고 수량
        int minOrderQuantity,    // 최소 주문 수량
        LocalDateTime lastOrderedAt // 🔥 주문/수량 변경 일자
) {

}