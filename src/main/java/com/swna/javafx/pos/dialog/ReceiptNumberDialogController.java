package com.swna.javafx.pos.dialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/ReceiptNumberDialog.fxml")
public class ReceiptNumberDialogController {

    @FXML private TextField txtReceiptNumber;
    @FXML private Button btnSearch;
    @FXML private Button btnCancel;

    private Consumer<String> onResultCallback;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        setupKeyEvents();
        
        // ItemPriceChangeDialogController처럼 숫자 필터링이 필요하면 추가
        // applyNumericFilter(txtReceiptNumber);
    }

    private void setupKeyEvents() {
        txtReceiptNumber.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSearch();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                handleCancel();
                event.consume();
            }
        });
    }

    public void initData(String promptText, Consumer<String> callback) {
        this.onResultCallback = callback;
        if (promptText != null) {
            txtReceiptNumber.setPromptText(promptText);
        }

        Platform.runLater(() -> {
            txtReceiptNumber.requestFocus();
            txtReceiptNumber.selectAll();
        });
    }

    @FXML
    private void handleSearch() {
        String receiptNumber = txtReceiptNumber.getText().trim();
        if (receiptNumber.isEmpty()) {
            txtReceiptNumber.requestFocus();
            return;
        }
        
        if (onResultCallback != null) {
            onResultCallback.accept(receiptNumber);
        }
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    @FXML
    private void handleHeaderMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleHeaderMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) txtReceiptNumber.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    private void close() {
        if (txtReceiptNumber.getScene() != null) {
            ((Stage) txtReceiptNumber.getScene().getWindow()).close();
        }
    }
}