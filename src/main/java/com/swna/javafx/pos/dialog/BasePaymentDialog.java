package com.swna.javafx.pos.dialog;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public abstract class BasePaymentDialog {
    protected static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");
    protected static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean dragEnabled = false;

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

    /**
     * 다이얼로그 드래그 기능 활성화 (기본 비활성화)
     * @param node 드래그 시작점이 될 Node (예: 타이틀바, 패널 등)
     */
    protected void enableDrag(Node node) {
        this.dragEnabled = true;
        setupDragListeners(node);
    }

    /**
     * 전체 Scene에 드래그 기능 활성화 (다이얼로그 전체 드래그)
     */
    protected void enableFullWindowDrag() {
        Node focusField = getFocusField();
        if (focusField != null && focusField.getScene() != null) {
            enableDrag(focusField.getScene().getRoot());
        } else {
            // Scene이 아직 설정되지 않은 경우 리스너 등록
            if (focusField != null) {
                focusField.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        enableDrag(newScene.getRoot());
                    }
                });
            }
        }
    }

    /**
     * 드래그 리스너 설정
     */
    private void setupDragListeners(Node node) {
        if (node == null) return;
        
        // Scene이 있으면 리스너 등록 (중복 방지를 위해 기존 리스너 제거 후 추가)
        Scene scene = node.getScene();
        if (scene != null) {
            scene.setOnMousePressed(this::handleMousePressed);
            scene.setOnMouseDragged(this::handleMouseDragged);
        } else {
            // Scene이 아직 없으면 나중에 설정
            node.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.setOnMousePressed(this::handleMousePressed);
                    newScene.setOnMouseDragged(this::handleMouseDragged);
                }
            });
        }
    }

    /**
     * 마우스 클릭 시 오프셋 저장
     */
    private void handleMousePressed(MouseEvent event) {
        if (!dragEnabled) return;
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    /**
     * 마우스 드래그 시 윈도우 위치 이동
     */
    private void handleMouseDragged(MouseEvent event) {
        if (!dragEnabled) return;
        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        }
    }

    /**
     * 현재 Stage 가져오기
     */
    protected Stage getCurrentStage() {
        Node focusField = getFocusField();
        if (focusField != null && focusField.getScene() != null) {
            return (Stage) focusField.getScene().getWindow();
        }
        return null;
    }

    @FXML 
    protected abstract void handleConfirm();

    @FXML 
    protected void handleCancel() {
        closeDialog();
    }

    protected abstract TextField getFocusField();

    protected void closeDialog() {
        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.close();
        }
    }
}