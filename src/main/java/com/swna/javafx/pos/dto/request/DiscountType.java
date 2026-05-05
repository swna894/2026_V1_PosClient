package com.swna.javafx.pos.dto.request;

public enum DiscountType {
    /**
     * 할인 없음
     */
    NONE,
    /**
     * 고정 금액 할인 (예: 1,000원 할인)
     */
    AMOUNT,

    /**
     * 비율 할인 (예: 10% 할인)
     */
    PERCENT
}
