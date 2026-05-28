package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.Consumer;

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
@FxmlView("/view/pos/dialog/CancelDialog.fxml")
public class CancelDialogController extends BasePosDialog {
    
    @FXML private StackPane paneReceiptRow;
    @FXML private TextField txtReceiptNumber;

    private BigDecimal totalAmount;
    private Consumer<BigDecimal> onPaymentComplete;

    @FXML
    public void initialize() {
        // 전체 창 드래그 기능 활성화
        enableFullWindowDrag();
        
        // ESC/ENTER 키 전역 처리
        setupGlobalKeyEvents();
    }
    
    public void initData(BigDecimal total, Consumer<BigDecimal> callback) {
        this.totalAmount = total;
        this.onPaymentComplete = callback;
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

        if (onPaymentComplete != null) onPaymentComplete.accept(totalAmount);

        closeDialog();
    }
    
    @FXML
    @Override
    protected void handleCancel() {
        log.info("Cancel button clicked - Delete operation cancelled");
        closeDialog();
    }
    
    @Override
    protected TextField getFocusField() {
        return txtReceiptNumber;
    }
}