package com.swna.javafx.barcode.viewModel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.service.SupplierService;
import com.swna.javafx.barcode.domain.BarcodeLabel;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelViewModel extends BaseViewModel {

    private final BarcodeLabelPrintService labelPrintService;
    private final BarcodeGenerator barcodeGenerator;
    private final PdfLabelGenerator pdfGenerator;
    private final SupplierService supplierService;

    @Getter
    private final ObservableList<BarcodeLabel> productList = FXCollections.observableArrayList();

    @Getter // 콤보박스 바인딩을 위한 Supplier 리스트
    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();

    /**
     * 리액티브 스트림 구독을 통한 데이터 로드
     * DTO를 도메인 객체로 변환하여 저장합니다.
     */
    public void loadLabels() {
        setLoading(true);
        clearError();

        labelPrintService.getLabelDataList()
            .doOnTerminate(() -> Platform.runLater(() -> setLoading(false)))
            .map(this::convertToDomainList)
            .subscribe(
                items -> Platform.runLater(() -> productList.setAll(items)),
                error -> Platform.runLater(() -> handleError(error))
            );
    }

    /**
     * 특정 업체에 해당하는 라벨 목록을 서버에서 로드
     */
    public void loadLabelsBySupplier(String supplierName) {
        setLoading(true);
        
        // 기존 서비스 메서드를 활용하여 서버 요청
        labelPrintService.getLabelsBySupplier(supplierName)
            .subscribe(
                dtos -> {
                    List<BarcodeLabel> domains = convertToDomainList(dtos);
                    Platform.runLater(() -> {
                        productList.setAll(domains); // 서버에서 가져온 데이터로 교체
                        setLoading(false);
                        log.info("Loaded {} labels from server for supplier: {}", domains.size(), supplierName);
                    });
                },
                error -> {
                    log.error("Failed to load labels for supplier: {}", supplierName, error);
                    Platform.runLater(() -> setLoading(false));
                }
            );
    }
    
    /**
     * 거래처 목록 로드 (Supplier 객체 리스트)
     */
    public void loadSuppliers() {
        supplierService.getAllSuppliers()
            .subscribe(
                suppliers -> {
                    Platform.runLater(() -> {
                        // "전체" 선택을 위한 더미 객체 생성
                        Supplier allOption = new Supplier();
                        allOption.setCompany("전체");
                        
                        supplierList.clear();
                        supplierList.add(allOption);
                        supplierList.addAll(suppliers);
                        log.info("Loaded {} suppliers for ComboBox", suppliers.size());
                    });
                },
                error -> log.error("Failed to load suppliers in ViewModel", error)
            );
    }

    public ObservableList<Supplier> getSupplierList() {
        return this.supplierList;
    }
    /**
     * 선택된 라벨만 PDF로 출력합니다.
     *
     * @param selectedLabels 선택된 라벨 목록
     */
    public void exportSelectedToPdf(List<BarcodeLabel> selectedLabels) throws Exception {
        if (selectedLabels == null || selectedLabels.isEmpty()) {
            log.warn("No labels selected for PDF export");
            return;
        }
        
        List<BarcodeLabelDto> selectedDtos = selectedLabels.stream()
            .map(BarcodeLabel::toDto)
            .toList();
        
        pdfGenerator.generate(selectedDtos);
    }

    /**
     * 현재 전체 리스트 PDF 출력
     */
    public void exportToPdf() throws Exception {
        if (productList.isEmpty()) return;
        
        List<BarcodeLabelDto> dtos = productList.stream()
            .map(BarcodeLabel::toDto)
            .toList();
        
        pdfGenerator.generate(dtos);
    }

    /**
     * 바코드 생성 (안정성을 위해 null 대신 빈 배열 또는 체크 로직 지원)
     *
     * @param barcode 생성할 바코드 문자열
     * @return 바코드 이미지 바이트 배열
     */
    public byte[] generateBarcodeImage(String barcode) {
        try {
            return barcodeGenerator.generate(barcode);
        } catch (Exception e) {
            log.error("Barcode generation failed for barcode: {}", barcode, e);
            return new byte[0];
        }
    }

    /**
     * 특정 라벨의 바코드 이미지를 생성합니다.
     *
     * @param label 바코드를 생성할 라벨 객체
     * @return 바코드 이미지 바이트 배열
     */
    public byte[] generateBarcodeImage(BarcodeLabel label) {
        if (label == null || label.getBarcode() == null) {
            log.warn("Cannot generate barcode for null label or barcode");
            return new byte[0];
        }
        return generateBarcodeImage(label.getBarcode());
    }

    /**
     * DTO 리스트를 도메인 객체 리스트로 변환합니다.
     *
     * @param dtoList 변환할 DTO 리스트
     * @return 변환된 도메인 객체 리스트
     */
    private List<BarcodeLabel> convertToDomainList(List<BarcodeLabelDto> dtoList) {
        if (dtoList == null) {
            return new ArrayList<>();
        }
        return dtoList.stream()
            .map(BarcodeLabel::fromDto)
            .toList();
    }

    /**
     * 단일 DTO를 도메인 객체로 변환합니다.
     *
     * @param dto 변환할 DTO
     * @return 변환된 도메인 객체
     */
    @SuppressWarnings("unused")
    private BarcodeLabel convertToDomain(BarcodeLabelDto dto) {
        return BarcodeLabel.fromDto(dto);
    }

    /**
     * 선택된 모든 항목의 선택 상태를 일괄 변경합니다.
     *
     * @param selected 선택 여부
     */
    public void selectAll(boolean selected) {
        Platform.runLater(() -> {
            for (BarcodeLabel label : productList) {
                label.setSelected(selected);
            }
        });
    }

    /**
     * 선택된 항목 목록을 반환합니다.
     *
     * @return 선택된 라벨 목록
     */
    public List<BarcodeLabel> getSelectedLabels() {
        return productList.stream()
            .filter(BarcodeLabel::isSelected)
            .toList();
    }

    /**
     * 선택된 항목이 있는지 확인합니다.
     *
     * @return 선택된 항목 존재 여부
     */
    public boolean hasSelectedLabels() {
        return productList.stream().anyMatch(BarcodeLabel::isSelected);
    }

    /**
     * 선택된 항목의 개수를 반환합니다.
     *
     * @return 선택된 항목 개수
     */
    public int getSelectedCount() {
        return (int) productList.stream().filter(BarcodeLabel::isSelected).count();
    }

    /**
     * 모든 항목의 선택 상태를 해제합니다.
     */
    public void clearAllSelections() {
        selectAll(false);
    }
}