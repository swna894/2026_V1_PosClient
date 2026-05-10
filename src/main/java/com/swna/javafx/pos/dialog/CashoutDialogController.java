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
@FxmlView("/view/pos/dialog/CashoutPaymentDialog.fxml")
@RequiredArgsConstructor
public class CashoutDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblCredit;
    @FXML private TextField txtCashout;

    private BigDecimal totalAfterDiscount;
    private CashoutCallback callback;
    private final CardClient cardClient;

    // ========== Callback Interfaces ==========
    
    @FunctionalInterface
    public interface CashoutCallback {
        void onPaymentComplete(BigDecimal cashoutAmount, BigDecimal totalAmount, String cardNumber);
    }
    
    // ========== Initialization ==========

    /**
     * 레거시 콜백 지원 (카드번호 없음 - 호환성 유지)
     */
    public void initData(BigDecimal totalAmount, BigDecimal discount, 
                         BiConsumer<BigDecimal, BigDecimal> legacyCallback) {
        this.totalAfterDiscount = totalAmount;
        this.callback = (cashout, total, cardNumber) -> {
            log.debug("[CashoutDialog] Legacy callback - ignoring cardNumber: {}", cardNumber);
            legacyCallback.accept(cashout, total);
        };

        setupUI(totalAmount, discount);
        setupInputHandlers();
        
        log.debug("[CashoutDialog] Initialized with legacy callback - totalAfterDiscount: {}", totalAfterDiscount);
    }
    
    /**
     * 새로운 콜백 지원 (카드번호 포함)
     */
    public void initData(BigDecimal totalAmount, BigDecimal discount, 
                         CashoutCallback callback) {
        this.totalAfterDiscount = totalAmount;
        this.callback = callback;

        setupUI(totalAmount, discount);
        setupInputHandlers();
        
        log.debug("[CashoutDialog] Initialized with callback - totalAfterDiscount: {}", totalAfterDiscount);
    }
    
    private void setupUI(BigDecimal totalAmount, BigDecimal discount) {
        lblAmount.setText(CURRENCY_FORMAT.format(totalAmount));
        lblDiscount.setText(CURRENCY_FORMAT.format(discount));
        
        txtCashout.setText("0.00");
        txtCashout.selectAll();
        
        applyNumericFilter(txtCashout);
        setupKeyEvents(txtCashout);
        
        txtCashout.requestFocus();
    }
    
    private void setupInputHandlers() {
        txtCashout.textProperty().addListener((obs, old, val) -> updateTotalCardAmount(val));
        updateTotalCardAmount(txtCashout.getText());
    }

    // ========== UI Update Methods ==========
    
    private void updateTotalCardAmount(String input) {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(input);
            BigDecimal totalCardAmount = calculateTotalCardAmount(cashoutAmount);
            
            lblCredit.setText(CURRENCY_FORMAT.format(totalCardAmount));
            lblCredit.setStyle("-fx-text-fill: blue;");
            
            log.debug("[CashoutDialog] cashout: {}, totalCard: {}", cashoutAmount, totalCardAmount);
        } catch (Exception e) {
            lblCredit.setText("Invalid");
            lblCredit.setStyle("-fx-text-fill: red;");
            log.warn("[CashoutDialog] Parse error: {}", input, e);
        }
    }
    
    private BigDecimal parseCashoutInput(String input) {
        if (input == null || input.isEmpty() || input.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(input);
    }
    
    private BigDecimal calculateTotalCardAmount(BigDecimal cashoutAmount) {
        return totalAfterDiscount.add(cashoutAmount);
    }
    
    // ========== Validation Methods ==========
    
    private boolean isCashoutValid() {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(txtCashout.getText());
            return cashoutAmount.compareTo(BigDecimal.ZERO) >= 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showValidationError() {
        txtCashout.setStyle("-fx-border-color: red;");
        lblCredit.setText("Invalid cashout amount");
        lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
        txtCashout.requestFocus();
    }

    // ========== Payment Processing ==========
    
    @Override
    protected void handleConfirm() {
        if (!isCashoutValid()) {
            showValidationError();
            return;
        }
        
        processPayment();
    }
    
    private void processPayment() {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(txtCashout.getText());
            BigDecimal totalCardAmount = calculateTotalCardAmount(cashoutAmount);
            
            log.info("[CashoutDialog] Payment request - cashout: ${}, totalCard: ${}", 
                cashoutAmount, totalCardAmount);
            
            // 카드 결제 실행
            CardAuthResult result = executeCardPayment(cashoutAmount, totalCardAmount);
            
            if (!result.isSuccess()) {
                handlePaymentFailure(result);
                return;
            }
            
            // 결제 성공 처리
            handlePaymentSuccess(cashoutAmount, totalAfterDiscount, result);
            
        } catch (Exception e) {
            log.error("[CashoutDialog] Payment error", e);
            showError("Payment error: " + e.getMessage());
            txtCashout.requestFocus();
        }
    }
    
    private CardAuthResult executeCardPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount) {
        if (cashoutAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[CashoutDialog] No cashout - processing normal purchase");
            return cardClient.purchase(totalCardAmount);
        } else {
            log.info("[CashoutDialog] Processing cashout purchase");
            return cardClient.purchaseWithCashOut(totalCardAmount, cashoutAmount);
        }
    }
    
    private void handlePaymentSuccess(BigDecimal cashoutAmount, BigDecimal totalAmount, CardAuthResult result) {
        String cardNumber = result.getCardNumber();
        
        log.info("[CashoutDialog] Card payment successful - authCode: {}, txId: {}, cardNumber: {}, cashout: {}", 
            result.getAuthCode(), result.getTransactionId(), cardNumber, result.getCashOutAmount());
        
        // ✅ 카드번호를 포함한 콜백 실행
        if (callback != null) {
            callback.onPaymentComplete(cashoutAmount, totalAmount, cardNumber);
        }
        
        closeDialog();
    }
    
    private void handlePaymentFailure(CardAuthResult result) {
        if (result.isCancelled()) {
            log.info("[CashoutDialog] Payment cancelled by user");
            showError("Payment cancelled");
        } else if (result.isTimeout()) {
            log.warn("[CashoutDialog] Payment timeout");
            showError("Payment timeout - please try again");
        } else {
            log.error("[CashoutDialog] Payment failed: {}", result.getMessage());
            showError("Payment failed: " + result.getMessage());
        }
    }

    private void showError(String message) {
        lblCredit.setText(message);
        lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        
        // 3초 후 복원
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    updateTotalCardAmount(txtCashout.getText());
                    txtCashout.setStyle("");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    protected TextField getFocusField() {
        return txtCashout;
    }
}