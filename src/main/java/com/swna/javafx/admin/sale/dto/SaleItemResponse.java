package com.swna.javafx.admin.sale.dto;

import java.math.BigDecimal;

/**
 * 판매 아이템 DTO (Record 타입)
 * 변경사항: unitPrice 필드 제거, discountPrice 및 salePrice 적용 완료
 */
public record SaleItemResponse(
    String id,
    String barcode,
    String description, // ✅ 상품명 추가
    BigDecimal discountPrice, // ✅ unitPrice 제거 후 정렬
    BigDecimal cost,
    BigDecimal salePrice,     
    int quantity,
    String supplier,
    String comment 
) {
    /**
     * 컴팩트 생성자: 데이터 검증 및 기본값(Null 방어) 세팅
     */
    public SaleItemResponse {
        if (quantity < 0) quantity = 0;
        if (discountPrice == null) discountPrice = BigDecimal.ZERO;
        if (cost == null) cost = BigDecimal.ZERO;
        if (salePrice == null) salePrice = BigDecimal.ZERO;         
        if (comment == null) comment = "";
    }

    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 빌더 패턴 구현 (내부 클래스)
     */
    public static class Builder {
        private String id;
        private String barcode;
        private String description;
        private BigDecimal discountPrice = BigDecimal.ZERO; // ✅ unitPrice 필드 제거
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal salePrice = BigDecimal.ZERO;     
        private int quantity;
        private String supplier;
        private String comment;

        public Builder id(String id) { this.id = id; return this; }
        public Builder barcode(String barcode) { this.barcode = barcode; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder discountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; return this; } 
        public Builder cost(BigDecimal cost) { this.cost = cost; return this; }
        public Builder salePrice(BigDecimal salePrice) { this.salePrice = salePrice; return this; }     
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder supplier(String supplier) { this.supplier = supplier; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }
        
        /**
         * Record 생성자 스펙 순서(unitPrice 제외)에 완벽하게 대응하여 객체 생성
         */
        public SaleItemResponse build() {
            return new SaleItemResponse(
                this.id, 
                this.barcode, 
                this.description, // ✅ 상품명 추가
                this.discountPrice, // ✅ 순서 맞춤
                this.cost, 
                this.salePrice,     
                this.quantity, 
                this.supplier,
                this.comment
            );
        }
    }
}