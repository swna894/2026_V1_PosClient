package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;

@Component @Scope("prototype")
@FxmlView("/view/pos/dialog/CreditPaymentDialog.fxml")
public class CreditDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblCredit;
    @FXML private TextField txtCash;

    private BigDecimal totalToPay;
    private BiConsumer<BigDecimal, BigDecimal> onPaymentComplete;

    public void initData(BigDecimal total, BigDecimal discount, BiConsumer<BigDecimal, BigDecimal> callback) {
        this.totalToPay = total.subtract(discount);
        this.onPaymentComplete = callback;

        lblAmount.setText(String.format(CURRENCY_FORMAT, total));
        lblDiscount.setText(String.format(CURRENCY_FORMAT, discount));

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
            BigDecimal cash = new BigDecimal(input.isEmpty() || input.equals(".") ? "0" : input);
            BigDecimal credit = totalToPay.subtract(cash);
            
            lblCredit.setText(String.format(CURRENCY_FORMAT, credit.max(BigDecimal.ZERO)));
            lblCredit.setStyle(credit.compareTo(BigDecimal.ZERO) >= 0 ? "-fx-text-fill: blue;" : "-fx-text-fill: gray;");
        } catch (Exception e) { lblCredit.setText("Invalid"); }
    }

    @Override
    protected void handleConfirm() {
        try {
            BigDecimal cash = new BigDecimal(txtCash.getText().isEmpty() ? "0" : txtCash.getText());
            BigDecimal credit = totalToPay.subtract(cash).max(BigDecimal.ZERO);
            if (onPaymentComplete != null) onPaymentComplete.accept(cash, credit);
            closeDialog();
        } catch (Exception e) { txtCash.requestFocus(); }
    }

    @Override
    protected TextField getFocusField() { return txtCash; }
}
