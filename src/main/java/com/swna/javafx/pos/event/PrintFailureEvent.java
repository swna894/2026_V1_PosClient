package com.swna.javafx.pos.event;

/**
 * 프린트 실패 시 발생하는 이벤트
 * @param receiptNo 영수증 번호
 * @param errorMessage 에러 내용
 */
public record PrintFailureEvent(
    String receiptNo, 
    String errorMessage
) {
}
