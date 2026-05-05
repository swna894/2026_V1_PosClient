package com.swna.javafx.product.viewmodel;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.barcode.infrastructre.BarcodeGenerator;
import com.swna.javafx.barcode.infrastructre.PdfLabelGenerator;
import com.swna.javafx.barcode.service.BarcodeLabelPrintService;
import com.swna.javafx.common.viewmodel.BaseViewModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelViewModel extends BaseViewModel {

    private final BarcodeLabelPrintService labelPrintService;
    private final BarcodeGenerator barcodeGenerator;
    private final PdfLabelGenerator pdfGenerator;

    @Getter
    private final ObservableList<BarcodeLabelDto> productList = FXCollections.observableArrayList();

    /** 리액티브 스트림 구독을 통한 데이터 로드[cite: 1, 4] */
    public void loadLabels() {
        setLoading(true);
        clearError();

        labelPrintService.getLabelDataList()
            .doOnTerminate(() -> Platform.runLater(() -> setLoading(false)))
            .subscribe(
                items -> Platform.runLater(() -> productList.setAll(items)),
                error -> Platform.runLater(() -> handleError(error))
            );
    }

    /** 바코드 생성 (안정성을 위해 null 대신 빈 배열 또는 체크 로직 지원)[cite: 1, 2] */
    public byte[] generateBarcodeImage(String barcode) {
        try {
            return barcodeGenerator.generate(barcode);
        } catch (Exception e) {
            log.error("Barcode generation failed", e);
            return new byte[0];
        }
    }

    /** 현재 리스트 PDF 출력[cite: 2] */
    public void exportToPdf() throws Exception {
        if (productList.isEmpty()) return;
        pdfGenerator.generate(new ArrayList<>(productList));
    }
}