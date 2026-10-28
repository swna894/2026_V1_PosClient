package com.swna.javafx.pos.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.swna.javafx.pos.api.ProductApiService;
import com.swna.javafx.pos.dto.response.ProductResponse;
import com.swna.javafx.pos.model.PosItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 바코드 스캔 서비스.
 *
 * 핵심 원칙:
 *  - 캐시에는 "서버 응답(ProductResponse, 불변 record)"만 보관한다.
 *  - 스캔할 때마다 새 PosItem 을 만들어 반환한다. (qty=0, 할인=0)
 *    → 장바구니, 보류(Hold), 이전 거래와 객체를 공유하지 않으므로 수량 오염이 생기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private static final int API_TIMEOUT_SECONDS = 10;
    private static final int RETRY_COUNT = 2;

    private static final int CACHE_MAX_SIZE = 300;
    /** 가격/재고가 오래된 값으로 남지 않도록 만료 시간을 둔다. */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ProductApiService productApi;

    /** 캐시 항목: 응답 + 저장 시각 */
    private record CachedProduct(ProductResponse response, Instant cachedAt) {
        boolean isExpired() {
            return cachedAt.plus(CACHE_TTL).isBefore(Instant.now());
        }
    }

    /** access-order LinkedHashMap 으로 진짜 LRU 구현 (동기화 래핑) */
    private final Map<String, CachedProduct> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedProduct> eldest) {
                    boolean evict = size() > CACHE_MAX_SIZE;
                    if (evict) {
                        log.debug("[Cache] Evicted eldest: {}", eldest.getKey());
                    }
                    return evict;
                }
            });

    // =========================
    // 스캔
    // =========================

    /**
     * 바코드로 상품 조회. 호출할 때마다 새로운 PosItem(qty=0)을 반환한다.
     */
    public Mono<PosItem> scan(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            log.warn("[Scan] Barcode is null or empty");
            return Mono.empty();
        }

        final String key = barcode.trim();

        ProductResponse cached = getFromCache(key);
        if (cached != null) {
            log.debug("[Scan] Cache hit: {}", key);
            return Mono.just(toPosItem(cached));
        }

        return productApi.getProductByBarcode(key)
                .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                .retry(RETRY_COUNT)
                .doOnNext(response -> putToCache(key, response))
                .map(this::toPosItem)
                .doOnError(e -> log.error("[Scan] Failed for barcode: {}", key, e));
    }

    // =========================
    // 캐시 관리
    // =========================

    private ProductResponse getFromCache(String key) {
        CachedProduct entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("[Cache] Expired: {}", key);
            return null;
        }
        return entry.response();
    }

    private void putToCache(String key, ProductResponse response) {
        // 스캔에 사용한 바코드를 키로 사용 (response.barcode() 와 다를 경우에도 hit 보장)
        cache.put(key, new CachedProduct(response, Instant.now()));
        log.debug("[Cache] Stored: {}", key);
    }

    /** 전체 캐시 초기화 (가격/재고 갱신이 필요할 때) */
    public void clearCache() {
        cache.clear();
        log.info("[Cache] Cleared all cached products");
    }

    /** 특정 바코드 캐시 제거 */
    public void evictCache(String barcode) {
        if (barcode != null && cache.remove(barcode.trim()) != null) {
            log.debug("[Cache] Evicted: {}", barcode);
        }
    }

    // =========================
    // ProductResponse → PosItem (매번 새 객체)
    // =========================

    private PosItem toPosItem(ProductResponse p) {
        PosItem item = new PosItem();

        item.setCode(p.code());
        item.setBarcode(p.barcode());
        item.setDescription(p.description());

        double original = safe(p.price(), p.sellingPrice());
        double selling = safe(p.sellingPrice(), original);
        item.setOriginalPrice(original);
        item.setSellingPrice(selling);

        item.setStock(p.stock());
        item.setQty(0);
        item.applyDiscount(0, 0);

        item.setUpdated(LocalDateTime.now(ZoneId.systemDefault()));
        return item;
    }

    private double safe(Double value, Double fallback) {
        if (value != null) return value;
        return (fallback != null) ? fallback : 0.0;
    }
}
