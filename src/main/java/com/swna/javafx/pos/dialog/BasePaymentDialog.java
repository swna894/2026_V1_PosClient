package com.swna.javafx.pos.dialog;

import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public abstract class BasePaymentDialog {
    protected static final String CURRENCY_FORMAT = "$%.2f";
    protected static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");

    /** 
     * 숫자와 소수점 하나만 허용하는 필터
     */
    protected void applyNumericFilter(TextField textField) {
        textField.setTextFormatter(new TextFormatter<>(change -> 
            NUMERIC_PATTERN.matcher(change.getControlNewText()).matches() ? change : null));
    }

    /** 
     * Enter는 handleConfirm, ESC는 handleCancel 실행
     */
    protected void setupKeyEvents(TextField textField) {
        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleConfirm();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                handleCancel();
                event.consume();
            }
        });
    }

    @FXML protected abstract void handleConfirm();

    @FXML protected void handleCancel() {
        closeDialog();
    }

    protected abstract TextField getFocusField();

    protected void closeDialog() {
        Stage stage = (Stage) getFocusField().getScene().getWindow();
        stage.close();
    }
}
