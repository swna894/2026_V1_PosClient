package com.swna.javafx.pos.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.ApiResponseException;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.response.ProductResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j 
@Service
@RequiredArgsConstructor
public class PosService {

    
    private final CommonApiClient apiClient;
    private final ApiEndpointMapper apiMapper;
    
    // API 호출 타임아웃 (초)
    private static final int API_TIMEOUT_SECONDS = 10;
    
    // 재시도 횟수
    private static final int RETRY_COUNT = 2;
    
    // 간단한 인메모리 캐시 (선택사항)
    private final Map<String, PosItem> itemCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 300;

  // =========================
    // 메인 스캔 메서드 (타입 안전)
    // =========================
    
    /**
     * 바코드로 상품 스캔 (타입 안전한 버전)
     * 
     * @param barcode 스캔할 바코드
     * @return PosItem Mono (존재하지 않으면 empty)
     */
    public Mono<PosItem> scan(String barcode) {
        // 입력 검증
        if (barcode == null || barcode.isBlank()) {
            log.warn("[Scan] Barcode is null or empty");
            return Mono.empty();
        }
        
        // 1. 캐시 확인 (선택사항)
        PosItem cached = itemCache.get(barcode);
        if (cached != null) {
            log.debug("[Scan] Cache hit for barcode: {}", barcode);
            return Mono.just(cached);
        }
        
        // 2. 타입 안전한 메타데이터 조회
        var metadata = apiMapper.<ApiResponse<ProductResponse>>getMetadata("barcode_search");
        
        // 3. 경로 변수 설정
        Map<String, Object> pathVars = Map.of("barcode", barcode);
        
        // 4. API 호출 (getForData 사용 - 자동 언래핑)
        return apiClient.getForData(metadata, pathVars, null)
                .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                .retry(RETRY_COUNT)
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::toPosItemWithCache)
                .doOnSuccess(item -> {
                    if (item != null) {
                        log.info("[Scan] Product found: {} ({})", item.getDescription(), barcode);
                    }
                })
                .onErrorResume(ApiResponseException.class, e -> {
                    // API 응답 실패 (비즈니스 에러)
                    log.warn("[Scan] API error for barcode {}: code={}, message={}", 
                        barcode, e.getCode(), e.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(TimeoutException.class, e -> {
                    // 타임아웃 에러
                    log.error("[Scan] Timeout for barcode {}: {}", barcode, e.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    // 기타 에러
                    log.error("[Scan] Unexpected error for barcode {}: {}", barcode, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    // =========================
    // 캐시 관리 메서드
    // =========================
    
    /**
     * 캐시에 아이템 저장
     */
    private PosItem toPosItemWithCache(ProductResponse response) {
        PosItem item = toPosItem(response);
        String barcode = response.barcode();
        
        // 캐시 크기 제한
        if (itemCache.size() >= CACHE_MAX_SIZE) {
            // 간단한 LRU: 가장 오래된 항목 제거
            String firstKey = itemCache.keySet().iterator().next();
            itemCache.remove(firstKey);
            log.debug("[Cache] Removed oldest item: {}", firstKey);
        }
        
        itemCache.put(barcode, item);
        log.debug("[Cache] Added item for barcode: {}", barcode);
        
        return item;
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        itemCache.clear();
        log.info("[Cache] Cleared all cached items");
    }
    
    /**
     * 특정 바코드 캐시 제거
     */
    public void evictCache(String barcode) {
        PosItem removed = itemCache.remove(barcode);
        if (removed != null) {
            log.debug("[Cache] Evicted item for barcode: {}", barcode);
        }
    }

    // =========================
    // DTO → PosItem Conversion
    // =========================
    private PosItem toPosItem(ProductResponse p) {
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