package com.swna.javafx.repository.product;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.domain.product.PageResult;
import com.swna.javafx.domain.product.Product;

@Repository
public class ProductApiRepository {

    private final WebClient client;

    public ProductApiRepository() {
        this.client = WebClient.builder().baseUrl("http://localhost:8080").build();
    }

    public List<Product> fetchProducts() {
        return client.get()
                .uri("/products")
                .retrieve()
                .bodyToFlux(Product.class)
                .collectList()
                .block();
    }

    public PageResult<Product> fetchProducts(String keyword, int page, int size) {
        return client.get()
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

    
}
