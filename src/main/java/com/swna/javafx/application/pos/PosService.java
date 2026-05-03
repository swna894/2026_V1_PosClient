package com.swna.javafx.application.pos;

import org.springframework.stereotype.Service;
import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.dto.pos.ProductResponse;
import com.swna.javafx.infrastructure.pos.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 추가
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Slf4j 
@Service
@RequiredArgsConstructor
public class PosService {

    private final ProductClient productClient;

    // =========================
    // Scan (Async)
    // =========================
    public Mono<PosItem> scan(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            log.warn("Scan failed: Barcode is null or empty");
            return Mono.empty();
        }

        log.debug("Starting product lookup for barcode: {}", barcode);

        return productClient.findByBarcode(barcode)
                .map(response -> {
                    log.info("Product found: {} ({})", response.description(), barcode);
                    return toPosItem(response);
                })
                .doOnError(e -> 
                    log.error("Failed to retrieve product for barcode: {}. Error: {}", barcode, e.getMessage())
                )
                .onErrorResume(e -> Mono.empty());
    }

    // =========================
    // DTO → PosItem Conversion
    // =========================
    private PosItem toPosItem(ProductResponse p) {
        log.trace("Converting ProductResponse to PosItem for code: {}", p.code());
        
        PosItem item = new PosItem();

        // Basic Information
        item.setCode(p.code());
        item.setBarcode(p.barcode());
        item.setDescription(p.description());

        // Pricing logic
        double original = safe(p.originalPrice(), p.sellingPrice());
        double selling  = safe(p.sellingPrice(), original);

        item.setOriginalPrice(original);
        item.setSellingPrice(selling);

        // Inventory / Quantity
        item.setStock(p.stock());
        item.setQty(0); 

        // Initialize discounts
        item.applyDiscount(0, 0);

        // Timestamp
        item.setUpdated(LocalDateTime.now());

        return item;
    }

    // =========================
    // null-safe utility
    // =========================
    private double safe(Double value, Double fallback) {
        if (value != null) return value;
        if (fallback == null) {
            log.trace("Both value and fallback are null, defaulting to 0.0");
            return 0.0;
        }
        return fallback;
    }
}