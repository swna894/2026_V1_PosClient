package com.swna.javafx.pos.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.ProductResponseDto;
import com.swna.javafx.pos.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j 
@Service
@RequiredArgsConstructor
public class PosService {

    private final ProductRepository productClient;

    // =========================
    // Scan (Async)
    // =========================
    public Mono<PosItem> scan(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            log.warn("Scan failed: Barcode is null or empty");
            return Mono.empty();
        }

        log.debug("Starting product lookup for barcode: {}", barcode);

        // 1. productClient 호출 결과가 ApiResponse<ProductResponse>를 반환하도록 처리
        return productClient.findByBarcode(barcode)
                .flatMap(response -> {
                    // 2. 응답이 성공(success=true)이고 데이터가 존재하는지 확인
                    if (response.success() && response.data() != null) {
                        log.info("Product found: {} ({})", response.data().description(), barcode);
                        return Mono.just(toPosItem(response.data()));
                    } else {
                        // 3. 서버에서 에러 응답을 보냈거나 데이터가 없는 경우 처리
                        log.warn("Product not found or error response for barcode: {}. Code: {}, Message: {}", 
                                 barcode, response.code(), response.message());
                        return Mono.empty();
                    }
                })
                .doOnError(e -> 
                    log.error("Network or Server error for barcode: {}. Error: {}", barcode, e.getMessage())
                )
                .onErrorResume(e -> Mono.empty());
    }

    // =========================
    // DTO → PosItem Conversion
    // =========================
    private PosItem toPosItem(ProductResponseDto p) {
        log.trace("Converting ProductResponse to PosItem for code: {}", p.code());
        
        PosItem item = new PosItem();

        // Basic Information[cite: 2]
        item.setCode(p.code());
        item.setBarcode(p.barcode());
        item.setDescription(p.description());

        // Pricing logic[cite: 2]
        double original = safe(p.originalPrice(), p.sellingPrice());
        double selling  = safe(p.sellingPrice(), original);

        item.setOriginalPrice(original);
        item.setSellingPrice(selling);

        // Inventory / Quantity[cite: 2]
        item.setStock(p.stock());
        item.setQty(0); 

        // Initialize discounts[cite: 2]
        item.applyDiscount(0, 0);

        // Timestamp[cite: 2]
        item.setUpdated(LocalDateTime.now());

        return item;
    }

    // =========================
    // null-safe utility[cite: 2]
    // =========================
    private double safe(Double value, Double fallback) {
        if (value != null) return value;
        return (fallback != null) ? fallback : 0.0;
    }
}