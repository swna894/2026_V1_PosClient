package com.swna.javafx.admin.inventory.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.inventory.model.InventoryCreateRequest;
import com.swna.javafx.admin.inventory.model.InventoryUpdateRequest;
import com.swna.javafx.admin.inventory.model.InvoentoryResponse;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryApiClient {

    private final SimpleApiClient webClientCommon;

    private static final String API_PRODUCTS = "/products";

    // ==========================================
    // Create
    // ==========================================

    /**
     * 상품(재고) 신규 등록
     */
    public Mono<ApiResponse<InvoentoryResponse>> createProduct(InventoryCreateRequest request) {
        log.info("[API] POST {} - body: {}", API_PRODUCTS, request);
        return webClientCommon.post(API_PRODUCTS, request, TypeReferences.single(InvoentoryResponse.class));
    }

    // ==========================================
    // Read
    // ==========================================

    /**
     * 단건 조회 (ID)
     */
    public Mono<ApiResponse<InvoentoryResponse>> getProductById(Long id) {
        String url = API_PRODUCTS + "/" + id;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.single(InvoentoryResponse.class));
    }

    /**
     * 단건 조회 (바코드)
     */
    public Mono<ApiResponse<InvoentoryResponse>> getProductByBarcode(String barcode) {
        String url = API_PRODUCTS + "/barcode/" + barcode;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.single(InvoentoryResponse.class));
    }

    /**
     * 전체 상품(재고) 조회
     */
    public Mono<ApiResponse<List<InvoentoryResponse>>> getAllProducts() {
        log.info("[API] GET {}", API_PRODUCTS);
        return webClientCommon.get(API_PRODUCTS, TypeReferences.list(InvoentoryResponse.class));
    }

    /**
     * 거래처 약어(abbr)로 상품 목록 조회 (예: 특정 공급사의 취급 상품 목록)
     */
    public Mono<ApiResponse<List<InvoentoryResponse>>> getProductsByAbbr(String abbr) {
        String url = API_PRODUCTS + "/abbr/" + abbr;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.list(InvoentoryResponse.class));
    }

    // ==========================================
    // Update
    // ==========================================

    /**
     * 상품(재고) 정보 수정 (가격/재고 수량 등)
     */
    public Mono<ApiResponse<InvoentoryResponse>> updateProduct(Long id, InventoryUpdateRequest request) {
        String url = API_PRODUCTS + "/" + id;
        log.info("[API] PUT {} - body: {}", url, request);
        return webClientCommon.put(url, request, TypeReferences.single(InvoentoryResponse.class));
    }

    /**
     * 상품(재고) 일괄 수정
     */
    public Mono<ApiResponse<List<InvoentoryResponse>>> updateProductsBulk(List<InventoryUpdateRequest> requests) {
        String url = API_PRODUCTS + "/bulk";
        log.info("[API] PUT {} - {} items", url, requests.size());
        return webClientCommon.put(url, requests, TypeReferences.list(InvoentoryResponse.class));
    }


    // ==========================================
    // Delete
    // ==========================================

    /**
     * 상품(재고) 삭제
     */
    public Mono<ApiResponse<Void>> deleteProduct(Long id) {
        String url = API_PRODUCTS + "/" + id;
        log.info("[API] DELETE {}", url);
        return webClientCommon.delete(url, TypeReferences.VOID_TYPE);
    }

    /**
     * 상품(재고) 일괄 삭제
     */
    public Mono<ApiResponse<Integer>> deleteProductsBulk(List<Long> ids) {
        String url = API_PRODUCTS + "/bulk";
        log.info("[API] DELETE {} - {} ids", url, ids.size());
        return webClientCommon.delete(url, ids, TypeReferences.single(Integer.class));
    }
}