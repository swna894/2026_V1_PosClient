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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LabelViewModel extends BaseViewModel {

    private final BarcodeLabelPrintService labelPrintService;
    private final BarcodeGenerator barcodeGenerator;
    private final PdfLabelGenerator pdfGenerator;
    private final SupplierService supplierService;

    // 마스터 데이터 (전체 목록)
    private final ObservableList<BarcodeLabel> masterProductList = FXCollections.observableArrayList();
    
    // 필터링된 데이터
    private final FilteredList<BarcodeLabel> filteredProductList;
    
    // 정렬된 데이터 (UI에 바인딩될 최종 리스트)
    private final SortedList<BarcodeLabel> sortedProductList;
    
    // 검색어 프로퍼티
    @Getter
    private final StringProperty searchKeyword = new SimpleStringProperty("");
    
    // 현재 선택된 업체명 (null이면 전체)
    private String currentSupplierName = null;
    
    @Getter
    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();

    // 명시적 생성자 추가
    public LabelViewModel(BarcodeLabelPrintService labelPrintService,
                          BarcodeGenerator barcodeGenerator,
                          PdfLabelGenerator pdfGenerator,
                          SupplierService supplierService) {
        this.labelPrintService = labelPrintService;
        this.barcodeGenerator = barcodeGenerator;
        this.pdfGenerator = pdfGenerator;
        this.supplierService = supplierService;
        
        // FilteredList 초기화 (초기에는 모든 항목 표시)
        this.filteredProductList = new FilteredList<>(this.masterProductList, p -> true);
        
        // SortedList 초기화 (FilteredList를 감쌈)
        this.sortedProductList = new SortedList<>(this.filteredProductList);
        
        // 검색어 변경 리스너
        this.searchKeyword.addListener((obs, oldVal, newVal) -> applyFilter());
    }
    
    /**
     * 전체 필터 적용 (업체 + 검색어)
     */
    private void applyFilter() {
        String keyword = searchKeyword.get();
        
        filteredProductList.setPredicate(label -> {
            // 1. 업체 필터링
            if (currentSupplierName != null && !currentSupplierName.isEmpty()) {
                if (!currentSupplierName.equals(label.getCompany())) {
                    return false;
                }
            }
            
            // 2. 검색어 필터링
            if (keyword != null && !keyword.trim().isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                return (label.getBarcode() != null && label.getBarcode().toLowerCase().contains(lowerKeyword)) ||
                       (label.getCode() != null && label.getCode().toLowerCase().contains(lowerKeyword)) ||
                       (label.getDescription() != null && label.getDescription().toLowerCase().contains(lowerKeyword)) ||
                       (label.getCompany() != null && label.getCompany().toLowerCase().contains(lowerKeyword));
            }
            
            return true;
        });
        
        log.debug("Filter applied - Keyword: {}, Supplier: {}, Result size: {}", 
                  keyword, currentSupplierName, filteredProductList.size());
    }
    
    /**
     * 검색어 설정
     */
    public void setSearchKeyword(String keyword) {
        Platform.runLater(() -> searchKeyword.set(keyword == null ? "" : keyword));
    }
    
    /**
     * 업체 필터 설정 (null이면 전체)
     */
    public void setSupplierFilter(String supplierName) {
        this.currentSupplierName = supplierName;
        applyFilter();
    }
    
    /**
     * UI에 바인딩할 정렬/필터링된 상품 목록 반환
     */
    public SortedList<BarcodeLabel> getProductList() {
        return sortedProductList;
    }
    
    /**
     * TableView의 comparator와 SortedList 연결
     */
    public void bindSortedListComparator(javafx.scene.control.TableView<BarcodeLabel> tableView) {
        sortedProductList.comparatorProperty().bind(tableView.comparatorProperty());
    }
    
    /**
     * 전체 상품 개수 (필터 전)
     */
    public int getTotalCount() {
        return masterProductList.size();
    }
    
    /**
     * 필터링된 상품 개수
     */
    public int getFilteredCount() {
        return filteredProductList.size();
    }
    
    /**
     * 리액티브 스트림 구독을 통한 데이터 로드
     */
    public void loadLabels() {
        setLoading(true);
        clearError();
        setSupplierFilter(null); // 전체 조회 시 업체 필터 초기화

        labelPrintService.getLabelDataList()
            .doOnTerminate(() -> Platform.runLater(() -> setLoading(false)))
            .map(this::convertToDomainList)
            .subscribe(
                items -> Platform.runLater(() -> {
                    masterProductList.setAll(items);
                    log.info("Loaded {} labels from server", items.size());
                }),
                error -> Platform.runLater(() -> handleError(error))
            );
    }

    /**
     * 특정 업체에 해당하는 라벨 목록을 서버에서 로드
     */
    public void loadLabelsBySupplier(String supplierName) {
        setLoading(true);
        setSupplierFilter(supplierName);
        
        labelPrintService.getLabelsBySupplier(supplierName)
            .subscribe(
                dtos -> {
                    List<BarcodeLabel> domains = convertToDomainList(dtos);
                    Platform.runLater(() -> {
                        masterProductList.setAll(domains);
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
     * 거래처 목록 로드
     */
    public void loadSuppliers() {
        supplierService.getAllSuppliers()
            .subscribe(
                suppliers -> {
                    Platform.runLater(() -> {
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

    // ==================== PDF 생성 메서드 ====================

    public void exportSelectedToPdf(List<BarcodeLabel> selectedLabels) throws Exception {
        exportSelectedToPdf(selectedLabels, 3, 13);
    }

    public void exportToPdf() throws Exception {
        exportToPdf(3, 13);
    }

    public void exportSelectedToPdf(List<BarcodeLabel> selectedLabels, int cols, int rows) throws Exception {
        if (selectedLabels == null || selectedLabels.isEmpty()) {
            log.warn("No labels selected for PDF export");
            return;
        }
        
        List<BarcodeLabelDto> selectedDtos = selectedLabels.stream()
            .map(BarcodeLabel::toDto)
            .toList();
        
        pdfGenerator.generate(selectedDtos, cols, rows);
    }

    public void exportToPdf(int cols, int rows) throws Exception {
        if (sortedProductList.isEmpty()) {
            log.warn("Product list is empty, cannot export PDF");
            return;
        }
        
        List<BarcodeLabelDto> dtos = sortedProductList.stream()
            .map(BarcodeLabel::toDto)
            .toList();
        
        pdfGenerator.generate(dtos, cols, rows);
    }

    // ==================== 바코드 생성 메서드 ====================

    public byte[] generateBarcodeImage(String barcode) {
        try {
            return barcodeGenerator.generate(barcode);
        } catch (Exception e) {
            log.error("Barcode generation failed for barcode: {}", barcode, e);
            return new byte[0];
        }
    }

    public byte[] generateBarcodeImage(BarcodeLabel label) {
        if (label == null || label.getBarcode() == null) {
            log.warn("Cannot generate barcode for null label or barcode");
            return new byte[0];
        }
        return generateBarcodeImage(label.getBarcode());
    }

    // ==================== 변환 메서드 ====================

    private List<BarcodeLabel> convertToDomainList(List<BarcodeLabelDto> dtoList) {
        if (dtoList == null) {
            return new ArrayList<>();
        }
        return dtoList.stream()
            .map(BarcodeLabel::fromDto)
            .toList();
    }

    // ==================== 선택 관련 메서드 ====================

    public void selectAll(boolean selected) {
        Platform.runLater(() -> {
            for (BarcodeLabel label : sortedProductList) {
                label.setSelected(selected);
            }
        });
    }

    public List<BarcodeLabel> getSelectedLabels() {
        return sortedProductList.stream()
            .filter(BarcodeLabel::isSelected)
            .toList();
    }

    public boolean hasSelectedLabels() {
        return sortedProductList.stream().anyMatch(BarcodeLabel::isSelected);
    }

    public int getSelectedCount() {
        return (int) sortedProductList.stream().filter(BarcodeLabel::isSelected).count();
    }

    public void clearAllSelections() {
        selectAll(false);
    }
}