package com.swna.javafx.common.exception;

public enum ClientErrorCode {

    // AUTH
    AUTH_TOKEN_EXPIRED,
    AUTH_INVALID_TOKEN,
    AUTH_UNAUTHORIZED,
    AUTH_INVALID_PASSWORD,

    // USER
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,

    // COMMON
    INVALID_INPUT,
    INTERNAL_ERROR,

    // fallback
    UNKNOWN;

    public static ClientErrorCode from(String code) {
        try {
            return ClientErrorCode.valueOf(code);
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}