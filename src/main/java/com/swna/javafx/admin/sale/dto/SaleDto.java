package com.swna.javafx.admin.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SaleDto {
    private String id;
    private BigDecimal discountAmount;
    private BigDecimal originalAmount;
    private BigDecimal saleAmount;
    private BigDecimal costAmount;
    
    private BigDecimal cashAmount;
    private BigDecimal cashoutAmount;
    private BigDecimal creditAmount;
    private BigDecimal receivedAmount;
    private BigDecimal changeAmount;
    
    private String receiptNo;
    private String paymentType;
    private String cardNumber;
    private String cashier;
    private LocalDateTime paymentDateTime;
}
