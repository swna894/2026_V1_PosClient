package com.swna.javafx.pos.dialog;

import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/BarcodeDialog.fxml")
public class BarcodeDialogController extends BasePosDialog {

    @FXML private TextField txtBarcodeNumber;

    private Consumer<String> onSearchCallback;

    @FXML
    public void initialize() {
        // 단축키 설정 (BasePaymentDialog 메서드)
        setupKeyEvents(txtBarcodeNumber);
        
        // 포커스 요청
        Platform.runLater(() -> {
            txtBarcodeNumber.requestFocus();
            txtBarcodeNumber.selectAll();
        });
    }

    @FXML
    private void handleBarcodeRowClick(MouseEvent event) {
        txtBarcodeNumber.requestFocus();
        txtBarcodeNumber.selectAll();
    }

    @FXML
    private void handleSearch() {
        String barcodeNumber = txtBarcodeNumber.getText();
        if (barcodeNumber == null || barcodeNumber.trim().isEmpty()) {
            log.warn("[BarcodeDialog] Empty barcode number");
            return;
        }
        
        log.info("[BarcodeDialog] Searching for barcode: {}", barcodeNumber);
        if (onSearchCallback != null) {
            onSearchCallback.accept(barcodeNumber.trim());
        }
        closeDialog();
    }

    public void initData(Consumer<String> onSearchCallback) {
      this.onSearchCallback = onSearchCallback;
      this.txtBarcodeNumber.setText("");

      // 🔥 방법 1: 전체 창 드래그 활성화 (가장 간단)
      enableFullWindowDrag();
        
    }
    

    // ========== BasePaymentDialog 구현 ==========
    
    @Override
    protected void handleConfirm() {
        handleSearch();
    }

    @Override
    protected TextField getFocusField() {
        return txtBarcodeNumber;
    }

    // handleCancel()은 BasePaymentDialog에서 상속받아 사용
    // closeDialog()는 BasePaymentDialog에서 상속받아 사용
}
