package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;

@Component @Scope("prototype")
@FxmlView("/view/pos/dialog/CashPaymentDialog.fxml")
public class CashDialogController extends BasePosDialog {

    @FXML private Label lblAmount;
    @FXML private Label lblBalance;
    @FXML private Label lblDiscount;
    @FXML private TextField txtCash;

    private BigDecimal totalAmount;
    private Consumer<BigDecimal> onPaymentComplete;

    // PaymentDialogManager.PaymentDialogController.initData 참조
    public void initData(BigDecimal total, BigDecimal discount, Consumer<BigDecimal> callback) {
        this.totalAmount = total;
        this.onPaymentComplete = callback;

        lblAmount.setText(CURRENCY_FORMAT.format(total));
        lblDiscount.setText(CURRENCY_FORMAT.format(discount));

         // 🔥 방법 1: 전체 창 드래그 활성화 (가장 간단)
        enableFullWindowDrag();
        
        // 추상 클래스 기능 활용[cite: 7]
        applyNumericFilter(txtCash);
        setupKeyEvents(txtCash);

        txtCash.setText(total.toPlainString());
        txtCash.selectAll();
        
        txtCash.textProperty().addListener((obs, old, val) -> updateUI(val));
        updateUI(txtCash.getText());
        txtCash.requestFocus();
    }

    private void updateUI(String input) {
        try {
            BigDecimal received = new BigDecimal(input.isEmpty() || input.equals(".") ? "0" : input);
            BigDecimal change = received.subtract(totalAmount);
            lblBalance.setText(change.compareTo(BigDecimal.ZERO) >= 0 ? CURRENCY_FORMAT.format(change) : "Insufficient");
            lblBalance.setStyle(change.compareTo(BigDecimal.ZERO) >= 0 ? "-fx-text-fill: blue;" : "-fx-text-fill: red;");
        } catch (Exception e) { lblBalance.setText("Invalid"); }
    }

    @Override
    protected void handleConfirm() {
        try {
            BigDecimal received = new BigDecimal(txtCash.getText());
            if (received.compareTo(totalAmount) >= 0) {
                if (onPaymentComplete != null) onPaymentComplete.accept(received);
                closeDialog();
            }
        } catch (Exception e) { txtCash.requestFocus(); }
    }

    @Override
    protected TextField getFocusField() { return txtCash; }
}