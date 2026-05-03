package com.swna.javafx.infrastructure.pos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.dto.pos.ProductResponse;

import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final WebClient webClient;

    public Mono<ProductResponse> findByBarcode(String barcode) {

        return webClient.get()
                .uri("/products/barcode/{barcode}", barcode)
                .retrieve()
                .bodyToMono(ProductResponse.class);
    }
}
