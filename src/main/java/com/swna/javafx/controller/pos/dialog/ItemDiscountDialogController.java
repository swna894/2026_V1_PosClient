package com.swna.javafx.controller.pos.dialog;

import java.util.function.Consumer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent; // MouseEvent 추가
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/ItemDiscountDialog.fxml")
public class ItemDiscountDialogController {
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
        // 엔터키/ESC키 이벤트 설정
        setupKeyEvents(txtPrice);
        setupKeyEvents(txtPercent);
        
        // 초기 포커스 설정
        txtPrice.requestFocus();
    }

    /**
     * FXML의 Price 행(StackPane) 클릭 시 호출됨
     */
    @FXML
    private void handlePriceRowClick(MouseEvent event) {
        rbPrice.setSelected(true);
        txtPercent.setText("0");
        txtPrice.requestFocus();
        txtPrice.selectAll(); // 기존 금액 수정 용이하도록 전체 선택
    }

    /**
     * FXML의 Percent 행(StackPane) 클릭 시 호출됨
     */
    @FXML
    private void handlePercentRowClick(MouseEvent event) {
        rbPercent.setSelected(true);
        txtPrice.setText("0");
        txtPercent.requestFocus();
        txtPercent.selectAll();
    }

    private void setupKeyEvents(TextField textField) {
        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) handleConfirm();
            else if (event.getCode() == KeyCode.ESCAPE) handleCancel();
            // F12 키로도 확인 가능하도록 추가 (FXML 버튼 텍스트 반영)
            else if (event.getCode() == KeyCode.F12) handleConfirm();
        });
    }

    public void initData(String barcode, double price, Consumer<Double> callback) {
        this.lblItemBarcode.setText(barcode);
        this.originalPrice = price;
        this.onResultCallback = callback;
        this.txtPrice.setText(String.format("%.2f", price));
        this.txtPercent.setText("0");

        // UI 바인딩: 선택되지 않은 쪽의 입력창 비활성화
        txtPrice.disableProperty().bind(rbPercent.selectedProperty());
        txtPercent.disableProperty().bind(rbPrice.selectedProperty());

        // 드래그 이동 기능
        lblItemBarcode.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
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

    @FXML
    private void handleConfirm() {
        try {
            double finalPrice;
            if (rbPrice.isSelected()) {
                finalPrice = Double.parseDouble(txtPrice.getText());
            } else {
                double percent = Double.parseDouble(txtPercent.getText());
                finalPrice = originalPrice * (1 - (percent / 100.0));
            }
            if (onResultCallback != null) onResultCallback.accept(finalPrice);
            close();
        } catch (NumberFormatException e) {
            // 숫자 입력 오류 시 txtPrice나 txtPercent 스타일 변경 등 추가 처리 가능
            System.err.println("Invalid number format");
        }
    }

    @FXML private void handleCancel() { close(); }
    private void close() { ((Stage) lblItemBarcode.getScene().getWindow()).close(); }
}