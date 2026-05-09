package com.swna.javafx.pos.event;

import java.util.List;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.service.PaymentResult;

/**
 * 결제 성공 시 발행될 이벤트 데이터
 */
public record PaymentSuccessEvent(
    List<PosItem> soldItems,  // 결제된 상품들
    PaymentResult result     // 결제 승인 결과
) {}
