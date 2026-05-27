package com.swna.javafx.pos.dialog;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component 
@Scope("prototype")
@FxmlView("/view/pos/dialog/ConfirmDialog.fxml")
public class ConfirmDialogController extends BasePaymentDialog {
    
    @FXML
    private StackPane paneReceiptRow;
    
    @FXML
    private TextField txtReceiptNumber;
    
    private Runnable onConfirm;
    private Runnable onCancel;
    
    @FXML
    public void initialize() {
        // 전체 창 드래그 기능 활성화
        enableFullWindowDrag();
        
        // ESC/ENTER 키 전역 처리
        setupGlobalKeyEvents();
    }
    
    /**
     * 다이얼로그 초기화 (콜백 설정)
     */
    public void initData(Runnable onConfirm, Runnable onCancel) {
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }
    
    /**
     * 다이얼로그 전체에서 ESC/ENTER 키 처리
     */
    private void setupGlobalKeyEvents() {
        TextField focusField = getFocusField();
        if (focusField != null && focusField.getScene() != null) {
            focusField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    handleCancel();
                    event.consume();
                } else if (event.getCode() == KeyCode.ENTER) {
                    handleConfirm();
                    event.consume();
                }
            });
        }
    }
    
    @FXML
    @Override
    protected void handleConfirm() {
        log.info("Confirm button clicked - Deleting all items");
        if (onConfirm != null) {
            onConfirm.run();
        }
        closeDialog();
    }
    
    @FXML
    @Override
    protected void handleCancel() {
        log.info("Cancel button clicked - Delete operation cancelled");
        if (onCancel != null) {
            onCancel.run();
        }
        closeDialog();
    }
    
    @Override
    protected TextField getFocusField() {
        return txtReceiptNumber;
    }
}