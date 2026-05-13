package com.swna.javafx.admin.supplier.api;

import com.swna.javafx.admin.supplier.dto.SupplierResponseRecord;
import com.swna.javafx.common.api.WebClientCommon;
import com.swna.javafx.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierApiClient {

    private final WebClientCommon webClientCommon;
    
    // API Endpoint 상수
    private static final String API_SUPPLIERS_BASE = "/suppliers";
    
    // ParameterizedTypeReference 상수
    private static final ParameterizedTypeReference<ApiResponse<List<SupplierResponseRecord>>> SUPPLIER_LIST_TYPE = 
        new ParameterizedTypeReference<ApiResponse<List<SupplierResponseRecord>>>() {};
    
    private static final ParameterizedTypeReference<ApiResponse<SupplierResponseRecord>> SUPPLIER_SINGLE_TYPE = 
        new ParameterizedTypeReference<ApiResponse<SupplierResponseRecord>>() {};
    
    private static final ParameterizedTypeReference<ApiResponse<Void>> VOID_TYPE = 
        new ParameterizedTypeReference<ApiResponse<Void>>() {};
    
    // =========================================================
    // GET 요청
    // =========================================================
    
    /**
     * 전체 거래처 목록 조회
     */
    public Mono<List<SupplierResponseRecord>> getAllSuppliers() {
        log.info("[Supplier API] Fetching all suppliers from: {}", API_SUPPLIERS_BASE);
        
        return webClientCommon.get(API_SUPPLIERS_BASE, SUPPLIER_LIST_TYPE)
            .flatMap(this::unwrapListResponse)
            .doOnSuccess(suppliers -> log.info("[Supplier API] Success - Fetched {} suppliers", suppliers.size()))
            .doOnError(error -> log.error("[Supplier API] Failed to fetch suppliers: {}", error.getMessage()));
    }
    
    /**
     * 활성화된 거래처 목록 조회
     */
    public Mono<List<SupplierResponseRecord>> getActiveSuppliers() {
        String url = API_SUPPLIERS_BASE + "/active";
        log.info("[Supplier API] Fetching active suppliers from: {}", url);
        
        return webClientCommon.get(url, SUPPLIER_LIST_TYPE)
            .flatMap(this::unwrapListResponse)
            .doOnSuccess(suppliers -> log.info("[Supplier API] Success - Fetched {} active suppliers", suppliers.size()));
    }
    
    /**
     * 거래처 단건 조회
     */
    public Mono<SupplierResponseRecord> getSupplierById(Long id) {
        String url = API_SUPPLIERS_BASE + "/" + id;
        log.info("[Supplier API] Fetching supplier by id: {}", id);
        
        return webClientCommon.get(url, SUPPLIER_SINGLE_TYPE)
            .flatMap(this::unwrapSingleResponse)
            .doOnSuccess(supplier -> log.info("[Supplier API] Success - Fetched supplier: {}", supplier.name()))
            .doOnError(error -> log.error("[Supplier API] Failed to fetch supplier {}: {}", id, error.getMessage()));
    }
    
    /**
     * 약어로 거래처 조회
     */
    public Mono<SupplierResponseRecord> getSupplierByAbbr(String abbr) {
        String url = API_SUPPLIERS_BASE + "/abbr/" + abbr;
        log.info("[Supplier API] Fetching supplier by abbr: {}", abbr);
        
        return webClientCommon.get(url, SUPPLIER_SINGLE_TYPE)
            .flatMap(this::unwrapSingleResponse);
    }
    
    /**
     * 키워드로 거래처 검색
     */
    public Mono<List<SupplierResponseRecord>> searchSuppliers(String keyword) {
        String url = API_SUPPLIERS_BASE + "/search?keyword=" + (keyword != null ? keyword : "");
        log.info("[Supplier API] Searching suppliers with keyword: {}", keyword);
        
        return webClientCommon.get(url, SUPPLIER_LIST_TYPE)
            .flatMap(this::unwrapListResponse);
    }
    
    /**
     * 활성화된 거래처 검색
     */
    public Mono<List<SupplierResponseRecord>> searchActiveSuppliers(String keyword) {
        String url = API_SUPPLIERS_BASE + "/search/active?keyword=" + (keyword != null ? keyword : "");
        log.info("[Supplier API] Searching active suppliers with keyword: {}", keyword);
        
        return webClientCommon.get(url, SUPPLIER_LIST_TYPE)
            .flatMap(this::unwrapListResponse);
    }
    
    // =========================================================
    // 응답 언래핑 메서드
    // =========================================================
    
    private Mono<List<SupplierResponseRecord>> unwrapListResponse(ApiResponse<List<SupplierResponseRecord>> response) {
        if (response == null) {
            return Mono.error(new RuntimeException("Empty response from server"));
        }
        
        if (response.isSuccess() && response.hasData()) {
            return Mono.just(response.data());
        } else {
            String errorMsg = String.format("API Error - Code: %s, Message: %s", 
                response.code(), response.message());
            log.error(errorMsg);
            return Mono.error(new RuntimeException(errorMsg));
        }
    }
    
    private Mono<SupplierResponseRecord> unwrapSingleResponse(ApiResponse<SupplierResponseRecord> response) {
        if (response == null) {
            return Mono.error(new RuntimeException("Empty response from server"));
        }
        
        if (response.isSuccess() && response.hasData()) {
            return Mono.just(response.data());
        } else {
            String errorMsg = String.format("API Error - Code: %s, Message: %s", 
                response.code(), response.message());
            log.error(errorMsg);
            return Mono.error(new RuntimeException(errorMsg));
        }
    }
}