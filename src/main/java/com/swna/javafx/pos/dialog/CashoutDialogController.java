package com.swna.javafx.pos.dialog;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/CashoutPaymentDialog.fxml") // FXML 경로 지정
public class CashoutDialogController {

    private static final String CURRENCY_FORMAT = "$%.2f";
    // 숫자와 최대 하나의 소수점만 허용하는 정규식 패턴
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");

    @FXML private Label lblAmount;      // 원금액[cite: 2]
    @FXML private Label lblDiscount;    // 할인금액[cite: 2]
    @FXML private TextField txtCashout; // 추가 인출(현금) 금액[cite: 2]
    @FXML private Label lblEftpos;      // 최종 카드 단말기 요청 금액[cite: 2]

    private BigDecimal baseAmount;      // (Total Amount - Discount)
    private BiConsumer<BigDecimal, BigDecimal> onProcessComplete; // (Cashout, EftposTotal) 콜백

    /**
     * 다이얼로그 초기화
     * @param amount 결제 원금액
     * @param discount 적용된 할인액
     * @param callback 완료 시 (현금인출액, 카드결제액)을 전달할 콜백
     */
    public void initData(BigDecimal amount, BigDecimal discount, BiConsumer<BigDecimal, BigDecimal> callback) {
        this.baseAmount = amount.subtract(discount);
        this.onProcessComplete = callback;

        // 초기 UI 세팅
        this.lblAmount.setText(String.format(CURRENCY_FORMAT, amount));
        this.lblDiscount.setText(String.format(CURRENCY_FORMAT, discount));
        updateEftposAmount("0"); // 초기 현금인출 0원 기준

        // 숫자 전용 텍스트 포매터 적용 (숫자와 . 만 허용) [추가]
        applyNumericFilter(txtCashout);

        // Cashout 입력 시 실시간 EFTPOS 합산 금액 계산
        txtCashout.textProperty().addListener((observable, oldValue, newValue) -> {
            updateEftposAmount(newValue);
        });

        //  키 이벤트 리스너 추가 (Enter = Confirm, ESC = Cancel)
        txtCashout.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
            handleConfirm();
            event.consume(); // 이벤트 전파 방지
            } else if (event.getCode() == KeyCode.ESCAPE) {
            handleCancel();
            event.consume();
            }
        });


        txtCashout.requestFocus();
    }

    /**
    * TextField에 숫자와 소수점 한 개만 입력 가능하도록 필터 적용 [추가]
    */
    private void applyNumericFilter(TextField textField) {
        TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (NUMERIC_PATTERN.matcher(newText).matches()) {
                return change; // 패턴과 일치하면 변경 허용
            }
            return null; // 일치하지 않으면 변경 거부
        });
        textField.setTextFormatter(textFormatter);
    }

    /**
     * Cashout 금액 변동에 따른 EFTPOS(카드) 결제 총액 업데이트
     */
    private void updateEftposAmount(String cashoutInput) {
        try {
            BigDecimal cashout = new BigDecimal(cashoutInput.isEmpty() ? "0" : cashoutInput);
            BigDecimal totalCredit = baseAmount.add(cashout);

            lblEftpos.setText(String.format(CURRENCY_FORMAT, totalCredit));
            
            // 음수 입력 등에 대한 간단한 UI 처리
            if (cashout.compareTo(BigDecimal.ZERO) < 0) {
                lblEftpos.setStyle("-fx-text-fill: red;");
            } else {
                lblEftpos.setStyle("-fx-text-fill: blue;");
            }
        } catch (NumberFormatException e) {
            lblEftpos.setText("Invalid");
        }
    }

    @FXML
    private void handleConfirm() {
        try {
            BigDecimal cashout = new BigDecimal(txtCashout.getText().isEmpty() ? "0" : txtCashout.getText());
            BigDecimal totalCredit = baseAmount.add(cashout);

            // 비즈니스 로직: Cashout은 0보다 크거나 같아야 함
            if (cashout.compareTo(BigDecimal.ZERO) >= 0) {
                if (onProcessComplete != null) {
                    onProcessComplete.accept(cashout, totalCredit);
                }
                closeDialog();
            } else {
                txtCashout.requestFocus();
            }
        } catch (NumberFormatException e) {
            log.error("Invalid cashout amount entered");
            txtCashout.requestFocus();
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) txtCashout.getScene().getWindow();
        stage.close();
    }
}
