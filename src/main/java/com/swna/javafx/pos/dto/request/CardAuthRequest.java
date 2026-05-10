package com.swna.javafx.pos.dto.request;

import java.math.BigDecimal;

/**
 * 카드 승인 요청 DTO (현재 POS에 필요한 필드만)
 */
public record CardAuthRequest(
    // ========== 필수 필드 ==========
    String transactionId,      // 거래 고유 ID (필수)
    BigDecimal amount,         // 거래 금액 (필수)
    
    // ========== 선택 필드 ==========
    TransactionType type,      // PURCHASE, REFUND, CASH_OUT (기본: PURCHASE)
    String currency,           // 통화 (기본: "NZD")
    BigDecimal cashOutAmount   // Cash Out 금액 (CASH_OUT 거래 시 필요)
) {
    
    // 컴팩트 생성자 - 유효성 검증
    public CardAuthRequest {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (type == null) {
            type = TransactionType.PURCHASE;
        }
        if (currency == null || currency.isBlank()) {
            currency = "NZD";
        }
        if (type == TransactionType.CASH_OUT && (cashOutAmount == null || cashOutAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Cash out amount required for CASH_OUT transaction");
        }
    }
    
    // ========== 팩토리 메서드 ==========
    
    /**
     * 일반 구매 (Purchase)
     */
    public static CardAuthRequest purchase(String transactionId, BigDecimal amount) {
        return new CardAuthRequest(transactionId, amount, TransactionType.PURCHASE, "NZD", null);
    }
    
    /**
     * 현금 인출 (Cash Out)
     */
    public static CardAuthRequest cashOut(String transactionId, BigDecimal amount, BigDecimal cashOutAmount) {
        return new CardAuthRequest(transactionId, amount, TransactionType.CASH_OUT, "NZD", cashOutAmount);
    }
    
    /**
     * 환불 (Refund)
     */
    public static CardAuthRequest refund(String transactionId, BigDecimal amount) {
        return new CardAuthRequest(transactionId, amount, TransactionType.REFUND, "NZD", null);
    }
    
    // ========== 헬퍼 메서드 ==========
    
    public boolean hasCashOut() {
        return cashOutAmount != null && cashOutAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isRefund() {
        return type == TransactionType.REFUND;
    }
    
    public boolean isPurchase() {
        return type == TransactionType.PURCHASE;
    }
    
    // ========== 거래 유형 ==========
    
    public enum TransactionType {
        PURCHASE,   // 구매
        REFUND,     // 환불
        CASH_OUT    // 현금 인출
    }
}