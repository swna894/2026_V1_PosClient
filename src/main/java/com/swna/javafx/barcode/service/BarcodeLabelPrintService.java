package com.swna.javafx.barcode.service;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.barcode.infrastructre.PdfLabelGenerator;
import com.swna.javafx.barcode.repository.BarcodeApiClient;
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

    private final BarcodeApiClient apiClient;
    private final PdfLabelGenerator pdfGenerator;

    /** UI 리스트용 데이터 가져오기[cite: 5] */
    public Mono<List<BarcodeLabelDto>> getLabelDataList() {
        return apiClient.getLabels()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(list -> log.info("Fetched {} labels", list.size()))
                .doOnError(e -> log.error("Fetch error", e));
    }

    /** PDF 생성 및 자동 열기 프로세스[cite: 5] */
    public Mono<Void> generateLabels() {
        return apiClient.getLabels()
                .timeout(Duration.ofSeconds(10))
                .retry(2)
                .subscribeOn(Schedulers.boundedElastic())
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