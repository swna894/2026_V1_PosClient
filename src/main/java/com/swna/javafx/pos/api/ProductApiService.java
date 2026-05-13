package com.swna.javafx.pos.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.swna.javafx.common.api.ApiResponseException;
import com.swna.javafx.common.api.WebClientCommon;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.response.ProductResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductApiService {
    private final WebClientCommon webClient;

    public Mono<ProductResponse> getProductByBarcode(String barcode) {
        String url = "/products/barcode/" + barcode;
        var typeRef = new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {};

        return webClient.get(url, typeRef)
                .flatMap(this::handleResponse);
    }

    // ApiResponse를 공통적으로 처리하는 private 메서드
    private <T> Mono<T> handleResponse(ApiResponse<T> response) {
        if (response.isSuccess() && response.hasData()) {
            return Mono.just(response.data());
        }
        return Mono.error(new ApiResponseException(response.code(), response.message()));
    }
}
