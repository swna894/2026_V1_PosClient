package com.swna.javafx.admin.sale.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SaleItem {
    
    private final StringProperty itemId = new SimpleStringProperty();
    private Sale sale;
    private final StringProperty productId = new SimpleStringProperty();
    private final StringProperty productCode = new SimpleStringProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    
    // 금액 관련 변수들을 ObjectProperty<BigDecimal>로 변경
    private final ObjectProperty<BigDecimal> unitPrice = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> discount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> tax = new SimpleObjectProperty<>(BigDecimal.ZERO);
    
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty brand = new SimpleStringProperty();
    
    // 기본 생성자
    public SaleItem() {
        this.itemId.set(generateItemId());
        this.quantity.set(1);
        this.unitPrice.set(BigDecimal.ZERO);
        this.discount.set(BigDecimal.ZERO);
        this.subtotal.set(BigDecimal.ZERO);
        this.tax.set(BigDecimal.ZERO);
    }

    // 매개변수가 있는 생성자 (double -> BigDecimal)
    public SaleItem(String productId, String productName, int quantity, BigDecimal unitPrice) {
        this();
        this.productId.set(productId);
        this.productName.set(productName);
        this.quantity.set(quantity);
        this.unitPrice.set(unitPrice);
        calculateSubtotal();
    }

    // 전체 매개변수 생성자 (double -> BigDecimal)
    public SaleItem(String productId, String productCode, String productName, 
                   int quantity, BigDecimal unitPrice, BigDecimal discount) {
        this();
        this.productId.set(productId);
        this.productCode.set(productCode);
        this.productName.set(productName);
        this.quantity.set(quantity);
        this.unitPrice.set(unitPrice);
        this.discount.set(discount);
        calculateSubtotal();
    }

    // 아이템 ID 생성
    private String generateItemId() {
        return "ITEM_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    // 소계 계산 (BigDecimal 연산 적용)
    public void calculateSubtotal() {
        BigDecimal qty = BigDecimal.valueOf(quantity.get());
        BigDecimal subtotalBeforeDiscount = unitPrice.get().multiply(qty);
        
        // 소계 = 할인 전 총액 - 할인액
        BigDecimal finalSubtotal = subtotalBeforeDiscount.subtract(discount.get());
        subtotal.set(finalSubtotal);
        
        // 부가세 10% (소수점 버림)
        tax.set(finalSubtotal.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.DOWN));
        
        if (sale != null) {
            sale.calculateTotals();
        }
    }

    // Property Getters
    public StringProperty itemIdProperty() { return itemId; }
    public StringProperty productIdProperty() { return productId; }
    public StringProperty productCodeProperty() { return productCode; }
    public StringProperty productNameProperty() { return productName; }
    public IntegerProperty quantityProperty() { return quantity; }
    public ObjectProperty<BigDecimal> unitPriceProperty() { return unitPrice; }
    public ObjectProperty<BigDecimal> discountProperty() { return discount; }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }
    public ObjectProperty<BigDecimal> taxProperty() { return tax; }
    public StringProperty barcodeProperty() { return barcode; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty brandProperty() { return brand; }

    // Standard Getters
    public String getItemId() { return itemId.get(); }
    public Sale getSale() { return sale; }
    public String getProductId() { return productId.get(); }
    public String getProductCode() { return productCode.get(); }
    public String getProductName() { return productName.get(); }
    public int getQuantity() { return quantity.get(); }
    public BigDecimal getUnitPrice() { return unitPrice.get(); }
    public BigDecimal getDiscount() { return discount.get(); }
    public BigDecimal getSubtotal() { return subtotal.get(); }
    public BigDecimal getTax() { return tax.get(); }
    public String getBarcode() { return barcode.get(); }
    public String getCategory() { return category.get(); }
    public String getBrand() { return brand.get(); }

    // Standard Setters
    public void setItemId(String itemId) { this.itemId.set(itemId); }
    public void setSale(Sale sale) { this.sale = sale; }
    public void setProductId(String productId) { this.productId.set(productId); }
    public void setProductCode(String productCode) { this.productCode.set(productCode); }
    public void setProductName(String productName) { this.productName.set(productName); }
    public void setQuantity(int quantity) { 
        this.quantity.set(quantity);
        calculateSubtotal();
    }
    public void setUnitPrice(BigDecimal unitPrice) { 
        this.unitPrice.set(unitPrice);
        calculateSubtotal();
    }
    public void setDiscount(BigDecimal discount) { 
        this.discount.set(discount);
        calculateSubtotal();
    }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal.set(subtotal); }
    public void setTax(BigDecimal tax) { this.tax.set(tax); }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    public void setCategory(String category) { this.category.set(category); }
    public void setBrand(String brand) { this.brand.set(brand); }

    // 포맷된 가격 반환
    public String getFormattedUnitPrice() {
        return String.format("₩%,.0f", unitPrice.get());
    }

    public String getFormattedSubtotal() {
        return String.format("₩%,.0f", subtotal.get());
    }

    public String getFormattedDiscount() {
        return String.format("₩%,.0f", discount.get());
    }

    @Override
    public String toString() {
        return String.format("%s x %d = ₩%,.0f (할인: ₩%,.0f)", 
            productName.get(), quantity.get(), subtotal.get(), discount.get());
    }
}