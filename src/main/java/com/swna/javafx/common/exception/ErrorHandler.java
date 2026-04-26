package com.swna.javafx.common.exception;

import com.swna.javafx.common.response.ApiResponse;

public class ErrorHandler {

    public static ErrorPolicy resolve(ApiResponse<?> response) {

        ClientErrorCode code = ClientErrorCode.from(response.code());

        return ErrorPolicyResolver.resolve(code);
    }
}