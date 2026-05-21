package com.swna.javafx.admin.sale.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 판매 데이터 모델 (JavaFX Property 적용)
 */
public class SaleModel {
    
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty receiptNo = new SimpleStringProperty();
    private final StringProperty cashier = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> paymentDateTime = new SimpleObjectProperty<>();
    
    // 금액 관련
    private final ObjectProperty<BigDecimal> originalAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> cashAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> cashoutAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> creditAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> discountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> costAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> saleAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> receivedAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> changeAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    
    // 결제 정보
    private final StringProperty paymentType = new SimpleStringProperty();
    private final StringProperty cardNumber = new SimpleStringProperty();
    private final StringProperty approvalNo = new SimpleStringProperty();
    
    // 판매 아이템 목록
    private final List<SaleItemModel> items = new ArrayList<>();
    
    // 생성자
    public SaleModel() {
    }
    
    public SaleModel(String receiptNo, BigDecimal saleAmount, String paymentType, 
                     LocalDateTime paymentDateTime, String cashier) {
        this.receiptNo.set(receiptNo);
        this.saleAmount.set(saleAmount);
        this.paymentType.set(paymentType);
        this.paymentDateTime.set(paymentDateTime);
        this.cashier.set(cashier);
    }
    
    // Getters and Properties
    public long getId() { return id.get(); }
    public LongProperty idProperty() { return id; }
    public void setId(long id) { this.id.set(id); }
    
    public String getReceiptNo() { return receiptNo.get(); }
    public StringProperty receiptNoProperty() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo.set(receiptNo); }
    
    // public String getCashier() { return cashier.get(); }
    // public StringProperty cashierProperty() { return cashier; }
    // public void setCashier(String cashier) { this.cashier.set(cashier); }
    
    public LocalDateTime getPaymentDateTime() { return paymentDateTime.get(); }
    public ObjectProperty<LocalDateTime> paymentDateTimeProperty() { return paymentDateTime; }
    public void setPaymentDateTime(LocalDateTime paymentDateTime) { this.paymentDateTime.set(paymentDateTime); }
    
    public BigDecimal getOriginalAmount() { return originalAmount.get(); }
    public ObjectProperty<BigDecimal> originalAmountProperty() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount.set(originalAmount); }

    public BigDecimal getCashAmount() { return cashAmount.get(); }
    public ObjectProperty<BigDecimal> cashAmountProperty() { return cashAmount; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount.set(cashAmount); }

    public BigDecimal getCreditAmount() { return creditAmount.get(); }
    public ObjectProperty<BigDecimal> creditAmountProperty() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount.set(creditAmount); }
    
    public BigDecimal getDiscountAmount() { return discountAmount.get(); }
    public ObjectProperty<BigDecimal> discountAmountProperty() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount.set(discountAmount); }

    public BigDecimal getCostAmount() { return costAmount.get(); } 
    public ObjectProperty<BigDecimal> costAmountProperty() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount.set(costAmount); }
    
    public BigDecimal getSaleAmount() { return saleAmount.get(); }
    public ObjectProperty<BigDecimal> saleAmountProperty() { return saleAmount; }
    public void setSaleAmount(BigDecimal saleAmount) { this.saleAmount.set(saleAmount); }
    
    public BigDecimal getReceivedAmount() { return receivedAmount.get(); }
    public ObjectProperty<BigDecimal> receivedAmountProperty() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount.set(receivedAmount); }
    
    public BigDecimal getCashoutAmount() { return cashoutAmount.get(); }
    public ObjectProperty<BigDecimal> cashoutAmountProperty() { return cashoutAmount; }
    public void setCashoutAmount(BigDecimal cashoutAmount) { this.cashoutAmount.set(cashoutAmount); }
    
    public BigDecimal getChangeAmount() { return changeAmount.get(); }
    public ObjectProperty<BigDecimal> changeAmountProperty() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount.set(changeAmount); }
    
    public String getPaymentType() { return paymentType.get(); }
    public StringProperty paymentTypeProperty() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType.set(paymentType); }
    
    public String getCardNumber() { return cardNumber.get(); }
    public StringProperty cardNumberProperty() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber.set(cardNumber); }
    
    public String getApprovalNo() { return approvalNo.get(); }
    public StringProperty approvalNoProperty() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo.set(approvalNo); }
    
    public List<SaleItemModel> getItems() { return items; }
    
    // 포맷된 문자열 반환
    public String getFormattedSaleAmount() {
        return String.format("%,.0f원", saleAmount.get());
    }
    
    public String getFormattedPaymentDateTime() {
        if (paymentDateTime.get() == null) return "";
        return paymentDateTime.get().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}