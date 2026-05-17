package com.swna.javafx.admin.sale.dto;

import java.math.BigDecimal;

/**
 * 판매 아이템 DTO (Record 타입)
 */
public record SaleItemDto(
    String itemId,
    String productId,
    String productCode,
    String productName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal discount,
    BigDecimal subtotal,
    BigDecimal tax,
    String barcode,
    String category,
    String brand
) {
    public SaleItemDto {
        if (quantity < 0) quantity = 0;
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        if (discount == null) discount = BigDecimal.ZERO;
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (tax == null) tax = BigDecimal.ZERO;
    }
    
    public String getFormattedUnitPrice() {
        return String.format("%,.0f원", unitPrice);
    }
    
    public String getFormattedSubtotal() {
        return String.format("%,.0f원", subtotal);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String itemId;
        private String productId;
        private String productCode;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private BigDecimal discount = BigDecimal.ZERO;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private String barcode;
        private String category;
        private String brand;
        
        public Builder itemId(String itemId) { this.itemId = itemId; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder productCode(String productCode) { this.productCode = productCode; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder discount(BigDecimal discount) { this.discount = discount; return this; }
        public Builder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public Builder tax(BigDecimal tax) { this.tax = tax; return this; }
        public Builder barcode(String barcode) { this.barcode = barcode; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        
        public SaleItemDto build() {
            return new SaleItemDto(itemId, productId, productCode, productName, quantity,
                                  unitPrice, discount, subtotal, tax, barcode, category, brand);
        }
    }
}