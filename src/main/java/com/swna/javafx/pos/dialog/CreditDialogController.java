package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.dto.request.CardAuthResult;
import com.swna.javafx.pos.service.CardClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component 
@Scope("prototype")
@FxmlView("/view/pos/dialog/CreditPaymentDialog.fxml")
@RequiredArgsConstructor
public class CreditDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblCredit;
    @FXML private TextField txtCash;

    private BigDecimal totalToPay;
    private BiConsumer<BigDecimal, BigDecimal> onPaymentComplete;

    private final CardClient cardClient;

    public void initData(BigDecimal total, BigDecimal discount, 
                         BiConsumer<BigDecimal, BigDecimal> callback) {
        this.totalToPay = total;
        this.onPaymentComplete = callback;

        lblAmount.setText(CURRENCY_FORMAT.format(total));
        lblDiscount.setText(CURRENCY_FORMAT.format(discount));

        applyNumericFilter(txtCash);
        setupKeyEvents(txtCash);
        txtCash.setText("0.00");
        txtCash.selectAll();

        txtCash.textProperty().addListener((obs, old, val) -> updateCredit(val));
        updateCredit(txtCash.getText());
        txtCash.requestFocus();
    }

    private void updateCredit(String input) {
        try {
            BigDecimal cash = parseCashInput(input);
            
            if (cash.compareTo(totalToPay) > 0) {
                lblCredit.setText("Invalid");
                lblCredit.setStyle("-fx-text-fill: orange;");
                return;
            }
            
            BigDecimal credit = totalToPay.subtract(cash);
            
            if (credit.compareTo(BigDecimal.ZERO) > 0) {
                lblCredit.setText(CURRENCY_FORMAT.format(credit));
                lblCredit.setStyle("-fx-text-fill: blue;");
            } else {
                lblCredit.setText(CURRENCY_FORMAT.format(BigDecimal.ZERO));
                lblCredit.setStyle("-fx-text-fill: green;");
            }
            
        } catch (Exception e) {
            lblCredit.setText("Invalid");
            lblCredit.setStyle("-fx-text-fill: red;");
        }
    }
    
    private BigDecimal parseCashInput(String input) {
        if (input == null || input.isEmpty() || input.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(input);
    }
    
    private boolean isPaymentValid() {
        try {
            BigDecimal cash = parseCashInput(txtCash.getText());
            return cash.compareTo(totalToPay) <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void handleConfirm() {
        if (!isPaymentValid()) {
            txtCash.setStyle("-fx-border-color: red;");
            lblCredit.setText("Too much cash");
            lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
            txtCash.requestFocus();
            return;
        }
        
        try {
            BigDecimal cashAmount = parseCashInput(txtCash.getText());
            BigDecimal cardAmount = totalToPay.subtract(cashAmount);
            
            log.info("[CreditDialog] 결제 요청 - cash: ${}, card: ${}", cashAmount, cardAmount);
            
            // 카드 결제가 필요한 경우
            if (cardAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 동기 방식으로 카드 결제 요청
                CardAuthResult result = cardClient.purchase(cardAmount);
                System.out.println("  result: " + result);
                
                if (!result.isSuccess()) {
                    if (result.isCancelled()) {
                        log.info("[CreditDialog] 사용자 취소");
                        showError("Payment cancelled");
                    } else if (result.isTimeout()) {
                        log.warn("[CreditDialog] 시간 초과");
                        showError("Payment timeout - please try again");
                    } else {
                        log.error("[CreditDialog] 결제 실패: {}", result.getMessage());
                        showError("Payment failed: " + result.getMessage());
                    }
                    return;
                }
                
                log.info("[CreditDialog] 카드 결제 성공 - authCode: {}, txId: {}", 
                    result.getAuthCode(), result.getTransactionId());
            } else {
                log.info("[CreditDialog] 현금 결제만 진행 - cash: ${}", cashAmount);
            }
            
            // 콜백 실행 (영수증 출력은 여기서 처리됨)
            if (onPaymentComplete != null) {
                onPaymentComplete.accept(cashAmount, cardAmount);
            }
            
            closeDialog();
            
        } catch (Exception e) {
            log.error("[CreditDialog] 결제 처리 중 오류", e);
            showError("Payment error: " + e.getMessage());
            txtCash.requestFocus();
        }
    }
    
    private void showError(String message) {
        lblCredit.setText(message);
        lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        
        // 3초 후 원래 상태로 복원
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    updateCredit(txtCash.getText());
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    protected TextField getFocusField() { 
        return txtCash; 
    }
}