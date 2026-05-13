package com.swna.javafx.barcode.service;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.swna.javafx.barcode.api.BarcodeLabelApiClient;
import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.barcode.infrastructre.PdfLabelGenerator;
import com.swna.javafx.common.util.PdfOpenUtil;

import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 바코드 라벨 인쇄 서비스
 * 
 * 리팩토링 주요 변경사항:
 * 1. WebClientCommon 적용 및 API 로직 분리 (BarcodeLabelApiClient)
 * 2. ApiEndpointMapper 및 CommonApiClient 의존성 제거
 * 3. 명확한 책임 분리: API 통신과 비즈니스 로직 분리
 * 4. 코드 간소화 및 가독성 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLabelPrintService {

    private final BarcodeLabelApiClient labelApiClient;
    private final PdfLabelGenerator pdfGenerator;

    // =========================
    // Public Methods
    // =========================
    
    /**
     * 라벨 데이터 조회
     * 
     * @return 라벨 DTO 리스트 Mono
     */
    public Mono<List<BarcodeLabelDto>> getLabelDataList() {
        return labelApiClient.getLabelDataList()
            .doOnSubscribe(sub -> log.debug("[Label] Fetching label data..."))
            .doOnSuccess(data -> log.debug("[Label] Fetched {} labels", data.size()))
            .doOnError(e -> log.error("[Label] Failed to fetch label data", e));
    }

    /**
     * PDF 생성 및 자동 열기 (메인 엔트리 포인트)
     */
    public Mono<Void> generateLabels() {
        return getLabelDataList()
                .flatMap(this::processPdfGeneration)
                .doOnSubscribe(sub -> log.info("[Label] Starting label generation..."))
                .doOnSuccess(v -> log.info("[Label] Label generation completed successfully"))
                .doOnError(e -> log.error("[Label] Label generation failed", e));
    }
    
    /**
     * 특정 상품 리스트로 PDF 생성
     */
    public Mono<Void> generateLabelsForProducts(List<BarcodeLabelDto> products) {
        if (products == null || products.isEmpty()) {
            log.warn("[Label] No products provided for label generation");
            return Mono.empty();
        }
        return processPdfGeneration(products);
    }

    // =========================
    // Private Helper Methods
    // =========================

    /**
     * PDF 생성 및 열기 처리
     */
    private Mono<Void> processPdfGeneration(List<BarcodeLabelDto> products) {
        if (products == null || products.isEmpty()) {
            log.warn("[Label] No label data available for PDF generation");
            return Mono.empty();
        }
        
        return Mono.fromRunnable(() -> {
            log.info("[Label] Generating PDF for {} products", products.size());
            
            try {
                // PDF 생성
                pdfGenerator.generate(products);
                log.info("[Label] PDF generated successfully");
                
                // JavaFX 스레드에서 파일 열기
                Platform.runLater(() -> {
                    try {
                        PdfOpenUtil.open("labels65.pdf");
                        log.info("[Label] PDF opened successfully");
                    } catch (Exception e) {
                        log.error("[Label] Failed to open PDF: {}", e.getMessage(), e);
                    }
                });
                
            } catch (Exception e) {
                log.error("[Label] PDF generation failed", e);
                throw new RuntimeException("PDF Generation failed: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * 비동기 PDF 생성 (Future 스타일, 콜백 지원)
     */
    public void generateLabelsAsync(Runnable onSuccess, Consumer<Throwable> onError) {
        generateLabels()
            .subscribe(
                v -> {
                    if (onSuccess != null) {
                        Platform.runLater(onSuccess);
                    }
                },
                error -> {
                    if (onError != null) {
                        Platform.runLater(() -> onError.accept(error));
                    }
                    log.error("[Label] Async label generation failed", error);
                }
            );
    }
}