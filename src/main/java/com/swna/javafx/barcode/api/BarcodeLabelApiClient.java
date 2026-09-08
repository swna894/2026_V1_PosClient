package com.swna.javafx.barcode.api;

import java.time.Duration;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 바코드 라벨 API 클라이언트
 * WebClientCommon을 사용하여 API 통신 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BarcodeLabelApiClient {

    private final SimpleApiClient webClientCommon;
    
    // 1. 백엔드 매핑 경로와 일치하도록 수정
    private static final String API_PRODUCT_LABELS = "/api/v1/products/labels";
    private static final String API_SEARCH_SUPPLIER = "/api/v1/products/labels/search/supplier";
    
    // 타임아웃 및 재시도 설정
    private static final int API_TIMEOUT_SECONDS = 30;
    private static final int RETRY_COUNT = 3;
    
    private static final ParameterizedTypeReference<ApiResponse<List<BarcodeLabelDto>>> LABEL_LIST_TYPE = 
        new ParameterizedTypeReference<ApiResponse<List<BarcodeLabelDto>>>() {};
    
    /**
     * 라벨 데이터 목록 조회
     */
    public Mono<List<BarcodeLabelDto>> getLabelDataList() {
        log.debug("[Label API] Fetching label data from: {}", API_PRODUCT_LABELS);
        
        return webClientCommon.get(API_PRODUCT_LABELS, LABEL_LIST_TYPE)
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .flatMap(this::unwrapResponse)
            .onErrorResume(e -> {
                log.error("[Label API] Failed to fetch label data", e);
                return Mono.just(List.of());
            });
    }

    /**
     * 특정 업체명에 해당하는 라벨 목록 조회
     * @param name 업체명 (company)
     */
    public Mono<List<BarcodeLabelDto>> getLabelsBySupplier(String name) {
        // 2. UriComponentsBuilder를 통한 안전한 URL 및 쿼리 파라미터 빌드 (특수문자 자동 인코딩)
        String url = UriComponentsBuilder.fromPath(API_SEARCH_SUPPLIER)
                .queryParam("name", name == null ? "" : name)
                .build()
                .toUriString();
        
        log.debug("[Label API] Fetching labels by supplier: {} from: {}", name, url);
        
        return webClientCommon.get(url, LABEL_LIST_TYPE)
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .flatMap(this::unwrapResponse)
            .onErrorResume(e -> {
                log.error("[Label API] Failed to fetch labels for supplier: {}", name, e);
                return Mono.just(List.of());
            });
    }

    /**
     * ApiResponse 언래핑
     */
    private Mono<List<BarcodeLabelDto>> unwrapResponse(ApiResponse<List<BarcodeLabelDto>> response) {
        if (response == null) {
            log.warn("[Label API] Received null response");
            return Mono.just(List.of());
        }
        
        if (response.isSuccess() && response.hasData()) {
            List<BarcodeLabelDto> data = response.data();
            log.debug("[Label API] Success - Fetched {} labels", data.size());
            return Mono.just(data);
        } else {
            log.warn("[Label API] Failed - Code: {}, Message: {}", response.code(), response.message());
            return Mono.just(List.of());
        }
    }
}