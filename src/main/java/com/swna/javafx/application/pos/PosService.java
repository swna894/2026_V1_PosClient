package com.swna.javafx.application.pos;

import org.springframework.stereotype.Service;

import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.dto.pos.ProductResponse;
import com.swna.javafx.infrastructure.pos.ProductClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PosService {

    private final ProductClient productClient;

    // =========================
    // Scan (비동기)
    // =========================
    public Mono<PosItem> scan(String barcode) {

        if (barcode == null || barcode.isBlank()) {
            return Mono.empty();
        }

        return productClient.findByBarcode(barcode)
                .map(this::toPosItem)
                .doOnError(e ->
                        System.err.println("상품 조회 실패: " + e.getMessage())
                )
                .onErrorResume(e -> Mono.empty());
    }

    // =========================
    // DTO → PosItem 변환
    // =========================
    private PosItem toPosItem(ProductResponse p) {

        PosItem item = new PosItem();

        // =========================
        // 기본 정보
        // =========================
        item.setCode(p.code());
        item.setBarcode(p.barcode());
        item.setDescription(p.description());

        // =========================
        // 가격 (🔥 중요)
        // =========================
        double original = safe(p.originalPrice(), p.sellingPrice());
        double selling  = safe(p.sellingPrice(), original);

        item.setOriginalPrice(original);
        item.setSellingPrice(selling);

        // =========================
        // 재고 / 수량
        // =========================
        item.setStock(p.stock());
        item.setQty(0); // 초기 수량

        // =========================
        // 할인 초기화
        // =========================
        item.applyDiscount(0, 0);

        // =========================
        // 시간
        // =========================
        item.setUpdated(LocalDateTime.now());

        return item;
    }

    // =========================
    // null-safe 유틸
    // =========================
    private double safe(Double value, Double fallback) {
        if (value != null) return value;
        return fallback != null ? fallback : 0.0;
    }
}