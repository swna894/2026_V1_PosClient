package com.swna.javafx.admin.supplier.service;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.dto.SupplierRequestRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

    /**
     * 전체 거래처 조회
     */
    public Mono<List<Supplier>> getAllSuppliers() {
        return repository.findAll()
                .map(records -> records.stream()
                        .map(Supplier::from)
                        .toList()
                );
    }

    /**
     * 거래처 검색
     */
    public Mono<List<Supplier>> searchSuppliers(String keyword) {
        return repository.search(keyword)
                .map(records -> records.stream()
                        .map(Supplier::from)
                        .toList()
                );
    }

    /**
     * 단건 조회
     */
    public Mono<Supplier> getSupplier(Long id) {
        return repository.findById(id)
                .map(Supplier::from);
    }

    /**
     * 거래처 저장 (생성 또는 수정)
     */
    public Mono<Supplier> saveSupplier(Supplier supplier) {
        SupplierRequestRecord request = SupplierRequestRecord.from(supplier);
        
        return repository.save(request)
                .map(Supplier::from)
                .doOnSuccess(saved -> log.info("Saved supplier: {}", saved.getId()))
                .doOnError(error -> log.error("Failed to save supplier: {}", error.getMessage()));
    }

    /**
     * 새 거래처 생성
     */
    public Mono<Supplier> createSupplier(Supplier supplier) {
        SupplierRequestRecord request = SupplierRequestRecord.from(supplier);
        
        return repository.create(request)
                .map(Supplier::from)
                .doOnSuccess(created -> log.info("Created supplier: {}", created.getId()))
                .doOnError(error -> log.error("Failed to create supplier: {}", error.getMessage()));
    }

    /**
     * 거래처 수정
     */
    public Mono<Supplier> updateSupplier(Long id, Supplier supplier) {
        SupplierRequestRecord request = SupplierRequestRecord.from(supplier);
        
        return repository.update(id, request)
                .map(Supplier::from)
                .doOnSuccess(updated -> log.info("Updated supplier: {}", updated.getId()))
                .doOnError(error -> log.error("Failed to update supplier: {}", error.getMessage()));
    }

    /**
     * 거래처 삭제
     */
    public Mono<Void> deleteSupplier(Long id) {
        return repository.delete(id)
                .doOnSuccess(v -> log.info("Deleted supplier: {}", id))
                .doOnError(error -> log.error("Failed to delete supplier: {}", error.getMessage()));
    }
}