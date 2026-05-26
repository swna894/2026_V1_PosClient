package com.swna.javafx.admin.sale.model;

import javafx.beans.property.*;
import java.math.BigDecimal;

/**
 * 판매 아이템 데이터 모델
 * * [계산 규칙]
 * 1. 단가 기준: originalPrice = discountPrice + salePrice
 * 2. 총액 기준: originalAmount = originalPrice * quantity
 * 3. 할인 총액: discountAmount = discountPrice * quantity
 * 4. 실판매 총액: saleAmount = salePrice * quantity
 */
public class SaleItemModel {
    
    private static final String NUMBER_FORMAT = "%,.0f";
    
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();  // ✅ 추가
    private final IntegerProperty quantity = new SimpleIntegerProperty(0);
    
    // 단가 관련 속성 (오리지널 단가 = 할인 단가 + 실판매 단가)
    private final ObjectProperty<BigDecimal> originalPrice = new SimpleObjectProperty<>(BigDecimal.ZERO); 
    private final ObjectProperty<BigDecimal> discountPrice = new SimpleObjectProperty<>(BigDecimal.ZERO); 
    private final ObjectProperty<BigDecimal> salePrice = new SimpleObjectProperty<>(BigDecimal.ZERO);   
    
    // 총액 관련 속성 (수량이 곱해진 결과 자동 연산)
    private final ObjectProperty<BigDecimal> originalAmount = new SimpleObjectProperty<>(BigDecimal.ZERO); 
    private final ObjectProperty<BigDecimal> discountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO); 
    private final ObjectProperty<BigDecimal> saleAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);   
    
    private final ObjectProperty<BigDecimal> cost = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final StringProperty comment = new SimpleStringProperty(); 
    
    /**
     * 기본 생성자
     */
    public SaleItemModel() {
    }
    
    /**
     * 내부 빌더 전용 생성자
     */
    SaleItemModel(Builder builder) {
        this.id.set(builder.id);
        this.barcode.set(builder.barcode);
        this.description.set(builder.description != null ? builder.description : "");  // ✅ 추가
        this.quantity.set(builder.quantity);
        this.discountPrice.set(builder.discountPrice != null ? builder.discountPrice : BigDecimal.ZERO);
        this.salePrice.set(builder.salePrice != null ? builder.salePrice : BigDecimal.ZERO);
        this.cost.set(builder.cost != null ? builder.cost : BigDecimal.ZERO);
        this.comment.set(builder.comment != null ? builder.comment : "");
        
        // 데이터 설정 후 전체 자동 계산 수행
        calculatePricesAndAmounts();
    }

    /**
     * 핵심 비즈니스 로직: 가격 및 금액 통합 자동 연산
     */
    private void calculatePricesAndAmounts() {
        // 1. 단가 계산: originalPrice = discountPrice + salePrice
        BigDecimal discPrice = discountPrice.get() != null ? discountPrice.get() : BigDecimal.ZERO;
        BigDecimal sPrice = salePrice.get() != null ? salePrice.get() : BigDecimal.ZERO;
        BigDecimal origPrice = discPrice.add(sPrice);
        originalPrice.set(origPrice);
        
        // 2. 총액(Amount) 계산: 각 단가 * 수량
        BigDecimal qty = BigDecimal.valueOf(quantity.get());
        
        originalAmount.set(origPrice.multiply(qty));
        discountAmount.set(discPrice.multiply(qty));
        saleAmount.set(sPrice.multiply(qty));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 빌더 패턴 구현
     */
    public static class Builder {
        private String id;
        private String barcode;
        private String description;  // ✅ 추가
        private int quantity;
        private BigDecimal discountPrice = BigDecimal.ZERO; 
        private BigDecimal salePrice = BigDecimal.ZERO;   
        private BigDecimal cost = BigDecimal.ZERO;
        private String comment;

        public Builder id(String id) { this.id = id; return this; }
        public Builder barcode(String barcode) { this.barcode = barcode; return this; }
        public Builder description(String description) { this.description = description; return this; }  // ✅ 추가
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder discountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; return this; } 
        public Builder salePrice(BigDecimal salePrice) { this.salePrice = salePrice; return this; }     
        public Builder cost(BigDecimal cost) { this.cost = cost; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }
        
        public SaleItemModel build() {
            return new SaleItemModel(this);
        }
    }
    
    // ==========================================
    // JavaFX Properties, Getters & Setters
    // ==========================================
    
    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }
    public void setId(String id) { this.id.set(id); }

    public String getBarcode() { return barcode.get(); }
    public StringProperty barcodeProperty() { return barcode; }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    
    // ✅ description getter/setter
    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }
    public void setDescription(String description) { this.description.set(description); }
    
    public int getQuantity() { return quantity.get(); }
    public IntegerProperty quantityProperty() { return quantity; }
    public void setQuantity(int quantity) { 
        this.quantity.set(quantity); 
        calculatePricesAndAmounts();
    }
    
    // 단가(Price) 관련 Getter / Setter
    public BigDecimal getOriginalPrice() { return originalPrice.get(); }
    public ObjectProperty<BigDecimal> originalPriceProperty() { return originalPrice; }

    public BigDecimal getDiscountPrice() { return discountPrice.get(); }
    public ObjectProperty<BigDecimal> discountPriceProperty() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { 
        this.discountPrice.set(discountPrice); 
        calculatePricesAndAmounts();
    }

    public BigDecimal getSalePrice() { return salePrice.get(); }
    public ObjectProperty<BigDecimal> salePriceProperty() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { 
        this.salePrice.set(salePrice); 
        calculatePricesAndAmounts();
    }
    
    // 총액(Amount) 관련 Getter (Setter는 자동 연산되므로 제공하지 않음)
    public BigDecimal getOriginalAmount() { return originalAmount.get(); }
    public ObjectProperty<BigDecimal> originalAmountProperty() { return originalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount.get(); }
    public ObjectProperty<BigDecimal> discountAmountProperty() { return discountAmount; }

    public BigDecimal getSaleAmount() { return saleAmount.get(); }
    public ObjectProperty<BigDecimal> saleAmountProperty() { return saleAmount; }

    // 기타 필드
    public BigDecimal getCost() { return cost.get(); }
    public ObjectProperty<BigDecimal> costProperty() { return cost; }
    public void setCost(BigDecimal cost) { this.cost.set(cost); }
    
    public String getComment() { return comment.get(); }
    public StringProperty commentProperty() { return comment; }
    public void setComment(String comment) { this.comment.set(comment); }
    
    // ==========================================
    // UI 포맷팅용 편의 메서드
    // ==========================================
    public String getFormattedOriginalPrice() { return String.format(NUMBER_FORMAT, originalPrice.get()); }
    public String getFormattedDiscountPrice() { return String.format(NUMBER_FORMAT, discountPrice.get()); }
    public String getFormattedSalePrice() { return String.format(NUMBER_FORMAT, salePrice.get()); }
    
    public String getFormattedOriginalAmount() { return String.format(NUMBER_FORMAT, originalAmount.get()); }
    public String getFormattedDiscountAmount() { return String.format(NUMBER_FORMAT, discountAmount.get()); }
    public String getFormattedSaleAmount() { return String.format(NUMBER_FORMAT, saleAmount.get()); }
    
    public String getFormattedCost() { return String.format(NUMBER_FORMAT, cost.get()); }
}