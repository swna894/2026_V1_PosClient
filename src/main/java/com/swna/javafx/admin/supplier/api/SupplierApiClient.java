package com.swna.javafx.admin.supplier.api;

import com.swna.javafx.admin.supplier.dto.SupplierRequestRecord;
import com.swna.javafx.admin.supplier.dto.SupplierResponseRecord;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierApiClient {

    private final SimpleApiClient webClientCommon;

    private static final String API_SUPPLIERS = "/suppliers";

    /**
     * 전체 거래처 조회
     */
    public Mono<ApiResponse<List<SupplierResponseRecord>>> getAllSuppliers() {
        log.info("[API] GET {}", API_SUPPLIERS);
        return webClientCommon.get(API_SUPPLIERS, TypeReferences.list(SupplierResponseRecord.class));
    }

    /**
     * 검색
     */
    public Mono<ApiResponse<List<SupplierResponseRecord>>> searchSuppliers(String keyword) {
        String url = API_SUPPLIERS + "/search?keyword=" + (keyword == null ? "" : keyword);
        log.info("[API] SEARCH : {}", keyword);
        return webClientCommon.get(url, TypeReferences.list(SupplierResponseRecord.class));
    }

    /**
     * 단건 조회
     */
    public Mono<ApiResponse<SupplierResponseRecord>> getSupplierById(Long id) {
        String url = API_SUPPLIERS + "/" + id;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.single(SupplierResponseRecord.class));
    }

    /**
     * 거래처 생성 (POST)
     */
    public Mono<ApiResponse<SupplierResponseRecord>> createSupplier(SupplierRequestRecord request) {
        log.info("[API] POST {} - body: {}", API_SUPPLIERS, request);
        return webClientCommon.post(API_SUPPLIERS, request, TypeReferences.single(SupplierResponseRecord.class));
    }

    /**
     * 거래처 수정 (PUT)
     */
    public Mono<ApiResponse<SupplierResponseRecord>> updateSupplier(Long id, SupplierRequestRecord request) {
        String url = API_SUPPLIERS + "/" + id;
        log.info("[API] PUT {} - body: {}", url, request);
        return webClientCommon.put(url, request, TypeReferences.single(SupplierResponseRecord.class));
    }

    /**
     * 거래처 삭제 (DELETE)
     */
    public Mono<ApiResponse<Void>> deleteSupplier(Long id) {
        String url = API_SUPPLIERS + "/" + id;
        log.info("[API] DELETE {}", url);
        return webClientCommon.delete(url, TypeReferences.VOID_TYPE);
    }
}