package com.swna.javafx.pos.dialog;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import javafx.application.Platform;
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
    private Scene scene = null;

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
     * 다이얼로그 드래그 기능 활성화 (전체 영역)
     */
    protected void enableFullWindowDrag() {
        this.dragEnabled = true;
        
        // Stage가 준비될 때까지 대기
        Platform.runLater(() -> {
            Stage stage = getCurrentStage();
            if (stage != null && stage.getScene() != null) {
                setupDragListeners(stage.getScene());
            } else {
                // Scene이 아직 없으면 리스너 등록
                Node focusField = getFocusField();
                if (focusField != null) {
                    focusField.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null && newScene.getWindow() instanceof Stage) {
                            setupDragListeners(newScene);
                        }
                    });
                }
            }
        });
    }

    /**
     * 드래그 리스너 설정 (Scene 전체)
     */
    private void setupDragListeners(Scene scene) {
        if (scene == null || !dragEnabled) return;
        
        this.scene = scene;
        
        // 마우스 이벤트 리스너 추가
        scene.setOnMousePressed(this::handleMousePressed);
        scene.setOnMouseDragged(this::handleMouseDragged);
        
        System.out.println("[BasePaymentDialog] Drag listeners enabled for scene");
    }

    /**
     * 마우스 클릭 시 오프셋 저장
     */
    private void handleMousePressed(MouseEvent event) {
        if (!dragEnabled) return;
        
        Stage stage = getCurrentStage();
        if (stage != null) {
            xOffset = event.getScreenX() - stage.getX();
            yOffset = event.getScreenY() - stage.getY();
            System.out.println("[BasePaymentDialog] Mouse pressed - offset: " + xOffset + ", " + yOffset);
        }
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
            System.out.println("[BasePaymentDialog] Mouse dragged - stage position: " + stage.getX() + ", " + stage.getY());
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
        
        // Scene이 이미 저장되어 있으면 사용
        if (scene != null && scene.getWindow() instanceof Stage) {
            return (Stage) scene.getWindow();
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