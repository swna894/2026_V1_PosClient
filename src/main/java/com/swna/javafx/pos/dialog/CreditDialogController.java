package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.dto.request.CardAuthResult;
import com.swna.javafx.pos.functional.TriConsumer;
import com.swna.javafx.pos.service.CardPaymentService;

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
    private CreditCallback callback;
    private final CardPaymentService cardClient;

    // ========== Callback Interface ==========
    
    @FunctionalInterface
    public interface CreditCallback {
        void onPaymentComplete(BigDecimal cashAmount, BigDecimal cardAmount, String cardNumber);
    }
    
    // ========== Initialization ==========

    /**
     * 레거시 콜백 지원 (카드번호 없음) - TriConsumer 사용
     * TriConsumer<BigDecimal, BigDecimal, String> 으로 통일
     */
    public void initData(BigDecimal total, BigDecimal discount, 
                         TriConsumer<BigDecimal, BigDecimal, String> legacyCallback) {
        this.totalToPay = total;
        this.callback = (cash, card, cardNumber) -> {
            log.debug("[CreditDialog] Legacy callback - passing cardNumber: {}", cardNumber);
            // 3개 모두 전달 (기존 코드에서 cardNumber 무시 가능)
            legacyCallback.accept(cash, card, cardNumber);
        };

        setupUI(total, discount);
        setupInputHandlers();
    }
    
    /**
     * 새로운 콜백 지원 (카드번호 포함)
     */
    public void initData(BigDecimal total, BigDecimal discount, CreditCallback callback) {
        this.totalToPay = total;
        this.callback = callback;
        setupUI(total, discount);
        setupInputHandlers();
    }
    
    private void setupUI(BigDecimal total, BigDecimal discount) {
        lblAmount.setText(CURRENCY_FORMAT.format(total));
        lblDiscount.setText(CURRENCY_FORMAT.format(discount));
        
        txtCash.setText("0.00");
        txtCash.selectAll();
        
        applyNumericFilter(txtCash);
        setupKeyEvents(txtCash);
        
        txtCash.requestFocus();
    }
    
    private void setupInputHandlers() {
        txtCash.textProperty().addListener((obs, old, val) -> updateCredit(val));
        updateCredit(txtCash.getText());
    }

    // ========== UI Update Methods ==========
    
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
    
    // ========== Validation Methods ==========
    
    private boolean isPaymentValid() {
        try {
            BigDecimal cash = parseCashInput(txtCash.getText());
            return cash.compareTo(totalToPay) <= 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showValidationError() {
        txtCash.setStyle("-fx-border-color: red;");
        lblCredit.setText("Too much cash");
        lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
        txtCash.requestFocus();
    }

    // ========== Payment Processing ==========
    
    @Override
    protected void handleConfirm() {
        if (!isPaymentValid()) {
            showValidationError();
            return;
        }
        
        processPayment();
    }
    
    private void processPayment() {
        try {
            BigDecimal cashAmount = parseCashInput(txtCash.getText());
            BigDecimal cardAmount = totalToPay.subtract(cashAmount);
            
            log.info("[CreditDialog] Payment request - cash: ${}, card: ${}", cashAmount, cardAmount);
            
            String cardNumber = null;
            
            // 카드 결제가 필요한 경우
            if (cardAmount.compareTo(BigDecimal.ZERO) > 0) {
                CardAuthResult cardResult = cardClient.purchase(cardAmount);
                
                if (!cardResult.isSuccess()) {
                    handlePaymentFailure(cardResult);
                    return;
                }
                
                cardNumber = cardResult.getCardNumber();
         
                log.info("[CreditDialog] Card payment success - authCode: {}, txId: {}, cardNumber: {}", 
                    cardResult.getAuthCode(), cardResult.getTransactionId(), cardNumber);
            } else {
                log.info("[CreditDialog] Cash only payment - cash: ${}", cashAmount);
            }
            
            // ✅ 카드번호를 포함한 콜백 실행
            if (callback != null) {
                callback.onPaymentComplete(cashAmount, cardAmount, cardNumber);
            }
            
            closeDialog();
            
        } catch (Exception e) {
            log.error("[CreditDialog] Payment error", e);
            showError("Payment error: " + e.getMessage());
            txtCash.requestFocus();
        }
    }
    
    private void handlePaymentFailure(CardAuthResult result) {
        if (result.isCancelled()) {
            log.info("[CreditDialog] Payment cancelled by user");
            showError("Payment cancelled");
        } else if (result.isTimeout()) {
            log.warn("[CreditDialog] Payment timeout");
            showError("Payment timeout - please try again");
        } else {
            log.error("[CreditDialog] Payment failed: {}", result.getMessage());
            showError("Payment failed: " + result.getMessage());
        }
    }
    
    private void showError(String message) {
        lblCredit.setText(message);
        lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    updateCredit(txtCash.getText());
                    txtCash.setStyle("");
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