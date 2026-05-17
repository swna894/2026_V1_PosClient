package com.swna.javafx.admin.sale.model;

import javafx.beans.property.*;
import java.math.BigDecimal;

/**
 * 판매 아이템 데이터 모델
 */
public class SaleItemModel {
    
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> discount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final StringProperty supplier = new SimpleStringProperty();
    
    public SaleItemModel() {
    }
    
    public SaleItemModel(String barcode, String productName, int quantity, 
                         BigDecimal price, BigDecimal discount, String supplier) {
        this.barcode.set(barcode);
        this.productName.set(productName);
        this.quantity.set(quantity);
        this.price.set(price);
        this.discount.set(discount);
        this.supplier.set(supplier);
        calculateSubtotal();
    }
    
    private void calculateSubtotal() {
        BigDecimal qty = BigDecimal.valueOf(quantity.get());
        BigDecimal subtotalValue = price.get().multiply(qty).subtract(discount.get());
        subtotal.set(subtotalValue);
    }
    
    // Getters and Properties
    public String getBarcode() { return barcode.get(); }
    public StringProperty barcodeProperty() { return barcode; }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    
    public String getProductName() { return productName.get(); }
    public StringProperty productNameProperty() { return productName; }
    public void setProductName(String productName) { this.productName.set(productName); }
    
    public int getQuantity() { return quantity.get(); }
    public IntegerProperty quantityProperty() { return quantity; }
    public void setQuantity(int quantity) { 
        this.quantity.set(quantity);
        calculateSubtotal();
    }
    
    public BigDecimal getPrice() { return price.get(); }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }
    public void setPrice(BigDecimal price) { 
        this.price.set(price);
        calculateSubtotal();
    }
    
    public BigDecimal getDiscount() { return discount.get(); }
    public ObjectProperty<BigDecimal> discountProperty() { return discount; }
    public void setDiscount(BigDecimal discount) { 
        this.discount.set(discount);
        calculateSubtotal();
    }
    
    public BigDecimal getSubtotal() { return subtotal.get(); }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }
    
    public String getSupplier() { return supplier.get(); }
    public StringProperty supplierProperty() { return supplier; }
    public void setSupplier(String supplier) { this.supplier.set(supplier); }
    
    public String getFormattedPrice() {
        return String.format("%,.0f", price.get());
    }
    
    public String getFormattedSubtotal() {
        return String.format("%,.0f", subtotal.get());
    }
}