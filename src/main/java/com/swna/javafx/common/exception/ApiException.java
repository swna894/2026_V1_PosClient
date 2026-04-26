package com.swna.javafx.common.exception;

public class ApiException extends RuntimeException {

    private final ClientErrorCode code;

    public ApiException(String code, String message) {
        super(message);
        this.code = ClientErrorCode.from(code);
    }

    public ClientErrorCode getCode() {
        return code;
    }
}
