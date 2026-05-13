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
@FxmlView("/view/pos/dialog/CashoutPaymentDialog.fxml")
@RequiredArgsConstructor
public class CashoutDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblCredit;
    @FXML private TextField txtCashout;

    private BigDecimal totalAfterDiscount;
    private TriConsumer<BigDecimal, BigDecimal, String> callback;  // ✅ 하나의 타입으로 통일
    private final CardPaymentService cardClient;

    // ========== Initialization - 단일 메서드만 유지 ==========

    /**
     * 통합 initData 메서드 - TriConsumer만 사용
     */
    public void initData(BigDecimal total, BigDecimal discount, 
                         TriConsumer<BigDecimal, BigDecimal, String> callback) {
        this.totalAfterDiscount = total;
        this.callback = callback;

        setupUI(total, discount);
        setupInputHandlers();
        
        log.debug("[CashoutDialog] Initialized - totalAfterDiscount: {}", totalAfterDiscount);
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
        
        // ✅ TriConsumer로 3개 파라미터 모두 전달
        if (callback != null) {
            callback.accept(cashoutAmount, totalAmount, cardNumber);
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