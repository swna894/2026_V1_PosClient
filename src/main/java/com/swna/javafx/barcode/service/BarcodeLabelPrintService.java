package com.swna.javafx.barcode.service;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.barcode.infrastructre.PdfLabelGenerator;
import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
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
 * 1. CommonApiClient의 타입 안전한 메서드 사용 (getForData)
 * 2. 명시적인 제네릭 타입 파라미터 전달
 * 3. 타임아웃 및 재시도 정책 추가
 * 4. 에러 처리 강화
 *//**
 * 바코드 라벨 인쇄 서비스
 * 
 * 리팩토링 주요 변경사항:
 * 1. CommonApiClient의 타입 안전한 메서드 사용 (getForData)
 * 2. 명시적인 제네릭 타입 파라미터 전달
 * 3. 타임아웃 및 재시도 정책 추가
 * 4. 에러 처리 강화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLabelPrintService {

       // API 호출 타임아웃 (초)
    private static final int API_TIMEOUT_SECONDS = 30;
    
    // 재시도 횟수
    private static final int RETRY_COUNT = 3;
    
    private final CommonApiClient apiClient;
    private final ApiEndpointMapper endpointMapper;
    private final PdfLabelGenerator pdfGenerator;


    // =========================
    // Public Methods (Type-Safe)
    // =========================
    
    /**
     * 라벨 데이터 조회 (타입 안전한 버전)
     * 
     * @return 라벨 DTO 리스트 Mono
     */
    public Mono<List<BarcodeLabelDto>> getLabelDataList() {
        // 1. 타입 안전한 메타데이터 조회
        var metadata = endpointMapper.<ApiResponse<List<BarcodeLabelDto>>>getMetadata("label_list");
        
        // 2. Flux를 사용하여 스트림으로 처리 (여러 개의 ApiResponse를 받는 경우)
        //    또는 단일 ApiResponse<List<T>>를 받는 경우 getForData 사용
        return apiClient.getFluxForData(metadata, null, null)
                .collectList()  // Flux<List<T>> → Mono<List<T>>
                .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                .retry(RETRY_COUNT)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("[Label] Failed to load label data", e);
                    return Mono.just(List.of());  // 에러 시 빈 리스트 반환
                });
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
        .then(); // 어떤 Mono든 Void 타입으로 변환
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