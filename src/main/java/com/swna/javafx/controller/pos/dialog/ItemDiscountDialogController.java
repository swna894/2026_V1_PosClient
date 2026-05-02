package com.swna.javafx.controller.pos.dialog;

import java.util.function.Consumer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/ItemDiscountDialog.fxml")
public class ItemDiscountDialogController {
    @FXML private Label lblItemBarcode;
    @FXML private TextField txtPriceAfterDC;
    @FXML private TextField txtDiscountPercent;
    @FXML private RadioButton rbPrice;
    @FXML private RadioButton rbPercent;

    private double originalPrice;
    private Consumer<Double> onResultCallback;

    // 드래그 이동을 위한 좌표 변수
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // 마우스 진입/이탈 시 자동 선택 설정
        setupAutoSelect(txtPriceAfterDC);
        setupAutoSelect(txtDiscountPercent);

        // 엔터키/ESC키 이벤트 설정
        setupKeyEvents(txtPriceAfterDC);
        setupKeyEvents(txtDiscountPercent);
    }

    private void setupAutoSelect(TextField textField) {
        // Mouse In: 포커스 주고 전체 선택
        textField.setOnMouseEntered(e -> {
            if (!textField.isDisabled()) {
                textField.requestFocus();
                textField.selectAll();
            }
        });
        // Mouse Out: 선택 해제
        textField.setOnMouseExited(e -> textField.deselect());
    }

    private void setupKeyEvents(TextField textField) {
        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) handleConfirm();
            else if (event.getCode() == KeyCode.ESCAPE) handleCancel();
        });
    }

    public void initData(String barcode, double price, Consumer<Double> callback) {
        this.lblItemBarcode.setText(barcode);
        this.originalPrice = price;
        this.onResultCallback = callback;
        this.txtPriceAfterDC.setText(String.format("%.2f", price));
        this.txtDiscountPercent.setText("0");

        txtPriceAfterDC.disableProperty().bind(rbPercent.selectedProperty());
        txtDiscountPercent.disableProperty().bind(rbPrice.selectedProperty());

        // 창 이동(드래그) 기능 추가: 헤더가 없으므로 씬 전체에 드래그 적용
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
                finalPrice = Double.parseDouble(txtPriceAfterDC.getText());
            } else {
                double percent = Double.parseDouble(txtDiscountPercent.getText());
                finalPrice = originalPrice * (1 - (percent / 100.0));
            }
            if (onResultCallback != null) onResultCallback.accept(finalPrice);
            close();
        } catch (NumberFormatException e) {
            // 숫자 변환 오류 시 처리
        }
    }

    @FXML private void handleCancel() { close(); }
    private void close() { ((Stage) lblItemBarcode.getScene().getWindow()).close(); }
}