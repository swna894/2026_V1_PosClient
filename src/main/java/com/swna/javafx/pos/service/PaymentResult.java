package com.swna.javafx.pos.service;


import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.SaleResponse;
import lombok.Value;

import java.math.BigDecimal;

/**
 * 결제 결과를 담는 DTO
 * 모든 결제 작업의 결과를 일관된 형태로 전달
 */
@Value
public class PaymentResult {

    private static final String STATUS_SUCCESS = "Success";

    boolean success;
    String message;
    SaleResponse saleResponse;    // 서버 응답 데이터 (성공시)
    SaleRequest saleRequest;      // 전송된 요청 데이터 (디버깅용)
    BigDecimal change;            // 현금 결제시 거스름돈
    
    // ========== Static Factory Methods ==========
    
    /**
     * 성공 결과 생성 (SaleResponse만 있음)
     */
    public static PaymentResult success(SaleResponse saleResponse) {
        return new PaymentResult(true, STATUS_SUCCESS, saleResponse, null, BigDecimal.ZERO);
    }
    
    /**
     * 성공 결과 생성 (SaleResponse + 거스름돈)
     */
    public static PaymentResult success(SaleResponse saleResponse, BigDecimal change) {
        return new PaymentResult(true, STATUS_SUCCESS, saleResponse, null, change);
    }
    
    /**
     * 성공 결과 생성 (디버깅용 SaleRequest 포함)
     */
    public static PaymentResult successWithRequest(SaleResponse saleResponse, SaleRequest saleRequest) {
        return new PaymentResult(true, STATUS_SUCCESS, saleResponse, saleRequest, BigDecimal.ZERO);
    }
    
    /**
     * 실패 결과 생성
     */
    public static PaymentResult fail(String message) {
        return new PaymentResult(false, message, null, null, BigDecimal.ZERO);
    }
    
    /**
     * 실패 결과 생성 (상세 정보 포함)
     */
    public static PaymentResult fail(String message, SaleRequest saleRequest) {
        return new PaymentResult(false, message, null, saleRequest, BigDecimal.ZERO);
    }
    
    // ========== Convenience Methods ==========
    
    /**
     * Sale ID 가져오기 (null-safe)
     */
    public Long getSaleId() {
        return saleResponse != null ? saleResponse.id() : null;
    }
    
    /**
     * 영수증 번호 가져오기 (null-safe)
     */
    public String getReceiptNo() {
        return saleResponse != null ? saleResponse.receiptNo() : null;
    }
    
    /**
     * 최종 결제 금액 가져오기
     */
    public BigDecimal getFinalAmount() {
        return saleResponse != null ? saleResponse.finalAmount() : BigDecimal.ZERO;
    }
    
    /**
     * 사용자 친화적 에러 메시지 반환
     */
    public String getUserFriendlyMessage() {
        if (success) {
            return "결제 성공";
        }
        
        if (message == null || message.isEmpty()) {
            return "결제에 실패했습니다.";
        }
        
        // 주요 에러 메시지 한글 매핑
        if (message.contains("Insufficient cash")) {
            return "⚠️ 받은 현금이 부족합니다.";
        }
        if (message.contains("Amount mismatch")) {
            return "⚠️ 결제 금액이 일치하지 않습니다.";
        }
        if (message.contains("Network") || message.contains("network")) {
            return "🌐 네트워크 연결을 확인해주세요.";
        }
        if (message.contains("timeout")) {
            return "⏰ 서버 응답 시간이 초과되었습니다.";
        }
        
        return "❌ " + message;
    }
}