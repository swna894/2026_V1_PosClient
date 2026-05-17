package com.swna.javafx.admin.sale.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Sale {
    
    private final StringProperty saleId = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> saleDate = new SimpleObjectProperty<>();
    private final List<SaleItem> items = new ArrayList<>();
    
    // double -> ObjectProperty<BigDecimal>로 변경
    private final ObjectProperty<BigDecimal> subtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> tax = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> discount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> amountPaid = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> change = new SimpleObjectProperty<>(BigDecimal.ZERO);
    
    private final StringProperty paymentMethod = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty cashierName = new SimpleStringProperty();
    private final StringProperty cashierId = new SimpleStringProperty();
    private final StringProperty storeId = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();

    // 기본 생성자
    public Sale() {
        this.saleId.set(generateSaleId());
        this.saleDate.set(LocalDateTime.now());
        this.status.set("PENDING");
        this.createdAt.set(LocalDateTime.now());
        this.updatedAt.set(LocalDateTime.now());
        
        this.subtotal.set(BigDecimal.ZERO);
        this.tax.set(BigDecimal.ZERO);
        this.discount.set(BigDecimal.ZERO);
        this.totalAmount.set(BigDecimal.ZERO);
        this.amountPaid.set(BigDecimal.ZERO);
        this.change.set(BigDecimal.ZERO);
    }

    // 매개변수가 있는 생성자
    public Sale(String cashierName, String cashierId) {
        this();
        this.cashierName.set(cashierName);
        this.cashierId.set(cashierId);
    }

    // ID 생성 메서드
    private String generateSaleId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6);
        return "SALE_" + timestamp + "_" + uuid;
    }

    // 판매 아이템 추가
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
        calculateTotals();
    }

    // 판매 아이템 제거
    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setSale(null);
        calculateTotals();
    }

    // 전체 금액 계산 (BigDecimal 연산 적용)
    public void calculateTotals() {
        BigDecimal newSubtotal = items.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        subtotal.set(newSubtotal);
        
        // 부가세 10% 계산 (원화 기준이므로 소수점 버림 처리 - 필요시 RoundingMode 변경)
        BigDecimal calculatedTax = newSubtotal.multiply(new BigDecimal("0.10"))
                                              .setScale(0, RoundingMode.DOWN);
        tax.set(calculatedTax);
        
        // 총액 = 소계 + 부가세 - 할인액
        BigDecimal total = newSubtotal.add(calculatedTax).subtract(discount.get());
        totalAmount.set(total);
    }

    // 거스름돈 계산
    public void calculateChange() {
        change.set(amountPaid.get().subtract(totalAmount.get()));
    }

    // 결제 완료 처리
    public void completePayment() {
        // compareTo를 이용한 크기 비교 (amountPaid >= totalAmount)
        if (amountPaid.get().compareTo(totalAmount.get()) >= 0) {
            status.set("COMPLETED");
            calculateChange();
            updatedAt.set(LocalDateTime.now());
        } else {
            throw new IllegalStateException("결제 금액이 총액보다 작습니다.");
        }
    }

    // 취소 처리
    public void cancel() {
        status.set("CANCELLED");
        updatedAt.set(LocalDateTime.now());
    }

    // Property Getters
    public StringProperty saleIdProperty() { return saleId; }
    public ObjectProperty<LocalDateTime> saleDateProperty() { return saleDate; }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }
    public ObjectProperty<BigDecimal> taxProperty() { return tax; }
    public ObjectProperty<BigDecimal> discountProperty() { return discount; }
    public ObjectProperty<BigDecimal> totalAmountProperty() { return totalAmount; }
    public ObjectProperty<BigDecimal> amountPaidProperty() { return amountPaid; }
    public ObjectProperty<BigDecimal> changeProperty() { return change; }
    public StringProperty paymentMethodProperty() { return paymentMethod; }
    public StringProperty statusProperty() { return status; }
    public StringProperty cashierNameProperty() { return cashierName; }
    public StringProperty cashierIdProperty() { return cashierId; }
    public StringProperty storeIdProperty() { return storeId; }
    public StringProperty notesProperty() { return notes; }

    // Standard Getters
    public String getSaleId() { return saleId.get(); }
    public LocalDateTime getSaleDate() { return saleDate.get(); }
    public List<SaleItem> getItems() { return new ArrayList<>(items); }
    public BigDecimal getSubtotal() { return subtotal.get(); }
    public BigDecimal getTax() { return tax.get(); }
    public BigDecimal getDiscount() { return discount.get(); }
    public BigDecimal getTotalAmount() { return totalAmount.get(); }
    public BigDecimal getAmountPaid() { return amountPaid.get(); }
    public BigDecimal getChange() { return change.get(); }
    public String getPaymentMethod() { return paymentMethod.get(); }
    public String getStatus() { return status.get(); }
    public String getCashierName() { return cashierName.get(); }
    public String getCashierId() { return cashierId.get(); }
    public String getStoreId() { return storeId.get(); }
    public String getNotes() { return notes.get(); }

    // Standard Setters
    public void setSaleId(String saleId) { this.saleId.set(saleId); }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate.set(saleDate); }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal.set(subtotal); }
    public void setTax(BigDecimal tax) { this.tax.set(tax); }
    public void setDiscount(BigDecimal discount) { 
        this.discount.set(discount);
        calculateTotals();
    }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount.set(totalAmount); }
    public void setAmountPaid(BigDecimal amountPaid) { 
        this.amountPaid.set(amountPaid);
        if ("COMPLETED".equals(status.get())) {
            calculateChange();
        }
    }
    public void setChange(BigDecimal change) { this.change.set(change); }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod.set(paymentMethod); }
    public void setStatus(String status) { this.status.set(status); }
    public void setCashierName(String cashierName) { this.cashierName.set(cashierName); }
    public void setCashierId(String cashierId) { this.cashierId.set(cashierId); }
    public void setStoreId(String storeId) { this.storeId.set(storeId); }
    public void setNotes(String notes) { this.notes.set(notes); }
}