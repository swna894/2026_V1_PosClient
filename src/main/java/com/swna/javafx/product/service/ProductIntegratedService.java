package com.swna.javafx.product.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.swna.javafx.admin.product.PageResult;
import com.swna.javafx.admin.product.Product;
import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.ApiResponseException;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.response.ProductResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIntegratedService {

    private final CommonApiClient apiClient;
    private final ApiEndpointMapper mapper;
    
    // API 호출 설정
    private static final int API_TIMEOUT_SECONDS = 30;
    private static final int RETRY_COUNT = 3;
    private static final int PAGE_SIZE_DEFAULT = 20;

    // =========================
    // 1. 전체 목록 조회 (Flux) - 타입 안전 버전
    // =========================
    
    /**
     * 전체 상품 목록 조회 (Flux 스트림)
     * ApiResponse 래퍼 없이 Product 객체를 직접 스트림으로 반환
     * 
     * @return Product Flux
     */
   // =========================
    // 방법 2: ApiResponse<List<Product>> → Flux<Product> 변환
    // =========================
    
    /**
     * 전체 상품 목록 조회 - ApiResponse<List<Product>>를 Flux<Product>로 변환
     * 서버가 ApiResponse로 감싸서 응답하는 경우 사용
     */
    public Flux<Product> fetchProducts() {
        try {
            var metadata = mapper.<ApiResponse<List<Product>>>getMetadata("product_list");
            
            return apiClient.getFluxForData(metadata, null, null)
                    .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                    .retry(RETRY_COUNT)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSubscribe(sub -> log.info("[Product] Fetching all products..."))
                    .doOnNext(product -> log.debug("[Product] Loaded: {} ({})", 
                        product.getDescription(), product.getBarcode()))
                    .doOnComplete(() -> log.info("[Product] Completed fetching all products"))
                    .onErrorResume(e -> {
                        log.error("[Product] Failed to fetch products", e);
                        return Flux.empty();
                    });
                    
        } catch (IllegalArgumentException e) {
            log.error("[Product] Metadata mapping failed for 'product_list'", e);
            return Flux.empty();
        }
    }
    
    /**
     * 전체 목록 조회 - List로 수집
     */
    public Mono<List<Product>> fetchProductList() {
        return fetchProducts()
                .collectList()
                .doOnNext(list -> log.info("[Product] Fetched {} products", list.size()));
    }

    // =========================
    // 2. 페이징 조회 - 타입 안전 버전
    // =========================
    
    /**
     * 페이징 조회 (PageResult 직접 반환)
     * 
     * @param keyword 검색어 (null 가능)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return PageResult<Product> Mono
     */
    public Mono<PageResult<Product>> fetchPage(String keyword, int page, int size) {
        try {
            var metadata = mapper.<PageResult<Product>>getMetadata("product_page");
            
            // 페이징 파라미터 생성
            Map<String, Object> params = mapper.createPageParams(keyword, page, size);
            
            return apiClient.getForPage(metadata, null, params)
                    .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                    .retry(RETRY_COUNT)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSubscribe(sub -> log.info("[Product] Fetching page {} with keyword '{}'", page, keyword))
                    .doOnNext(result -> log.info("[Product] Loaded page {}: {} of {} items", 
                        page, result.getContent().size(), result.getTotalElements()))
                    .onErrorResume(e -> {
                        log.error("[Product] Failed to fetch page {}: {}", page, e.getMessage());
                        return Mono.just(PageResult.empty());
                    });
                    
        } catch (IllegalArgumentException e) {
            log.error("[Product] Metadata mapping failed for 'product_page'", e);
            return Mono.just(PageResult.empty());
        }
    }
    
    /**
     * 페이징 조회 - 기본 페이지 크기 사용
     */
    public Mono<PageResult<Product>> fetchPage(String keyword, int page) {
        return fetchPage(keyword, page, PAGE_SIZE_DEFAULT);
    }
    
    /**
     * 페이징 조회 - 검색어 없이
     */
    public Mono<PageResult<Product>> fetchPage(int page, int size) {
        return fetchPage(null, page, size);
    }

    // =========================
    // 3. 바코드 조회 - 타입 안전 버전
    // =========================
    
    /**
     * 바코드로 상품 조회 (ApiResponse 언래핑 버전)
     * 
     * @param barcode 바코드
     * @return ProductResponse Mono (없으면 empty)
     */
    public Mono<ProductResponse> findByBarcode(String barcode) {
        // 입력 검증
        if (barcode == null || barcode.isBlank()) {
            log.warn("[Product] Barcode is null or empty");
            return Mono.empty();
        }
        
        try {
            var metadata = mapper.<ApiResponse<ProductResponse>>getMetadata("barcode_search");
            Map<String, Object> pathVars = Map.of("barcode", barcode);
            
            return apiClient.getForData(metadata, pathVars, null)
                    .timeout(Duration.ofSeconds(10))
                    .retry(2)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSuccess(product -> {
                        if (product != null) {
                            log.info("[Product] Found product by barcode: {} - {}", 
                                barcode, product.description());
                        }
                    })
                    .onErrorResume(ApiResponseException.class, e -> {
                        if (e.getCode().equals("PRODUCT_NOT_FOUND")) {
                            log.warn("[Product] Product not found for barcode: {}", barcode);
                        } else {
                            log.error("[Product] API error for barcode {}: {}", barcode, e.getMessage());
                        }
                        return Mono.empty();
                    })
                    .onErrorResume(TimeoutException.class, e -> {
                        log.error("[Product] Timeout for barcode: {}", barcode);
                        return Mono.empty();
                    })
                    .onErrorResume(Exception.class, e -> {
                        log.error("[Product] Unexpected error for barcode {}: {}", barcode, e.getMessage());
                        return Mono.empty();
                    });
                    
        } catch (IllegalArgumentException e) {
            log.error("[Product] Metadata mapping failed for 'barcode_search'", e);
            return Mono.empty();
        }
    }
    
    /**
     * 바코드 조회 - 원본 ApiResponse 유지 버전
     */
    public Mono<ApiResponse<ProductResponse>> findByBarcodeWithResponse(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Mono.just(ApiResponse.error("INVALID_BARCODE", "Barcode is required"));
        }
        
        try {
            var metadata = mapper.<ApiResponse<ProductResponse>>getMetadata("barcode_search");
            Map<String, Object> pathVars = Map.of("barcode", barcode);
            
            return apiClient.getForResponse(metadata, pathVars, null)
                    .timeout(Duration.ofSeconds(10))
                    .retry(2)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnNext(response -> {
                        if (response.success()) {
                            log.debug("[Product] API response success for barcode: {}", barcode);
                        } else {
                            log.warn("[Product] API response failed for barcode {}: {}", 
                                barcode, response.message());
                        }
                    });
                    
        } catch (IllegalArgumentException e) {
            log.error("[Product] Metadata mapping failed", e);
            return Mono.just(ApiResponse.error("CONFIG_ERROR", e.getMessage()));
        }
    }

    // =========================
    // 4. 추가 편의 메서드
    // =========================
    
    /**
     * 여러 바코드 일괄 조회
     */
    public Mono<Map<String, ProductResponse>> findByBarcodes(List<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) {
            return Mono.just(Map.of());
        }
        
        return Flux.fromIterable(barcodes)
                .filter(barcode -> barcode != null && !barcode.isBlank())
                .flatMap(barcode -> findByBarcode(barcode)
                    .map(product -> Map.entry(barcode, product)),
                    5) // 동시 요청 제한
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .doOnNext(result -> log.info("[Product] Batch lookup: requested {}, found {}", 
                    barcodes.size(), result.size()));
    }
    
    /**
     * 상품 ID로 조회 (별도 API가 있다고 가정)
     */
    public Mono<ProductResponse> findById(Long id) {
        if (id == null || id <= 0) {
            log.warn("[Product] Invalid product ID: {}", id);
            return Mono.empty();
        }
        
        try {
            var metadata = mapper.<ApiResponse<ProductResponse>>getMetadata("product_detail");
            Map<String, Object> pathVars = Map.of("id", id);
            
            return apiClient.getForData(metadata, pathVars, null)
                    .timeout(Duration.ofSeconds(10))
                    .retry(2)
                    .subscribeOn(Schedulers.boundedElastic());
                    
        } catch (IllegalArgumentException e) {
            log.error("[Product] Metadata mapping failed for 'product_detail'", e);
            return Mono.empty();
        }
    }
    
    /**
     * 상품 검색 (통합 검색)
     */
    public Mono<PageResult<Product>> search(String keyword, int page, int size) {
        return fetchPage(keyword, page, size);
    }
    
    /**
     * 모든 상품 조회 (예외 발생 시 빈 리스트 반환)
     */
    public List<Product> fetchProductsBlocking() {
        return fetchProductList()
                .blockOptional(Duration.ofSeconds(30))
                .orElse(List.of());
    }
}