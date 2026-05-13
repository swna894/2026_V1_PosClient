package com.swna.javafx.pos.dto.response; // 공통 DTO 패키지

import com.swna.javafx.pos.dto.request.SaleRequest;
import lombok.Value;
import java.math.BigDecimal;

/**
 * 통합 결제 결과 DTO
 * 서비스 레이어와 UI 레이어 모두에서 공통으로 사용
 */
@Value
public class PaymentResult {

    private static final String STATUS_SUCCESS = "Success";

    boolean success;
    String message;        // 성공 메시지 또는 상태 메시지
    SaleResponse saleResponse;    // 서버 응답 데이터 (성공 시)
    SaleRequest saleRequest;      // 전송된 요청 데이터 (디버깅용)
    BigDecimal change;            // 현금 결제 시 거스름돈
    
    // ========== Static Factory Methods (통합 및 확장) ==========
    
    /**
     * UI 레이어용 단순 성공 메시지 생성 (기존 Manager용 대응)
     */
    public static PaymentResult success(String message) {
        return new PaymentResult(true, message, null, null, BigDecimal.ZERO);
    }

    /**
     * UI 레이어용 단순 실패 메시지 생성 (기존 Manager용 대응)
     */
    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, errorMessage, null, null, BigDecimal.ZERO);
    }

    /**
     * 서버 응답을 포함한 성공 결과 생성
     */
    public static PaymentResult success(SaleResponse saleResponse) {
        return new PaymentResult(true, STATUS_SUCCESS, saleResponse, null, BigDecimal.ZERO);
    }
    
    /**
     * 서버 응답과 거스름돈을 포함한 성공 결과 생성
     */
    public static PaymentResult success(SaleResponse saleResponse, BigDecimal change) {
        return new PaymentResult(true, STATUS_SUCCESS, saleResponse, null, change);
    }
    
    /**
     * 에러 메시지를 포함한 실패 결과 생성
     */
    public static PaymentResult fail(String message) {
        return new PaymentResult(false, message, null, null, BigDecimal.ZERO);
    }

    /**
     * 실패 결과 생성 (에러 메시지 + 요청 데이터 포함)
     * PaymentService의 .onErrorResume에서 사용됨
     */
    public static PaymentResult fail(String message, SaleRequest saleRequest) {
        // success: false, message: 에러내용, response: null, request: 전송데이터, change: 0
        return new PaymentResult(false, message, null, saleRequest, BigDecimal.ZERO);
    }

    // (필요 시 기존의 fail(message, saleRequest) 등 추가 유지 가능)

    // ========== Convenience Methods (기존 로직 유지) ==========
    
    public Long getSaleId() {
        return saleResponse != null ? saleResponse.id() : null;
    }
    
    public String getReceiptNo() {
        return saleResponse != null ? saleResponse.receiptNo() : null;
    }
    
    public String getUserFriendlyMessage() {
        if (success) return message != null ? message : "결제 성공";
        
        if (message == null || message.isEmpty()) return "결제에 실패했습니다.";
        
        // 주요 에러 메시지 한글 매핑 로직 유지
        if (message.contains("Insufficient cash")) return "⚠️ 받은 현금이 부족합니다.";
        return "❌ " + message;
    }
}