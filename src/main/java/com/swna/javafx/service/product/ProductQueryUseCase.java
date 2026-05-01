package com.swna.javafx.service.product;

import org.springframework.stereotype.Service;

import com.swna.javafx.domain.admin.product.PageResult;
import com.swna.javafx.domain.admin.product.Product;
import com.swna.javafx.infrastructure.cashe.ProductCache;
import com.swna.javafx.repository.product.ProductApiRepository;

@Service
public class ProductQueryUseCase {

    private final ProductApiRepository repository;
    private final ProductCache cache;

    public ProductQueryUseCase(ProductApiRepository repository,
                               ProductCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public PageResult<Product> getProducts(String keyword, int page, int size) {

        String key = keyword + ":" + page + ":" + size;

        // ================= CACHE =================
        if (cache.exists(key)) {
            return cache.get(key);
        }

        PageResult<Product> result = repository.fetchProducts(keyword, page, size);

        cache.put(key, result);

        return result;
    }
}
