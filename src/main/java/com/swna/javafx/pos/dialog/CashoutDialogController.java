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
@FxmlView("/view/pos/dialog/CashoutPaymentDialog.fxml")
public class CashoutDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblCredit;
    @FXML private TextField txtCashout;

    private BigDecimal baseAmount;
    private BiConsumer<BigDecimal, BigDecimal> onProcessComplete;

    public void initData(BigDecimal amount, BigDecimal discount, BiConsumer<BigDecimal, BigDecimal> callback) {
        this.baseAmount = amount;
        this.onProcessComplete = callback;

        lblAmount.setText(String.format(CURRENCY_FORMAT, amount));
        lblDiscount.setText(String.format(CURRENCY_FORMAT, discount));
        
        applyNumericFilter(txtCashout);
        setupKeyEvents(txtCashout);

        txtCashout.setText("0.00");
        txtCashout.selectAll();

        txtCashout.textProperty().addListener((obs, old, val) -> updateEftpos(val));
        updateEftpos(txtCashout.getText());
        txtCashout.requestFocus();
    }

    private void updateEftpos(String input) {
        try {
            BigDecimal cashout = new BigDecimal(input.isEmpty() || input.equals(".") ? "0" : input);
            lblCredit.setText(String.format(CURRENCY_FORMAT, baseAmount.add(cashout)));
            lblCredit.setStyle("-fx-text-fill: blue;");
        } catch (Exception e) { lblCredit.setText("Invalid"); }
    }

    @Override
    protected void handleConfirm() {
        try {
            BigDecimal cashout = new BigDecimal(txtCashout.getText().isEmpty() ? "0" : txtCashout.getText());
            if (onProcessComplete != null) onProcessComplete.accept(cashout, baseAmount.add(cashout));
            closeDialog();
        } catch (Exception e) { txtCashout.requestFocus(); }
    }

    @Override
    protected TextField getFocusField() { return txtCashout; }
}