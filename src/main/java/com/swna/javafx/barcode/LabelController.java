package com.swna.javafx.barcode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.barcode.domain.BarcodeLabel;
import com.swna.javafx.barcode.viewModel.LabelViewModel;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.pos.PosViewController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/barcode/LabelView.fxml")
public class LabelController {

    private static final int NUMBER_COLUMN_WIDTH = 70;
    private static final int CHECKBOX_COLUMN_WIDTH = 50;

    // TableView 관련
    @FXML private TableColumn<BarcodeLabel, String> colNo;
    @FXML private TableColumn<BarcodeLabel, Boolean> colCheckbox;
    @FXML private TableColumn<BarcodeLabel, String> barcodeColumn;
    @FXML private TableColumn<BarcodeLabel, String> codeColumn;
    @FXML private TableColumn<BarcodeLabel, String> supplierColumn;
    @FXML private TableColumn<BarcodeLabel, String> descriptionColumn;
    @FXML private TableColumn<BarcodeLabel, BigDecimal> priceColumn;
    @FXML private TableView<BarcodeLabel> table;
    
    // 검색 및 필터 관련
    @FXML private TextField searchField;
    @FXML private ComboBox<Supplier> categoryCombo;
    
    // 레이아웃 관련
    @FXML private ComboBox<Integer> colsCombo;
    @FXML private ComboBox<Integer> rowsCombo;
    
    // 미리보기 관련
    @FXML private ImageView barcodeImageView;
    @FXML private Label previewName;
    @FXML private Label previewBarcodeText;
    @FXML private VBox printArea;
    @FXML private Label lblStatus;
    
    // 버튼
    @FXML private Button backButton;
    @FXML private Button btnGenerate;
    @FXML private Button btnPrint;
    
    private final LabelViewModel viewModel;
    private final NavigationService navigationService;

    @FXML
    public void initialize() {
        if (table == null) {
            log.error("FXML Injection failed! Check fx:id in LabelView.fxml");
            return;
        }
            
        setupTableColumns();   
        setupEventHandlers();
        setupSupplierComboBox();
        setupSearchFilter();
        setupLayoutComboBoxes();

        // ViewModel의 SortedList를 테이블에 바인딩
        table.setItems(viewModel.getProductList());
        
        // *** 중요: TableView의 정렬과 SortedList 연결 ***
        viewModel.bindSortedListComparator(table);
        
        // 데이터 로드
        viewModel.loadLabels();
        viewModel.loadSuppliers();
    }

    private void setupTableColumns() {
        table.setEditable(true);

        TableColumnUtil.createNumberColumn(table, colNo, NUMBER_COLUMN_WIDTH);           
        TableColumnUtil.createCheckBoxHeaderColumn(table, colCheckbox, BarcodeLabel::selectedProperty, "", CHECKBOX_COLUMN_WIDTH);
     
        TableColumnUtil.makeStringColumn(barcodeColumn, BarcodeLabel::barcodeProperty, BarcodeLabel::setBarcode, false, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeStringColumn(codeColumn, BarcodeLabel::codeProperty, BarcodeLabel::setCode, false, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeStringColumn(supplierColumn, BarcodeLabel::companyProperty, BarcodeLabel::setCompany, false, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeStringColumn(descriptionColumn, BarcodeLabel::descriptionProperty, BarcodeLabel::setDescription, false, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(priceColumn, BarcodeLabel::priceProperty, false, TableColumnUtil.RIGHT, null);
    }
    
    private void setupEventHandlers() {
        if (backButton != null) {
            backButton.setOnAction(e -> navigationService.navigateStage(PosViewController.class));
        }

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateBarcodePreview(newVal);
            }
        });
    }
    
    /**
     * 실시간 검색 필터 설정
     */
    private void setupSearchFilter() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.setSearchKeyword(newVal);
                updateStatusMessage();
            });
        }
    }

    private void setupSupplierComboBox() {
        categoryCombo.setItems(viewModel.getSupplierList());

        categoryCombo.setConverter(new StringConverter<Supplier>() {
            @Override
            public String toString(Supplier supplier) {
                return (supplier == null) ? "" : supplier.getCompany();
            }
            @Override
            public Supplier fromString(String string) { return null; }
        });

        categoryCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String companyName = "전체".equals(newVal.getCompany()) ? null : newVal.getCompany();
                
                if (companyName == null) {
                    viewModel.loadLabels();
                } else {
                    viewModel.loadLabelsBySupplier(companyName);
                }
                
                if (searchField != null) {
                    searchField.clear();
                }
            }
        });
    }

    private void setupLayoutComboBoxes() {
        colsCombo.setValue(3);
        rowsCombo.setValue(13);
        
        colsCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateStatusMessage());
        rowsCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateStatusMessage());
        
        updateStatusMessage();
    }
    
    /**
     * 상태 메시지 업데이트 (필터 정보 포함)
     */
    private void updateStatusMessage() {
        int cols = colsCombo.getValue() != null ? colsCombo.getValue() : 3;
        int rows = rowsCombo.getValue() != null ? rowsCombo.getValue() : 13;
        int labelsPerPage = cols * rows;
        
        int totalCount = viewModel.getTotalCount();
        int filteredCount = viewModel.getFilteredCount();
        String searchKeyword = viewModel.getSearchKeyword().get();
        
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            lblStatus.setText(String.format("🔍 '%s' 검색 결과: %d/%d건 | 레이아웃: %dx%d = %d장/page", 
                            searchKeyword, filteredCount, totalCount, cols, rows, labelsPerPage));
        } else {
            lblStatus.setText(String.format("📋 전체 %d건 | 레이아웃: %dx%d = %d장/page", 
                            totalCount, cols, rows, labelsPerPage));
        }
    }
    
    private int getSelectedCols() {
        return colsCombo.getValue() != null ? colsCombo.getValue() : 3;
    }
    
    private int getSelectedRows() {
        return rowsCombo.getValue() != null ? rowsCombo.getValue() : 13;
    }

    @FXML
    private void onCancel() {
        log.info("Cancel button clicked. Resetting selection.");
        table.getSelectionModel().clearSelection();
        barcodeImageView.setImage(null);
        previewName.setText("선택된 상품 없음");
        previewBarcodeText.setText("");
        
        if (searchField != null) {
            searchField.clear();
        }
    }

    @FXML
    private void onGenerate() {
        try {
            int cols = getSelectedCols();
            int rows = getSelectedRows();
            
            viewModel.exportToPdf(cols, rows);

            Path generatedFilePath = Paths.get(System.getProperty("user.home"), 
                                              "Downloads", 
                                              "barcode_labels.pdf");
            
            if (generatedFilePath != null) {
                openPdfFile(generatedFilePath);
            }
        } catch (Exception e) {
            log.error("PDF Export Error", e);
            showError("오류", "PDF 생성 실패: " + e.getMessage());
        }
    }

    private void openPdfFile(Path pdfPath) {
        if (pdfPath == null || !Files.exists(pdfPath)) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "start", pdfPath.toString()
                );
                Process process = pb.start();
                
                boolean completed = process.waitFor(2, TimeUnit.SECONDS);
                if (completed && process.exitValue() != 0) {
                    log.warn("Process exited with code: {}", process.exitValue());
                }
            } catch (IOException e) {
                log.error("Failed to open PDF: {}", e.getMessage());
                Platform.runLater(() -> 
                    showInfo("PDF 위치", "PDF 저장 위치: " + pdfPath.toAbsolutePath())
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while opening PDF");
            }
        });
    }
    
    @FXML
    private void onGenerateSelected() {
        try {
            if (!viewModel.hasSelectedLabels()) {
                showInfo("알림", "선택된 상품이 없습니다.");
                return;
            }
            
            int cols = getSelectedCols();
            int rows = getSelectedRows();
            
            viewModel.exportSelectedToPdf(viewModel.getSelectedLabels(), cols, rows);
            showInfo("성공", "선택된 상품의 PDF가 생성되었습니다.");
        } catch (Exception e) {
            log.error("PDF Export Error for selected items", e);
            showError("오류", "PDF 생성 실패: " + e.getMessage());
        }
    }

    private void updateBarcodePreview(BarcodeLabel product) {
        previewName.setText(product.getDescription());
        previewBarcodeText.setText(product.getBarcode());
        
        byte[] imageBytes = viewModel.generateBarcodeImage(product);
        if (imageBytes != null && imageBytes.length > 0) {
            barcodeImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } else {
            barcodeImageView.setImage(null);
        }
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(printArea.getScene().getWindow())) {
            boolean success = job.printPage(printArea);
            if (success) {
                job.endJob();
            }
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}