package com.swna.javafx.pos.dialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.model.PosItem;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/ItemPriceChangeDialog.fxml")
public class ItemPriceChangeDialogController {

    @FXML private Label lblItemBarcode;
    @FXML private Label lblOriginalPrice;
    @FXML private TextField txtNewPrice;

    private Consumer<Double> onResultCallback;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        applyNumericFilter(txtNewPrice);
        setupKeyEvents(txtNewPrice);
    }

    /**
     * FXML의 StackPane(paneNewPrice) 클릭 시 호출됨
     * 에러 원인이었던 메서드 누락을 해결합니다.
     */
    @FXML
    private void handlePriceRowClick(MouseEvent event) {
        txtNewPrice.requestFocus();
        txtNewPrice.selectAll();
    }

    private void applyNumericFilter(TextField textField) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            // 숫자와 소수점 한 개만 허용하는 정규식
            if (newText.matches("\\d*\\.?\\d*")) return change;
            return null;
        };
        textField.setTextFormatter(new TextFormatter<>(filter));
    }

    private void setupKeyEvents(TextField textField) {
        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F12 || event.getCode() == KeyCode.ENTER) {
                handleConfirm();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                handleCancel();
                event.consume();
            }
        });
    }

    public void initData(PosItem item, Consumer<Double> callback) {
        this.lblItemBarcode.setText(item.getBarcode());
        this.lblOriginalPrice.setText(String.format("%.2f", item.getOriginalPrice()));
        this.onResultCallback = callback;
        this.txtNewPrice.setText(String.format("%.2f", item.getOriginalPrice()));

        // 초기 포커스 설정[cite: 2]
        Platform.runLater(() -> {
            txtNewPrice.requestFocus();
            txtNewPrice.selectAll();
        });

        // 윈도우 드래그 이동 기능 설정
        Platform.runLater(() -> {
            if (txtNewPrice.getScene() != null) {
                setupDragEvents();
            }
        });
    }

    private void setupDragEvents() {
        txtNewPrice.getScene().setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        txtNewPrice.getScene().setOnMouseDragged(event -> {
            Stage stage = (Stage) txtNewPrice.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
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
            txtNewPrice.requestFocus();
        }
    }

    @FXML 
    private void handleCancel() { 
        close(); 
    }

    private void close() { 
        if (txtNewPrice.getScene() != null) {
            ((Stage) txtNewPrice.getScene().getWindow()).close();
        }
    }
}