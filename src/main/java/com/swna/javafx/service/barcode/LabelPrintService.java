package com.swna.javafx.service.barcode;

import javafx.application.Platform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.swna.javafx.common.util.PdfOpenUtil;
import com.swna.javafx.infrastructure.barcode.PdfLabelGenerator;
import com.swna.javafx.infrastructure.barcode.ProductApiClient;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelPrintService {

    private final ProductApiClient apiClient;
    private final PdfLabelGenerator pdfGenerator;

    /**
     * 65칸 바코드 PDF 생성
     */
    public Mono<Void> generateLabels() {

        log.info("LABEL PDF GENERATE START");

        return apiClient.getLabels()

                // timeout
                .timeout(java.time.Duration.ofSeconds(10))
                // retry
                .retry(2)
                // IO 작업은 별도 Thread
                .subscribeOn(  Schedulers.boundedElastic()  )
                .flatMap(products -> {
                    return Mono.fromRunnable(() -> {
                        try {
                            log.info( "PDF GENERATE START size={}", products.size() );
                            // PDF 생성
                            pdfGenerator.generate(products);
                            log.info("PDF GENERATE COMPLETE" );
                            // JavaFX UI Thread
                            Platform.runLater(() -> {
                                try {
                                    // PDF 열기
                                    PdfOpenUtil.open( "labels65.pdf");
                                    log.info("PDF OPEN COMPLETE" );
                                } catch (Exception e) {
                                    log.error(  "PDF OPEN ERROR", e );
                                }
                            });

                        } catch (Exception e) {
                            log.error( "PDF GENERATE ERROR", e );
                            throw new RuntimeException(e);
                        }
                    });
                })

                // 성공 처리
                .doOnSuccess(v ->  log.info(  "LABEL PROCESS COMPLETE" ) )
                // 실패 처리
                .doOnError(error ->  log.error( "LABEL PROCESS ERROR",  error ) )

                // 실패 이후 fallback
                .onErrorResume(error -> {
                    log.warn( "LABEL PROCESS FALLBACK" );
                    // 앱 종료 방지
                    return Mono.empty();
                })
                .then();
    }
}
