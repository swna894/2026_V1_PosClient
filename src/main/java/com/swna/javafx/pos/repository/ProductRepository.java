package com.swna.javafx.pos.repository;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductRepository {

    private final WebClient webClient;

    /**
     * 바코드를 이용해 서버로부터 상품 정보를 조회합니다.
     * 반환 타입은 공통 응답 규격인 ApiResponse로 감싸진 ProductResponse입니다.
     */
    public Mono<ApiResponse<ProductResponseDto>> findByBarcode(String barcode) {
        return webClient.get()
                .uri("/products/barcode/{barcode}", barcode)
                .retrieve()
                // ApiResponse<ProductResponse> 형태로 역직렬화하기 위해 ParameterizedTypeReference 사용
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ProductResponseDto>>() {});
    }
}
