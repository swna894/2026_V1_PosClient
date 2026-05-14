package com.swna.javafx.admin.supplier.service;

import com.swna.javafx.admin.supplier.api.SupplierApiClient;
import com.swna.javafx.admin.supplier.dto.SupplierRequestRecord;
import com.swna.javafx.admin.supplier.dto.SupplierResponseRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SupplierRepository {

    private final SupplierApiClient apiClient;

    /**
     * 전체 조회
     */
    public Mono<List<SupplierResponseRecord>> findAll() {
        return apiClient.getAllSuppliers()
                .flatMap(ResponseHandler::unwrap);
    }

    /**
     * 검색
     */
    public Mono<List<SupplierResponseRecord>> search(String keyword) {
        return apiClient.searchSuppliers(keyword)
                .flatMap(ResponseHandler::unwrap);
    }

    /**
     * 단건 조회
     */
    public Mono<SupplierResponseRecord> findById(Long id) {
        return apiClient.getSupplierById(id)
                .flatMap(ResponseHandler::unwrap);
    }

    /**
     * 저장 (생성 또는 수정)
     */
    public Mono<SupplierResponseRecord> save(SupplierRequestRecord request) {
        if (request.id() == null || request.id() == 0) {
            // 새 거래처 생성
            return apiClient.createSupplier(request)
                    .flatMap(ResponseHandler::unwrap);
        } else {
            // 기존 거래처 수정
            return apiClient.updateSupplier(request.id(), request)
                    .flatMap(ResponseHandler::unwrap);
        }
    }

    /**
     * 생성
     */
    public Mono<SupplierResponseRecord> create(SupplierRequestRecord request) {
        return apiClient.createSupplier(request)
                .flatMap(ResponseHandler::unwrap);
    }

    /**
     * 수정
     */
    public Mono<SupplierResponseRecord> update(Long id, SupplierRequestRecord request) {
        return apiClient.updateSupplier(id, request)
                .flatMap(ResponseHandler::unwrap);
    }

    /**
     * 삭제
     */
    public Mono<Void> delete(Long id) {
        return apiClient.deleteSupplier(id)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new RuntimeException(response.message()));
                    }
                });
    }
}