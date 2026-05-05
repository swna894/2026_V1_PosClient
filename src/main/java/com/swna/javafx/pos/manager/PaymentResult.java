package com.swna.javafx.pos.manager;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentResult {
    private final boolean success;
    private final String message;
    private final String errorMessage;

    public static PaymentResult success(String message) {
        return new PaymentResult(true, message, null);
    }

    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, null, errorMessage);
    }
}
