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
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * 바코드 라벨 생성 및 출력을 관리하는 컨트롤러
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/barcode/LabelView.fxml")
public class LabelController {

    private static final int NUMBER_COLUMN_WIDTH = 70;
    private static final int CHECKBOX_COLUMN_WIDTH = 50;

    @FXML private TableColumn<BarcodeLabel, String> colNo;
    @FXML private TableColumn<BarcodeLabel, Boolean> colCheckbox;
    @FXML private TableColumn<BarcodeLabel, String> barcodeColumn;
    @FXML private TableColumn<BarcodeLabel, String> codeColumn;
    @FXML private TableColumn<BarcodeLabel, String> supplierColumn;
    @FXML private TableColumn<BarcodeLabel, String> descriptionColumn;
    @FXML private TableColumn<BarcodeLabel, BigDecimal> priceColumn;
    @FXML private TableView<BarcodeLabel> table;
    
    @FXML private ImageView barcodeImageView;
    @FXML private ComboBox<Supplier> categoryCombo;
    @FXML private Label previewName;
    @FXML private Label previewBarcodeText;
    @FXML private VBox printArea;
    @FXML private Label lblStatus;
    
    @FXML private Button backButton;
    @FXML private Button btnCancel;
    @FXML private Button btnCancel1;
    @FXML private Button btnGenerate;
    
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

        table.setItems(viewModel.getProductList());
        viewModel.loadLabels();
        viewModel.loadSuppliers();
    }

    private void setupTableColumns() {

        table.setEditable(true);  // TableView 편집 가능

        // 번호 컬럼
        TableColumnUtil.createNumberColumn(table, colNo, NUMBER_COLUMN_WIDTH);           
        TableColumnUtil.createCheckBoxHeaderColumn( table, colCheckbox, BarcodeLabel::selectedProperty, "", CHECKBOX_COLUMN_WIDTH );
     
        TableColumnUtil.makeStringColumn(  barcodeColumn, BarcodeLabel::barcodeProperty, BarcodeLabel::setBarcode, false, TableColumnUtil.CENTER, null  );
        TableColumnUtil.makeStringColumn(  codeColumn, BarcodeLabel::codeProperty, BarcodeLabel::setCode, false, TableColumnUtil.CENTER, null  );
        TableColumnUtil.makeStringColumn(  supplierColumn, BarcodeLabel::companyProperty, BarcodeLabel::setCompany, false, TableColumnUtil.CENTER, null  );
        TableColumnUtil.makeStringColumn( descriptionColumn, BarcodeLabel::descriptionProperty, BarcodeLabel::setDescription, false, TableColumnUtil.LEFT, null );
        TableColumnUtil.makeBigDecimalCurrencyColumn( priceColumn,  BarcodeLabel::priceProperty,  false, TableColumnUtil.RIGHT, null );
    }
    
    private void setupEventHandlers() {
        // 뒤로가기 버튼 이벤트
        if (backButton != null) {
            backButton.setOnAction(e -> navigationService.navigateStage(PosViewController.class));
        }

        // 테이블 선택 이벤트
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateBarcodePreview(newVal);
            }
        });
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

        // 콤보박스 선택 변경 시 이벤트
        categoryCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // 로컬 필터링이 아닌 서버 조회를 호출
                fetchDataFromServer(newVal.getCompany());
            }
        });
    }

    /**
     * 서버에서 데이터를 새로 가져와서 테이블을 갱신
     */
    private void fetchDataFromServer(String companyName) {
        // ViewModel을 통해 서버 데이터 요청
        viewModel.loadLabelsBySupplier(companyName);
    }

    // 메서드 정의 수정: 매개변수 타입을 String에서 Supplier로 변경
    private void filterTableBySupplier(Supplier supplier) {
        // 객체에서 회사명을 꺼내옴
        String companyName = supplier.getCompany();

        if (companyName == null || companyName.equals("전체")) {
            table.setItems(viewModel.getProductList());
        } else {
            // 해당 거래처명과 일치하는 항목만 필터링하여 표시
            ObservableList<BarcodeLabel> filteredList = viewModel.getProductList().filtered(label -> 
                companyName.equals(label.getCompany()) // BarcodeLabel의 필드명에 맞춰 확인 (getSupplier 또는 getCompany)
            );
            table.setItems(filteredList);
        }
    }

    @FXML
    private void onCancel() {
        log.info("Cancel button clicked. Resetting selection.");
        table.getSelectionModel().clearSelection();
        barcodeImageView.setImage(null);
        previewName.setText("선택된 상품 없음");
        previewBarcodeText.setText("");
    }

    @FXML
    private void onGenerate() {
        try {
            viewModel.exportToPdf();
            //showInfo("Success", "PDF has been generated successfully.");

            // 파일 경로를 직접 생성
            Path generatedFilePath = Paths.get(System.getProperty("user.home"), 
                                              "Downloads", 
                                              "barcode_labels.pdf");

            
            if (generatedFilePath != null) {
                openPdfFile(generatedFilePath);
            }
        } catch (Exception e) {
            log.error("PDF Export Error", e);
            showError("Error", "PDF generation failed: " + e.getMessage());
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
                    showInfo("PDF Location", "PDF saved at: " + pdfPath.toAbsolutePath())
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // 복원
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
            viewModel.exportSelectedToPdf(viewModel.getSelectedLabels());
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