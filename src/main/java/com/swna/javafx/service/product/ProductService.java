package com.swna.javafx.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swna.javafx.domain.admin.product.Product;
import com.swna.javafx.repository.product.ProductApiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductApiRepository repo;

    public List<Product> search(String keyword) {
        return List.of(
                new Product(1L, "Apple", 1000),
                new Product(2L, "Banana", 2000)
        );
    }

    public List<Product> getProducts() {
        return repo.fetchProducts();
    }
}