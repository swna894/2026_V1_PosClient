package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat; // 1. DecimalFormat 임포트 추가
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

    // 2. 소수점 두 자리를 강제하는 포맷터 선언 (천 단위 콤마 + 소수점 두 자리 고정)
    private static final DecimalFormat DECIMAL_2_FORMAT = new DecimalFormat("$#,##0.00");

    // PaymentDialogManager.PaymentDialogController.initData 참조
    public void initData(BigDecimal total, BigDecimal discount, Consumer<BigDecimal> callback) {
        // 3. setScale을 0에서 2로 변경하여 소수점 두 자리를 유지하도록 설정
        this.totalAmount = total.setScale(2, RoundingMode.HALF_UP);
        this.onPaymentComplete = callback;

        // 4. 상단 금액 표시 라벨들도 소수점 두 자리가 보이도록 DECIMAL_2_FORMAT 적용
        lblAmount.setText(DECIMAL_2_FORMAT.format(total));
        lblDiscount.setText(DECIMAL_2_FORMAT.format(discount));

         // 🔥 방법 1: 전체 창 드래그 활성화 (가장 간단)
        enableFullWindowDrag();
        
        // 추상 클래스 기능 활용
        applyNumericFilter(txtCash);
        setupKeyEvents(txtCash);

        // 5. 텍스트 필드 초기값도 소수점 두 자리 형식으로 설정 (예: 10.00)
        txtCash.setText(totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        txtCash.selectAll();
        txtCash.textProperty().addListener((obs, old, val) -> updateUI(val));
        updateUI(txtCash.getText());
        txtCash.requestFocus();
    }

    private void updateUI(String input) {
        try {
            BigDecimal received = new BigDecimal(input.isEmpty() || input.equals(".") ? "0" : input);
            // 입력받은 값과 totalAmount 모두 소수점 2자리로 정렬하여 연산
            received = received.setScale(2, RoundingMode.HALF_UP);
            BigDecimal change = received.subtract(totalAmount);

            // 6. CURRENCY_FORMAT 대신 DECIMAL_2_FORMAT을 사용하여 잔돈을 소수점 두 자리까지 표시
            lblBalance.setText(change.compareTo(BigDecimal.ZERO) >= 0 ? DECIMAL_2_FORMAT.format(change) : "Insufficient");
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