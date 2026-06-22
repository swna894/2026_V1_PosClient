package com.swna.javafx.pos.dialog;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.model.PosItem;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/QuickItemDialog.fxml") // 또는 ItemManualRegisterDialog.fxml
public class QuickItemDialogController {

    @FXML private TextField txtNewPrice;
    @FXML private GridPane gridPane; // 필요 시 레이아웃 제어용

    private Consumer<Double> onResultCallback;

    @FXML
    public void initialize() {
        applyNumericFilter(txtNewPrice);
        setupKeyEvents(txtNewPrice);
    }

   

    private void applyNumericFilter(TextField textField) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*\\.?\\d*")) return change;
            return null;
        };
        textField.setTextFormatter(new TextFormatter<>(filter));
    }

    private void setupKeyEvents(TextField textField) {
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

    /**
     * 1. 기존 가격 변경용 초기화
     */
    public void initData(PosItem item, Consumer<Double> callback) {
        this.onResultCallback = callback;
        this.txtNewPrice.setText(String.format("%.2f", item.getOriginalPrice()));
        focusTextField();
    }

    /**
     * 2. 신규 미등록 상품 등록용 초기화 (새로운 FXML 사용 시 호출)
     */
    public void initUnregisteredItem(Consumer<Double> callback) {
        this.onResultCallback = callback;
        this.txtNewPrice.setText("");
        this.txtNewPrice.setPromptText("Enter price");
        focusTextField();
    }

    private void focusTextField() {
        Platform.runLater(() -> {
            txtNewPrice.requestFocus();
            txtNewPrice.selectAll();
        });
    }

    @FXML
    private void handleConfirm() {
        try {
            String text = txtNewPrice.getText().trim();
            double newPrice = Double.parseDouble(text.isEmpty() ? "0" : text);
            
            if (onResultCallback != null) {
                onResultCallback.accept(newPrice);
            }
            close();
        } catch (NumberFormatException e) {
            log.error("Price conversion error: {}", e.getMessage());
        }
    }

    @FXML 
    private void handleCancel() { close(); }

    private void close() { 
        if (txtNewPrice.getScene() != null) {
            ((Stage) txtNewPrice.getScene().getWindow()).close();
        }
    }
}