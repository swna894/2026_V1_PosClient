package com.swna.javafx.barcode.service;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.barcode.infrastructre.PdfLabelGenerator;
import com.swna.javafx.barcode.repository.BarcodeApiClient;
import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.common.util.PdfOpenUtil;

import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarcodeLabelPrintService {

    private final CommonApiClient apiClient; // 공통 클라이언트로 교체[cite: 23]
    private final ApiEndpointMapper mapper;
    private final PdfLabelGenerator pdfGenerator;

    /** UI 리스트용 데이터 가져오기 */
    public Mono<List<BarcodeLabelDto>> getLabelDataList() {
        // 1. 매퍼에서 수정된 메타데이터(ApiResponse 기반)를 가져옴
        ApiEndpointMapper.DomainMetadata metadata = mapper.getMetadata("label_list");

        // 2. 공통 클라이언트에 명시적인 제네릭 타입 힌트 제공[cite: 28]
        return apiClient.<ApiResponse<List<BarcodeLabelDto>>>requestMono(metadata, null, null)
                    .map(ApiResponse::data) // [중요] ApiResponse record의 data 필드 추출
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(e -> {
                        log.error("라벨 데이터 로드 실패", e);
                        return Mono.just(List.of()); // 에러 발생 시 빈 리스트 반환[cite: 26]
                    });
    }

    /** PDF 생성 및 자동 열기 프로세스[cite: 5] */
/** PDF 생성 로직[cite: 20] */
    public Mono<Void> generateLabels() {
        return getLabelDataList()
                .flatMap(this::processPdfGeneration)
                .then();
    }

    private Mono<Void> processPdfGeneration(List<BarcodeLabelDto> products) {
        return Mono.fromRunnable(() -> {
            try {
                pdfGenerator.generate(products);
                Platform.runLater(() -> {
                    try {
                        PdfOpenUtil.open("labels65.pdf");
                    } catch (Exception e) {
                        log.error("File open error", e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException("PDF Generation failed", e);
            }
        });
    }
}