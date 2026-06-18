package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/VolumnDiscountDialog.fxml")
public class VolumeDiscountDialogController extends BasePosDialog {

    @FXML private Label lblTotalAmount;
    @FXML private TextField txtPrice;
    @FXML private TextField txtPercent;
    @FXML private RadioButton rbPrice;
    @FXML private RadioButton rbPercent;

    private BigDecimal totalAmount;
    private VolumeDiscountHandler handler;

    @FXML
    public void initialize() {
        // 1. 단축키 설정 (BasePaymentDialog 메서드)
        setupKeyEvents(txtPrice);
        setupKeyEvents(txtPercent);
        
        // 2. 숫자 및 마침표(.) 입력 제한 설정 (BasePaymentDialog 메서드)
        applyNumericFilter(txtPrice);
        applyNumericFilter(txtPercent);
    }

    @FXML
    private void handlePriceRowClick(MouseEvent event) {
        rbPrice.setSelected(true);
        txtPercent.setText("0");
        txtPrice.requestFocus();
        txtPrice.selectAll();
    }

    @FXML
    private void handlePercentRowClick(MouseEvent event) {
        rbPercent.setSelected(true);
        txtPrice.setText("0");
        txtPercent.requestFocus();
        txtPercent.selectAll();
    }

    public void initData(BigDecimal totalAmount, VolumeDiscountHandler handler) {
        this.totalAmount = totalAmount;
        this.handler = handler;
        this.lblTotalAmount.setText(CURRENCY_FORMAT.format(totalAmount));
        this.txtPrice.setText(String.format("%.2f", totalAmount));
        this.txtPercent.setText("0");

        txtPrice.disableProperty().bind(rbPercent.selectedProperty());
        txtPercent.disableProperty().bind(rbPrice.selectedProperty());

        // 시작 시 txtPrice 전체 선택
        Platform.runLater(() -> {
            txtPrice.requestFocus();
            txtPrice.selectAll();
        });

        
        // 🔥 방법 1: 전체 창 드래그 활성화 (가장 간단)
        enableFullWindowDrag();

        // 창 드래그 및 전역 단축키 설정
        lblTotalAmount.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F12 || event.getCode() == KeyCode.ENTER) {
                        handleConfirm();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        handleCancel();
                        event.consume();
                    }
                });
            }
        });
    }

    private void showValidationError(String message) {
        lblTotalAmount.setText(message);
        lblTotalAmount.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    lblTotalAmount.setText(CURRENCY_FORMAT.format(totalAmount));
                    lblTotalAmount.setStyle("");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // ========== BasePaymentDialog 구현 ==========
    
    @Override protected void handleConfirm() {
        if (handler == null) {
            log.warn("[VolumeDiscountDialog] Handler is null");
            closeDialog();
            return;
        }

        try {
            if (rbPrice.isSelected()) {
                // 1. [금액 모드] 입력값은 '할인 후 최종 결제 금액'입니다.
                BigDecimal finalAmount = new BigDecimal(txtPrice.getText().isEmpty() ? "0" : txtPrice.getText())
                    .setScale(2, RoundingMode.HALF_UP);

                // 유효성 검사: 최종 금액은 0보다 커야 하며, 기존 총액보다 작아야 함(할인이 발생해야 하므로)
                if (finalAmount.compareTo(BigDecimal.ZERO) <= 0 || finalAmount.compareTo(totalAmount) >= 0) {
                    log.warn("[VolumeDiscountDialog] Invalid final amount: {}", finalAmount);
                    showValidationError("Invalid final amount. Must be less than original total.");
                    return;
                }

                log.info("[VolumeDiscountDialog] Setting final amount to: {}", finalAmount);
                // '최종 금액'을 전달하여 로직 처리
                handler.onVolumeDiscount(finalAmount, null); 

            } else {
                // 2. [퍼센트 모드] 입력값은 '할인율(%)'입니다.
                BigDecimal percent = new BigDecimal(txtPercent.getText().isEmpty() ? "0" : txtPercent.getText())
                    .setScale(2, RoundingMode.HALF_UP);

                if (percent.compareTo(BigDecimal.ZERO) <= 0 || percent.compareTo(BigDecimal.valueOf(100)) >= 0) {
                    log.warn("[VolumeDiscountDialog] Invalid percent: {}", percent);
                    showValidationError("Invalid percent value (0-100)");
                    return;
                }

                log.info("[VolumeDiscountDialog] Applying {}% discount", percent);
                // 퍼센트 값을 전달 (첫 번째 인자를 null로 하여 구분)
                handler.onVolumeDiscount(null, percent); 
            }
            
            closeDialog();
            
        } catch (NumberFormatException e) {
            log.error("Error converting input: {}", e.getMessage());
            showValidationError("Invalid number format");
        }
    }

    @Override
    protected TextField getFocusField() {
        // 현재 활성화된 TextField 반환
        if (rbPrice.isSelected() && !txtPrice.isDisabled()) {
            return txtPrice;
        } else if (rbPercent.isSelected() && !txtPercent.isDisabled()) {
            return txtPercent;
        }
        return txtPrice;
    }

    // handleCancel()은 BasePaymentDialog에서 상속받아 사용
    // closeDialog()는 BasePaymentDialog에서 상속받아 사용

    @FunctionalInterface
    public interface VolumeDiscountHandler {
        void onVolumeDiscount(BigDecimal amountAfterDC, BigDecimal percent);
    }
}