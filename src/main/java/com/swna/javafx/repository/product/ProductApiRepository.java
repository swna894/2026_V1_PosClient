package com.swna.javafx.repository.product;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

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

    
}
