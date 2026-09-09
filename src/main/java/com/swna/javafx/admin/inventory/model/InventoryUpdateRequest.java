package com.swna.javafx.admin.inventory.model;

import java.math.BigDecimal;

/**
 * 상품(재고) 수정 요청 DTO
 * 서버 ProductUpdateRequest 와 필드 매칭
 */
public record InventoryUpdateRequest(
        Long id,
        String description,
        BigDecimal price,
        BigDecimal cost,
        BigDecimal priceOld,
        BigDecimal costOld,
        String category,
        Integer quantity,
        Integer minStock,
        Integer maxStock,
        Integer minOrderQuantity
) {
}