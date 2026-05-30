package com.swna.javafx.pos.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SaleRequest(
    @NotEmpty(message = "At least one sale item is required.")
    @Valid
    List<SaleItemRequest> items,

    @NotEmpty(message = "At least one payment record is required.")
    @Valid
    List<PaymentRequest> payments,

    @Valid List<DiscountRequest> discounts
) {

/**
     * 결제 내역을 분석하여 대표 결제 유형 코드 반환
     */
    public String getPaymentTypeCode() {
        // Record 내부이므로 별도의 'saleRequest' 참조 없이 payments()를 직접 호출합니다.
        if (payments == null || payments.isEmpty()) {
            return "unknown";
        }

        // 모든 결제 타입 추출 (대문자 변환)
        List<String> types = payments.stream()
                .map((PaymentRequest p) -> p.type().toUpperCase())
                .toList();

        // 1. 우선순위: Cashout이 포함되어 있으면 무조건 "cashout"
        if (types.contains("CASHOUT")) {
            return "Cashout";
        }

        // 2. 우선순위: 카드 결제가 포함되어 있으면 "card" (Card + Cash 포함)
        // 기존 코드의 "PaymentRequest"를 실제 상수값인 "CARD" 또는 "CREDIT"으로 수정했습니다.
        if (types.contains("CARD") || types.contains("CREDIT")) {
            return "Card";
        }

        // 3. 순수 현금 결제
        if (types.contains("CASH")) {
            return "Cash";
        }

        return "Deleted";
    }



}