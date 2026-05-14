package com.swna.javafx.admin.supplier.service;

import com.swna.javafx.common.response.ApiResponse;

import reactor.core.publisher.Mono;

public final class ResponseHandler {

    private ResponseHandler() {
    }

    public static <T> Mono<T> unwrap(ApiResponse<T> response) {

        if (response == null) {
            return Mono.error(
                    new RuntimeException("Server response is null")
            );
        }

        if (!response.isSuccess()) {

            String errorMessage = String.format(
                    "[%s] %s",
                    response.code(),
                    response.message()
            );

            return Mono.error(
                    new RuntimeException(errorMessage)
            );
        }

        if (!response.hasData()) {
            return Mono.error(
                    new RuntimeException("Response data is empty")
            );
        }

        return Mono.just(response.data());
    }
}