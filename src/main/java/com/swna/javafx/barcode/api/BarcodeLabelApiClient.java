package com.swna.javafx.barcode.api;

import java.time.Duration;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.common.api.WebClientCommon;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 바코드 라벨 API 클라이언트
 * WebClientCommon을 사용하여 API 통신 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BarcodeLabelApiClient {

    private final WebClientCommon webClientCommon;
    
    // API Endpoint 상수
    private static final String API_PRODUCT_LABELS = "/products/labels";
    
    // 타임아웃 설정
    private static final int API_TIMEOUT_SECONDS = 30;
    private static final int RETRY_COUNT = 3;
    
    // ParameterizedTypeReference 상수
    private static final ParameterizedTypeReference<ApiResponse<List<BarcodeLabelDto>>> LABEL_LIST_TYPE = 
        new ParameterizedTypeReference<ApiResponse<List<BarcodeLabelDto>>>() {};
    
    /**
     * 라벨 데이터 목록 조회
     * 
     * @return 라벨 DTO 리스트 Mono
     */
    public Mono<List<BarcodeLabelDto>> getLabelDataList() {
        log.debug("[Label API] Fetching label data from: {}", API_PRODUCT_LABELS);
        
        return webClientCommon.get(API_PRODUCT_LABELS, LABEL_LIST_TYPE)
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(this::unwrapResponse)
            .onErrorResume(e -> {
                log.error("[Label API] Failed to fetch label data", e);
                return Mono.just(List.of());  // 에러 시 빈 리스트 반환
            });
    }
    

    /**
     * 특정 업체명에 해당하는 라벨 목록 조회 및 리팩토링된 공통 로직 적용
     * @param name 업체명 (company)
     */
    public Mono<List<BarcodeLabelDto>> getLabelsBySupplier(String name) {
        // URL 생성 (name이 "전체"인 경우 기본 API 호출을 고려할 수 있습니다)
        String url = "/products/labels/search/supplier?name=" + (name == null ? "" : name);
        
        log.debug("[Label API] Fetching labels by supplier: {} from: {}", name, url);
        
        return webClientCommon.get(url, LABEL_LIST_TYPE)
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS)) // 타임아웃 설정
            .retry(RETRY_COUNT)                              // 실패 시 재시도
            .subscribeOn(Schedulers.boundedElastic())        // 탄력적 스케줄러 사용
            .flatMap(this::unwrapResponse)                   // ApiResponse에서 데이터 추출
            .onErrorResume(e -> {
                log.error("[Label API] Failed to fetch labels for supplier: {}", name, e);
                return Mono.just(List.of());                 // 에러 발생 시 빈 리스트 반환
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
