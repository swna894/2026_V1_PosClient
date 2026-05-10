package com.swna.javafx.pos.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 카드 승인 결과 DTO (현재 POS에 필요한 필드만)
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class CardAuthResult {
    
    // ========== 기본 결과 ==========
    private final boolean success;
    private final TransactionResult result;
    private final String message;
    
    // ========== 승인 정보 ==========
    private final String authCode;           // 승인 번호
    private final String transactionId;      // 거래 ID
    private final String cardNumber;
    private final BigDecimal approvedAmount; // 승인된 금액
    
    // ========== 부가 정보 ==========
    private final BigDecimal cashOutAmount;  // 현금 인출 금액 (Cash Out 시)
    private final LocalDateTime approvedAt;  // 승인 시간
    
    // ========== 정적 팩토리 메서드 ==========
    
    /**
     * 승인 성공
     */
    public static CardAuthResult success(String authCode, String transactionId, BigDecimal approvedAmount, String cardNumber) {
        return CardAuthResult.builder()
            .success(true)
            .result(TransactionResult.SUCCESS)
            .message("Approved")
            .authCode(authCode)
            .transactionId(transactionId)
            .approvedAmount(approvedAmount)
            .approvedAt(LocalDateTime.now())
            .cardNumber(cardNumber)
            .build();
    }
    
    /**
     * Cash Out 포함 승인 성공
     */
    public static CardAuthResult successWithCashOut(String authCode, String transactionId,
                                                     BigDecimal approvedAmount, BigDecimal cashOutAmount, String cardNumber) {
        return CardAuthResult.builder()
            .success(true)
            .result(TransactionResult.SUCCESS)
            .message("Approved with Cash Out: " + cashOutAmount)
            .authCode(authCode)
            .transactionId(transactionId)
            .approvedAmount(approvedAmount)
            .cashOutAmount(cashOutAmount)
            .approvedAt(LocalDateTime.now())
            .cardNumber(cardNumber)
            .build();
    }
    
    /**
     * 승인 실패
     */
    public static CardAuthResult failure(String message) {
        return CardAuthResult.builder()
            .success(false)
            .result(TransactionResult.FAILURE)
            .message(message)
            .approvedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 결과 불명 (복구 필요)
     */
    public static CardAuthResult unknown(String transactionId) {
        return CardAuthResult.builder()
            .success(false)
            .result(TransactionResult.UNKNOWN)
            .message("Transaction result unknown. Please check terminal.")
            .transactionId(transactionId)
            .approvedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 취소됨
     */
    public static CardAuthResult cancelled() {
        return CardAuthResult.builder()
            .success(false)
            .result(TransactionResult.CANCELLED)
            .message("Transaction cancelled by user")
            .approvedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 시간 초과
     */
    public static CardAuthResult timeout() {
        return CardAuthResult.builder()
            .success(false)
            .result(TransactionResult.TIMED_OUT)
            .message("Transaction timed out")
            .approvedAt(LocalDateTime.now())
            .build();
    }
    
    // ========== 헬퍼 메서드 ==========
    
    public boolean hasCashOut() {
        return cashOutAmount != null && cashOutAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isUnknown() {
        return result == TransactionResult.UNKNOWN;
    }
    
    public boolean isCancelled() {
        return result == TransactionResult.CANCELLED;
    }
    
    public boolean isTimeout() {
        return result == TransactionResult.TIMED_OUT;
    }
    
    // ========== 거래 결과 유형 ==========
    
    public enum TransactionResult {
        SUCCESS,    // 거래 성공
        FAILURE,    // 거래 실패
        UNKNOWN,    // 결과 불명 (전원/통신 장애 시)
        CANCELLED,  // 사용자 취소
        TIMED_OUT   // 시간 초과
    }

    /**
     * POS 비활성 모드(Test/Demo)일 때 사용할 가상 승인 결과 생성
     */
    public static CardAuthResult virtualSuccess(String transactionId, BigDecimal amount) {
        return CardAuthResult.builder()
            .success(true)
            .result(TransactionResult.SUCCESS) // Enum 값 사용
            .message("가상 결제 승인 완료 (POS OFF 모드)")
            .authCode("VIRTUAL-9999")
            .transactionId(transactionId)
            .approvedAmount(amount)
            .cardNumber("411111******1111")
            .approvedAt(LocalDateTime.now())
            .build();
    }
}