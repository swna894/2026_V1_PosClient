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
@FxmlView("/view/pos/dialog/CreditPaymentDialog.fxml") // 실제 FXML 파일 경로 확인 필요
public class CreditDialogController {

    private static final String CURRENCY_FORMAT = "$%.2f";
    // 숫자와 최대 하나의 소수점만 허용하는 정규식 패턴
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");

    @FXML private Label lblAmount;      // 총 금액
    @FXML private Label lblDiscount;    // 할인 금액
    @FXML private TextField txtCash;    // 받은 현금 (입력)
    @FXML private Label lblEftpos;      // 남은 카드 결제액 (자동 계산)

    private BigDecimal totalToPay;      // (Amount - Discount)
    private BiConsumer<BigDecimal, BigDecimal> onPaymentComplete; // (현금분, 카드분) 콜백

    /**
     * 다이얼로그 초기화
     */
    public void initData(BigDecimal total, BigDecimal discount, BiConsumer<BigDecimal, BigDecimal> callback) {
        this.totalToPay = total.subtract(discount);
        this.onPaymentComplete = callback;

        this.lblAmount.setText(String.format(CURRENCY_FORMAT, total));
        this.lblDiscount.setText(String.format(CURRENCY_FORMAT, discount));
        updateCreditAmount("0"); // 초기화

        // 숫자 전용 텍스트 포매터 적용 (숫자와 . 만 허용) [추가]
        applyNumericFilter(txtCash);

        // 현금 입력 시 실시간으로 카드 결제액 계산
        txtCash.textProperty().addListener((observable, oldValue, newValue) -> {
            updateCreditAmount(newValue);
        });

        //  키 이벤트 리스너 추가 (Enter = Confirm, ESC = Cancel)
        txtCash.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
            handleConfirm();
            event.consume(); // 이벤트 전파 방지
            } else if (event.getCode() == KeyCode.ESCAPE) {
            handleCancel();
            event.consume();
            }
        });

        txtCash.requestFocus();
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


    private void updateCreditAmount(String cashInput) {
        try {
            BigDecimal cashPart = new BigDecimal(cashInput.isEmpty() ? "0" : cashInput);
            BigDecimal creditPart = totalToPay.subtract(cashPart);

            // 카드 결제액이 0보다 작을 수 없음 (현금이 총액을 초과할 경우 처리)
            if (creditPart.compareTo(BigDecimal.ZERO) < 0) {
                lblEftpos.setText(String.format(CURRENCY_FORMAT, 0.0));
                lblEftpos.setStyle("-fx-text-fill: gray;");
            } else {
                lblEftpos.setText(String.format(CURRENCY_FORMAT, creditPart));
                lblEftpos.setStyle("-fx-text-fill: blue;");
            }
        } catch (NumberFormatException e) {
            lblEftpos.setText("Invalid");
        }
    }

    @FXML
    private void handleConfirm() {
        try {
            BigDecimal cashPart = new BigDecimal(txtCash.getText().isEmpty() ? "0" : txtCash.getText());
            BigDecimal creditPart = totalToPay.subtract(cashPart);
            
            // 현금이 총액보다 많으면 카드분은 0으로 처리
            if (creditPart.compareTo(BigDecimal.ZERO) < 0) creditPart = BigDecimal.ZERO;

            if (onPaymentComplete != null) {
                onPaymentComplete.accept(cashPart, creditPart);
            }
            closeDialog();
        } catch (NumberFormatException e) {
            txtCash.requestFocus();
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) txtCash.getScene().getWindow();
        stage.close();
    }
}
