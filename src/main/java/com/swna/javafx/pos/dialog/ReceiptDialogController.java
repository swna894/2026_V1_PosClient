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
@FxmlView("/view/pos/dialog/ReceiptDialog.fxml")
public class ReceiptDialogController extends BasePaymentDialog {

    @FXML private TextField txtReceiptNumber;

    private Consumer<String> onSearchCallback;

    @FXML
    public void initialize() {
        // 단축키 설정 (BasePaymentDialog 메서드)
        setupKeyEvents(txtReceiptNumber);
        
        // 포커스 요청
        Platform.runLater(() -> {
            txtReceiptNumber.requestFocus();
            txtReceiptNumber.selectAll();
        });
    }

    @FXML
    private void handleReceiptRowClick(MouseEvent event) {
        txtReceiptNumber.requestFocus();
        txtReceiptNumber.selectAll();
    }

    @FXML
    private void handleSearch() {
        String receiptNumber = txtReceiptNumber.getText();
        if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
            log.warn("[ReceiptDialog] Empty receipt number");
            return;
        }
        
        log.info("[ReceiptDialog] Searching for receipt: {}", receiptNumber);
        if (onSearchCallback != null) {
            onSearchCallback.accept(receiptNumber.trim());
        }
        closeDialog();
    }

    public void initData(Consumer<String> onSearchCallback) {
      this.onSearchCallback = onSearchCallback;
      this.txtReceiptNumber.setText("");

      // 🔥 전체 창 드래그 활성화 (가장 간단)
      enableFullWindowDrag();
    }
    
    // ========== BasePaymentDialog 구현 ==========
    
    @Override
    protected void handleConfirm() {
        handleSearch();
    }

    @Override
    protected TextField getFocusField() {
        return txtReceiptNumber;
    }

    // handleCancel()은 BasePaymentDialog에서 상속받아 사용
    // closeDialog()는 BasePaymentDialog에서 상속받아 사용
}