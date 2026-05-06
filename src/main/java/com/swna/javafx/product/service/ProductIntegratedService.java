package com.swna.javafx.product.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.swna.javafx.admin.product.PageResult;
import com.swna.javafx.admin.product.Product;
import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIntegratedService {
    private final CommonApiClient apiClient;
    private final ApiEndpointMapper mapper;

/**
     * 1. 전체 목록 조회 (Flux)
     * [수정] Unknown domain 에러 방지를 위해 매퍼에 정의된 정확한 키("product")를 사용합니다.
     */
    public Flux<Product> fetchProducts() {
        try {
            // "product_list" 대신 매퍼에 등록된 기본 도메인 키 "product" 사용
            ApiEndpointMapper.DomainMetadata metadata = mapper.getMetadata("product");
            return apiClient.requestFlux(metadata, null, null); 
        } catch (IllegalArgumentException e) {
            log.error("API Metadata mapping failed for 'product'", e);
            return Flux.empty();
        }
    }

    // 2. 페이징 조회
    public Mono<PageResult<Product>> fetchPage(String keyword, int page, int size) {
        ApiEndpointMapper.DomainMetadata metadata = mapper.getMetadata("product_page");
        
        // 쿼리 파라미터 맵 타입 명시 (Map<String, Object>)
        Map<String, Object> params = mapper.createPageParams(keyword, page, size);
        
        return apiClient.requestMono(metadata, null, params); 
    }

    // 3. 바코드 조회
    public Mono<ApiResponse<ProductResponseDto>> findByBarcode(String barcode) {
        ApiEndpointMapper.DomainMetadata metadata = mapper.getMetadata("barcode_search");
        
        // 경로 변수 맵 타입 명시 (Map<String, Object>)
        // 이 부분을 Map<String, Object>로 명시해야 CommonApiClient의 인자와 일치합니다.
        Map<String, Object> pathVars = Map.of("barcode", barcode); 
        
        return apiClient.requestMono(metadata, pathVars, null);
    }
}