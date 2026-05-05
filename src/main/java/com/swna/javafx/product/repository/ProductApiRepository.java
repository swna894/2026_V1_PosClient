package com.swna.javafx.product.repository;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.admin.product.PageResult;
import com.swna.javafx.admin.product.Product;
import com.swna.javafx.barcode.infrastructre.ProductLabelDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class ProductApiRepository {

    private final WebClient webClient;

    public List<Product> fetchProducts() {
        return webClient.get()
                .uri("/products")
                .retrieve()
                .bodyToFlux(Product.class)
                .collectList()
                .block();
    }

    public PageResult<Product> fetchProducts(String keyword, int page, int size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/products")
                        .queryParam("keyword", keyword)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResult<Product>>() {})
                .block();
    }

    public Flux<ProductLabelDto> getAllProductLabels() {
        return webClient.get()
                .uri("/products/labels")
                .retrieve()
                .bodyToFlux(ProductLabelDto.class);
    } 
}
