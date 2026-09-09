package com.swna.javafx.admin.inventory.model;

import java.math.BigDecimal;

/**
 * 상품(재고) 등록 요청 DTO
 * 서버 ProductCreateRequest 와 필드 매칭
 */
public record InventoryCreateRequest(
        String code,
        String barcode,
        String description,
        BigDecimal price,
        BigDecimal cost,
        String category,
        int quantity,
        int minStock,
        int maxStock,
        int minOrderQuantity
) {
}