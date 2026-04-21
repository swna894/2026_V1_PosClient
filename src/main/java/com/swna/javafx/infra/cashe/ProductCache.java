package com.swna.javafx.infra.cashe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.swna.javafx.domain.product.PageResult;
import com.swna.javafx.domain.product.Product;

@Component
public class ProductCache {

    private final Map<String, PageResult<Product>> cache = new ConcurrentHashMap<>();

    public boolean exists(String key) {
        return cache.containsKey(key);
    }

    public PageResult<Product> get(String key) {
        return cache.get(key);
    }

    public void put(String key, PageResult<Product> value) {
        cache.put(key, value);
    }

    public void clear() {
        cache.clear();
    }
}