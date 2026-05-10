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

    @FXML private Label lblAmount;      // Total amount
    @FXML private Label lblDiscount;    // Discount amount
    @FXML private Label lblCredit;      // Card payment amount
    @FXML private TextField txtCashout; // Cash out request amount

    private BigDecimal totalAfterDiscount;  // Final amount after discount
    private BiConsumer<BigDecimal, BigDecimal> onProcessComplete;
    private final CardClient cardClient;

    public void initData(BigDecimal totalAmount, BigDecimal discount, 
                         BiConsumer<BigDecimal, BigDecimal> callback) {
        this.totalAfterDiscount = totalAmount;
        this.onProcessComplete = callback;

        lblAmount.setText(CURRENCY_FORMAT.format(totalAmount));
        lblDiscount.setText(CURRENCY_FORMAT.format(discount));
        
        applyNumericFilter(txtCashout);
        setupKeyEvents(txtCashout);

        txtCashout.setText("0.00");
        txtCashout.selectAll();

        txtCashout.textProperty().addListener((obs, old, val) -> updateTotalCardAmount(val));
        updateTotalCardAmount(txtCashout.getText());
        
        txtCashout.requestFocus();
        
        log.debug("[CashoutDialog] Initialized - totalAfterDiscount: {}", totalAfterDiscount);
    }

    private void updateTotalCardAmount(String input) {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(input);
            BigDecimal totalCardAmount = calculateTotalCardAmount(cashoutAmount);
            
            lblCredit.setText(CURRENCY_FORMAT.format(totalCardAmount));
            lblCredit.setStyle("-fx-text-fill: blue;");
            
            log.debug("[CashoutDialog] cashout: {}, totalCard: {}", cashoutAmount, totalCardAmount);
        } catch (Exception e) {
            lblCredit.setText("Invalid");
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
    
    private boolean isCashoutValid() {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(txtCashout.getText());
            return cashoutAmount.compareTo(BigDecimal.ZERO) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void handleConfirm() {
        if (!isCashoutValid()) {
            txtCashout.setStyle("-fx-border-color: red;");
            lblCredit.setText("Invalid cashout amount");
            lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
            txtCashout.requestFocus();
            return;
        }
        
        try {
            BigDecimal cashoutAmount = parseCashoutInput(txtCashout.getText());
            BigDecimal totalCardAmount = calculateTotalCardAmount(cashoutAmount);
            
            log.info("[CashoutDialog] Payment request - cashout: ${}, totalCard: ${}", 
                cashoutAmount, totalCardAmount);
            
            // Process card payment with cashout
            CardAuthResult result = cardClient.purchaseWithCashOut(totalCardAmount, cashoutAmount);
            
            if (!result.isSuccess()) {
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
                return;
            }
            
            log.info("[CashoutDialog] Card payment successful - authCode: {}, txId: {}, cashout: {}", 
                result.getAuthCode(), result.getTransactionId(), result.getCashOutAmount());
            
            // Execute callback (receipt printing handled by caller)
            if (onProcessComplete != null) {
                onProcessComplete.accept(cashoutAmount, totalAfterDiscount);
            }
            
            closeDialog();
            
        } catch (Exception e) {
            log.error("[CashoutDialog] Payment error", e);
            showError("Payment error: " + e.getMessage());
            txtCashout.requestFocus();
        }
    }
    
    private void showError(String message) {
        lblCredit.setText(message);
        lblCredit.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        
        // Restore after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    updateTotalCardAmount(txtCashout.getText());
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