package com.swna.javafx.admin.supplier.service;


import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.swna.javafx.admin.supplier.api.SupplierApiClient;
import com.swna.javafx.admin.supplier.dto.SupplierResponseRecord;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierApiClient supplierApiClient;

    /**
     * 전체 거래처 목록 조회 (Mono 반환)
     */
    public Mono<List<SupplierResponseRecord>> getAllSuppliers() {
        return supplierApiClient.getAllSuppliers()
            .doOnSubscribe(sub -> log.debug("[SupplierService] Fetching all suppliers..."))
            .doOnSuccess(suppliers -> log.debug("[SupplierService] Fetched {} suppliers", suppliers.size()))
            .doOnError(error -> log.error("[SupplierService] Failed to fetch suppliers", error));
    }
    
    /**
     * 활성화된 거래처 목록 조회
     */
    public Mono<List<SupplierResponseRecord>> getActiveSuppliers() {
        return supplierApiClient.getActiveSuppliers();
    }
    
    /**
     * 거래처 단건 조회
     */
    public Mono<SupplierResponseRecord> getSupplierById(Long id) {
        return supplierApiClient.getSupplierById(id);
    }
    
    /**
     * 거래처 검색
     */
    public Mono<List<SupplierResponseRecord>> searchSuppliers(String keyword) {
        return supplierApiClient.searchSuppliers(keyword);
    }
    
    // =========================================================
    // 콜백 기반 비동기 메서드 (JavaFX UI용)
    // =========================================================
    
    /**
     * 전체 거래처 조회 (콜백 방식)
     */
    public void getAllSuppliersAsync(Runnable onLoading, 
                                      Consumer<List<SupplierResponseRecord>> onSuccess, 
                                      Consumer<Throwable> onError) {
        
        if (onLoading != null) {
            Platform.runLater(onLoading);
        }
        
        getAllSuppliers()
            .subscribe(
                suppliers -> Platform.runLater(() -> onSuccess.accept(suppliers)),
                error -> Platform.runLater(() -> onError.accept(error))
            );
    }
    
    /**
     * 활성화된 거래처 조회 (콜백 방식)
     */
    public void getActiveSuppliersAsync(Consumer<List<SupplierResponseRecord>> onSuccess, 
                                         Consumer<Throwable> onError) {
        
        getActiveSuppliers()
            .subscribe(
                suppliers -> Platform.runLater(() -> onSuccess.accept(suppliers)),
                error -> Platform.runLater(() -> onError.accept(error))
            );
    }
    
    /**
     * 거래처 검색 (콜백 방식)
     */
    public void searchSuppliersAsync(String keyword,
                                      Consumer<List<SupplierResponseRecord>> onSuccess,
                                      Consumer<Throwable> onError) {
        
        searchSuppliers(keyword)
            .subscribe(
                suppliers -> Platform.runLater(() -> onSuccess.accept(suppliers)),
                error -> Platform.runLater(() -> onError.accept(error))
            );
    }
}