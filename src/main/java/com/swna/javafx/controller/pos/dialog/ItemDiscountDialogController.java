package com.swna.javafx.controller.pos.dialog;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter; // 입력 제한을 위한 클래스
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j 
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/ItemDiscountDialog.fxml")
public class ItemDiscountDialogController {
    // SLF4J 로거 설정 (logback.xml 설정에 따라 파일 기록)


    @FXML private Label lblItemBarcode;
    @FXML private TextField txtPrice;
    @FXML private TextField txtPercent;
    @FXML private RadioButton rbPrice;
    @FXML private RadioButton rbPercent;

    private double originalPrice;
    private Consumer<Double> onResultCallback;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // 1. 단축키 설정[cite: 6]
        setupKeyEvents(txtPrice);
        setupKeyEvents(txtPercent);
        
        // 2. 숫자 및 마침표(.) 입력 제한 설정
        applyNumericFilter(txtPrice);
        applyNumericFilter(txtPercent);
        
        txtPrice.requestFocus();
    }

    /**
     * TextField에 숫자와 마침표만 허용하는 Filter 적용
     */
    private void applyNumericFilter(TextField textField) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            // 정규식: 숫자와 최대 하나의 마침표만 허용
            if (newText.matches("\\d*\\.?\\d*")) {
                return change;
            }
            return null; // 조건에 맞지 않으면 입력 무시
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

    @FXML
    private void handleConfirm() {
        try {
            double finalPrice;
            if (rbPrice.isSelected()) {
                finalPrice = Double.parseDouble(txtPrice.getText().isEmpty() ? "0" : txtPrice.getText());
            } else {
                double percent = Double.parseDouble(txtPercent.getText().isEmpty() ? "0" : txtPercent.getText());
                finalPrice = originalPrice * (1 - (percent / 100.0));
            }
            if (onResultCallback != null) onResultCallback.accept(finalPrice);
            close();
        } catch (NumberFormatException e) {
            // 콘솔 대신 로그 파일에 기록 (SLF4J)
            log.error("Error converting discount value: barcode={}, input={}, error={}", 
                      lblItemBarcode.getText(), 
                      rbPrice.isSelected() ? txtPrice.getText() : txtPercent.getText(), 
                      e.getMessage());
        }
    }

    // 기존 handlePriceRowClick, initData, close 등 생략 (동일하게 유지)[cite: 6]
    @FXML
    private void handlePriceRowClick(MouseEvent event) {
        rbPrice.setSelected(true);
        txtPercent.setText("0");
        txtPrice.requestFocus();
        txtPrice.selectAll();
    }

    @FXML
    private void handlePercentRowClick(MouseEvent event) {
        rbPercent.setSelected(true);
        txtPrice.setText("0");
        txtPercent.requestFocus();
        txtPercent.selectAll();
    }

    public void initData(String barcode, double price, Consumer<Double> callback) {
        this.lblItemBarcode.setText(barcode);
        this.originalPrice = price;
        this.onResultCallback = callback;
        this.txtPrice.setText(String.format("%.2f", price));
        this.txtPercent.setText("0");

        txtPrice.disableProperty().bind(rbPercent.selectedProperty());
        txtPercent.disableProperty().bind(rbPrice.selectedProperty());

        lblItemBarcode.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F12) handleConfirm();
                    else if (event.getCode() == KeyCode.ESCAPE) handleCancel();
                });
                newScene.setOnMousePressed(event -> {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                });
                newScene.setOnMouseDragged(event -> {
                    Stage stage = (Stage) newScene.getWindow();
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                });
            }
        });
    }

    @FXML private void handleCancel() { close(); }
    private void close() { ((Stage) lblItemBarcode.getScene().getWindow()).close(); }
}