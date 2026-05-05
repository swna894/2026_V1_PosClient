package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;

@Component 
@Scope("prototype")
@FxmlView("/view/pos/dialog/CreditPaymentDialog.fxml")
public class CreditDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblCredit;
    @FXML private TextField txtCash;

    private BigDecimal totalToPay;
    private BiConsumer<BigDecimal, BigDecimal> onPaymentComplete;

    public void initData(BigDecimal total, BigDecimal discount, BiConsumer<BigDecimal, BigDecimal> callback) {
        // 할인 적용된 최종 금액 계산
        this.totalToPay = total;
        this.onPaymentComplete = callback;

        // ✅ static 필드는 클래스 이름으로 접근 (BasePaymentDialog.CURRENCY_FORMAT)
        lblAmount.setText(BasePaymentDialog.CURRENCY_FORMAT.format(total));
        lblDiscount.setText(BasePaymentDialog.CURRENCY_FORMAT.format(discount));

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
            
            // 현금이 totalToPay보다 큰 경우
            if (cash.compareTo(totalToPay) > 0) {
                // ✅ static 필드는 클래스 이름으로 접근
                lblCredit.setText("inValid");
                lblCredit.setStyle("-fx-text-fill: orange;");
                return;
            }
            
            BigDecimal credit = totalToPay.subtract(cash);
            
            if (credit.compareTo(BigDecimal.ZERO) > 0) {
                // ✅ static 필드는 클래스 이름으로 접근
                lblCredit.setText(BasePaymentDialog.CURRENCY_FORMAT.format(credit));
                lblCredit.setStyle("-fx-text-fill: blue;");
            } else {
                // ✅ static 필드는 클래스 이름으로 접근
                lblCredit.setText(BasePaymentDialog.CURRENCY_FORMAT.format(BigDecimal.ZERO));
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
            lblCredit.setText("Please enter valid cash amount");
            lblCredit.setStyle("-fx-text-fill: red;");
            txtCash.requestFocus();
            return;
        }
        
        try {
            BigDecimal cash = parseCashInput(txtCash.getText());
            BigDecimal credit = totalToPay.subtract(cash);
            
            if (onPaymentComplete != null) {
                onPaymentComplete.accept(cash, credit);
            }
            closeDialog();
            
        } catch (Exception e) {
            txtCash.requestFocus();
        }
    }

    @Override
    protected TextField getFocusField() { 
        return txtCash; 
    }
}