package com.swna.javafx.pos.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j 
@Service
@RequiredArgsConstructor
public class PosService {

    
    // 공통 API 컴포넌트 주입[cite: 29, 30]
    private final CommonApiClient apiClient;
    private final ApiEndpointMapper apiMapper;

    /**
     * 바코드를 이용한 상품 스캔
     */
    public Mono<PosItem> scan(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            log.warn("Scan failed: Barcode is null or empty");
            return Mono.empty();
        }

        // 1. 메타데이터 조회 (barcode_search 도메인 사용)[cite: 29]
        ApiEndpointMapper.DomainMetadata metadata = apiMapper.getMetadata("barcode_search");
        
        // 2. 경로 변수 설정[cite: 30]
        Map<String, Object> pathVars = Map.of("barcode", barcode);

        // 3. CommonApiClient를 통한 요청 실행[cite: 30]
        return apiClient.<ApiResponse<ProductResponseDto>>requestMono(metadata, pathVars, null)
                .flatMap(response -> {
                    // 응답 성공 여부 및 데이터 존재 확인
                    if (response.success() && response.data() != null) {
                        log.info("Product found: {} ({})", response.data().description(), barcode);
                        return Mono.just(toPosItem(response.data()));
                    } else {
                        log.warn("Product not found: {} - {}", barcode, response.message());
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.error("API Call failed for barcode {}: {}", barcode, e.getMessage());
                    return Mono.empty();
                });
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