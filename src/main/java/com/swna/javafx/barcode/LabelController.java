package com.swna.javafx.barcode;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.barcode.domain.BarcodeLabel;
import com.swna.javafx.barcode.viewModel.LabelViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
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

    @FXML private TableView<BarcodeLabel> table;

    @FXML private TableColumn<BarcodeLabel, Boolean> colCheckbox;
    @FXML private TableColumn<BarcodeLabel, String> colNo;
    @FXML private TableColumn<BarcodeLabel, String> barcodeColumn;
    @FXML private TableColumn<BarcodeLabel, String> descriptionColumn;
    @FXML private TableColumn<BarcodeLabel, BigDecimal> priceColumn;

    @FXML private ImageView barcodeImageView;
    @FXML private Label previewName;
    @FXML private Label previewBarcodeText;
    @FXML private VBox printArea;
 
    @FXML private Button btnCancel;
    @FXML private Button btnCancel1;
    @FXML private Button btnGenerate;

    private final LabelViewModel viewModel;

    @FXML
    public void initialize() {
        if (table == null) {
            log.error("FXML Injection failed! Check fx:id in LabelView.fxml");
            return;
        }
        
            // ✅ 중요: TableView와 컬럼 모두 편집 가능해야 함
        table.setEditable(true);  // TableView 편집 가능
        setupTableColumns();   
        table.setItems(viewModel.getProductList());

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, newSelection) -> {
            if (newSelection != null) {
                updateBarcodePreview(newSelection);
            }
        });

        viewModel.loadLabels();
    }

    private void setupTableColumns() {
        // 번호 컬럼
        TableColumnUtil.createNumberColumn(table, colNo, NUMBER_COLUMN_WIDTH);           
        TableColumnUtil.createCheckBoxHeaderColumn( table, colCheckbox, BarcodeLabel::selectedProperty, "", CHECKBOX_COLUMN_WIDTH );
     
        TableColumnUtil.makeStringColumn(  barcodeColumn, BarcodeLabel::barcodeProperty, BarcodeLabel::setBarcode, false, TableColumnUtil.CENTER, null  );
        TableColumnUtil.makeStringColumn( descriptionColumn, BarcodeLabel::descriptionProperty, BarcodeLabel::setDescription, false, TableColumnUtil.LEFT, null );
        TableColumnUtil.makeBigDecimalCurrencyColumn( priceColumn,  BarcodeLabel::priceProperty,  false, TableColumnUtil.RIGHT, null );
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
            showInfo("성공", "PDF가 생성되었습니다.");
        } catch (Exception e) {
            log.error("PDF Export Error", e);
            showError("오류", "PDF 생성 실패: " + e.getMessage());
        }
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