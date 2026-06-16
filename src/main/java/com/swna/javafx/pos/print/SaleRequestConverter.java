package com.swna.javafx.pos.print;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.swna.javafx.pos.dto.request.DiscountRequest;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleItemRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.SaleResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SaleRequestConverter {

    /**
     * SaleResponse와 List<SaleItemRequest>를 조합하여 
     * 최신 규격의 클라이언트 SaleRequest 객체를 역변환 생성합니다.
     */
    public static SaleRequest toSaleRequest(SaleResponse response, List<SaleItemRequest> items) {
        
        if (response == null) {
            throw new IllegalArgumentException("SaleResponse cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("SaleItemRequest list cannot be null or empty");
        }

        // 1. Items 영역 매핑
        List<SaleItemRequest> requestItems = items;

        // 2. SaleResponse의 status 문자열을 정규화하여 직접 결제 타입 문자열로 추출
        String status = response.status();
        String inferredPaymentType = (status != null) ? status.toUpperCase() : "CASH";
        
        // 3. Discounts 영역 역구성
        List<DiscountRequest> requestDiscounts = new ArrayList<>();
        BigDecimal totalDiscountAmount = response.discountAmount();
        
        // 정상 판매 건이고 할인이 존재할 때만 할인 객체 복구[cite: 4]
        if (totalDiscountAmount != null && totalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            DiscountRequest generalDiscount = DiscountRequest.fixed(
                    totalDiscountAmount, 
                    "Reconstructed from previous sale response [Receipt No: " + response.receiptNo() + "]"
            );
            requestDiscounts.add(generalDiscount);
        }

        // 4. Payments 영역 역구성 (SaleResponse의 finalAmount 기준)
        List<PaymentRequest> requestPayments = new ArrayList<>();
        BigDecimal finalPaidAmount = response.finalAmount();
        
        // 결제 금액 최소 검증(PositiveOrZero) 방어 코드
        if (finalPaidAmount == null || finalPaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            finalPaidAmount = BigDecimal.valueOf(0.01);
        }

        // 최신 PaymentRequest 규격(String type)에 맞춰 객체 매핑 수행
        PaymentRequest paymentRequest = switch (inferredPaymentType) {
            case "CASH" -> new PaymentRequest("CASH", finalPaidAmount, finalPaidAmount, BigDecimal.ZERO, null, null);
            case "CARD" -> new PaymentRequest("CARD", finalPaidAmount, BigDecimal.ZERO, BigDecimal.ZERO, "RE_AUTH", "****");
            case "CASHOUT" -> new PaymentRequest("CASHOUT", finalPaidAmount, BigDecimal.ZERO, finalPaidAmount, "RE_AUTH", "****");
            default -> {
                log.warn("Unknown sale status '{}', falling back to CASH", status);
                yield new PaymentRequest("CASH", finalPaidAmount, finalPaidAmount, BigDecimal.ZERO, null, null);
            }
        };
        requestPayments.add(paymentRequest);

        // 5. 최신 SaleRequest Record의 기본 생성자(Canonical Constructor)를 호출하여 최종 반환
        return new SaleRequest(requestItems, requestPayments, requestDiscounts);
    }
}