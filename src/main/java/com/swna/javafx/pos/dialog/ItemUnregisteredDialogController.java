package com.swna.javafx.pos.dialog;

import com.swna.javafx.pos.model.PosItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/ItemUnregisteredDialog.fxml") // 또는 ItemManualRegisterDialog.fxml
public class ItemUnregisteredDialogController {

    @FXML private Label lblItemBarcode;
    @FXML private Label lblOriginalPrice; // FXML에 없을 경우 null이 됨
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
            if (newText.matches("\\d*\\.?\\d*") && newText.length() <= 6) {
                return change;
            }
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
        this.lblItemBarcode.setText(item.getBarcode());
        if (lblOriginalPrice != null) lblOriginalPrice.setText(String.format("%.2f", item.getOriginalPrice()));
        this.onResultCallback = callback;
        this.txtNewPrice.setText(String.format("%.2f", item.getOriginalPrice()));
        focusTextField();
    }

    /**
     * 2. 신규 미등록 상품 등록용 초기화 (새로운 FXML 사용 시 호출)
     */
    public void initUnregisteredItem(String barcode, Consumer<Double> callback) {
        this.lblItemBarcode.setText(barcode);
        if (lblOriginalPrice != null) {
            lblOriginalPrice.setText("0.00");
        }
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

            // 글자 수가 6자 이상인 경우 입력 필드를 비우고 메소드 종료 (혹은 뒤의 로직 수행 방지)
            if (text.length() >= 6) {
                txtNewPrice.clear();
                return; // 6자 이상일 때 캐치문이나 콜백으로 넘어가지 않고 바로 종료하려면 추가
            }
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