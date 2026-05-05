package com.swna.javafx.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swna.javafx.admin.product.Product;
import com.swna.javafx.barcode.infrastructre.ProductLabelDto;
import com.swna.javafx.product.repository.ProductApiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductApiRepository repo;

    /**
     * 상품 라벨 목록을 가져옵니다. (Repository의 Flux를 List로 변환)
     */
    public List<ProductLabelDto> getProductLabels() {
        return repo.getAllProductLabels()
                   .collectList()
                   .block(); // Service에서 block하여 결과를 반환

    }
    
    public List<Product> getProducts() {
        return repo.fetchProducts();
    }
    
    public List<Product> search(String keyword) {
        return List.of(
                new Product(1L, "Apple", 1000),
                new Product(2L, "Banana", 2000)
        );
    }

    
}