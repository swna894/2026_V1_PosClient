package com.swna.javafx.controller.barcode;

import com.swna.javafx.infrastructure.barcode.ProductLabelDto;
import com.swna.javafx.viewmodel.product.LabelViewModel;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

/**
 * 바코드 라벨 생성 및 출력을 관리하는 컨트롤러
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/barcode/LabelView.fxml")
public class LabelController {

    // FXML UI 컨트롤 주입
    @FXML private TableView<ProductLabelDto> productTable;
    @FXML private TableColumn<ProductLabelDto, String> barcodeColumn;
    @FXML private TableColumn<ProductLabelDto, String> descriptionColumn;
    @FXML private TableColumn<ProductLabelDto, BigDecimal> priceColumn;

    @FXML private ImageView barcodeImageView;
    @FXML private Label previewName;
    @FXML private Label previewBarcodeText;
    @FXML private VBox printArea;

    private final LabelViewModel viewModel;

    /**
     * FXML 로드 후 자동으로 호출되는 초기화 메서드
     */
    @FXML
    public void initialize() {
        // 1. 주입 확인 (NPE 방지)
        if (productTable == null) {
            log.error("FXML Injection failed! Check fx:id in LabelView.fxml");
            return;
        }

        // 2. [에러 해결] Record 타입 지원을 위한 CellValueFactory 설정
        // PropertyValueFactory 대신 람다를 사용하여 Record의 접근자 메서드를 직접 호출합니다.
        barcodeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().barcode()));
            
        descriptionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().description()));
            
        priceColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().price()));

        // 3. 데이터 바인딩 및 선택 이벤트 리스너 등록
        productTable.setItems(viewModel.getProductList());

        productTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newSelection) -> {
            if (newSelection != null) {
                updateBarcodePreview(newSelection);
            }
        });

        // 4. 초기 데이터 로드 호출
        viewModel.loadLabels();
    }

    /** 
     * 취소 버튼 클릭 시 선택 해제 및 미리보기 초기화
     */
    @FXML
    private void onCancel() {
        log.info("Cancel button clicked. Resetting selection.");
        productTable.getSelectionModel().clearSelection();
        barcodeImageView.setImage(null);
        previewName.setText("선택된 상품 없음");
        previewBarcodeText.setText("");
    }

    /** 
     * PDF 생성 버튼 클릭 핸들러
     */
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

    /**
     * 선택된 상품 정보를 기반으로 우측 미리보기 업데이트
     */
    private void updateBarcodePreview(ProductLabelDto product) {
        previewName.setText(product.description());
        previewBarcodeText.setText(product.barcode());
        
        byte[] imageBytes = viewModel.generateBarcodeImage(product.barcode());
        if (imageBytes != null && imageBytes.length > 0) {
            barcodeImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } else {
            barcodeImageView.setImage(null);
        }
    }

    /**
     * 프린트 버튼 클릭 핸들러: printArea 영역을 출력
     */
    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(printArea.getScene().getWindow())) {
            // printArea (VBox) 영역을 한 페이지로 출력
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