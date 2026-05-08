package com.swna.javafx.common.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * 스타일과 사운드 기능이 내장된 StatusLabel
 * 사용법: <StatusLabel fx:id="labelStatus" /> 또는 new StatusLabel()
 */
@Slf4j
public class StatusLabel extends Label {
    
    private boolean soundEnabled = true;
    private boolean fadeOutEnabled = true;
    private double fadeOutDuration = 0.5;
    private StringProperty boundProperty;
    private boolean isShowingTemporaryMessage = false;
    private boolean isInternalTextChange = false;  // 내부 텍스트 변경 여부
    private boolean isBound = false;  // 바인딩 여부 추적
    
    private static final double DEFAULT_DURATION_SECONDS = 10.0;  // 5초에서 10초로 변경
    
    // CSS 스타일
    private static final String STYLE_SUCCESS = "-fx-text-fill: #2ecc71; -fx-font-weight: bold; ";
    private static final String STYLE_ERROR = "-fx-text-fill: #e74c3c; -fx-font-weight: bold; ";
    private static final String STYLE_INFO = "-fx-text-fill: #3498db; -fx-font-weight: normal;";
    private static final String STYLE_WARNING = "-fx-text-fill: #f39c12; -fx-font-weight: bold; ";
    private static final String STYLE_DEFAULT = "-fx-text-fill: #2c3e50; -fx-background-color: transparent; -fx-padding: 3 6 3 6;";
    private static final String STYLE_HIDDEN = "-fx-text-fill: transparent;";
    
    public StatusLabel() {
        super();
        setStyle(STYLE_DEFAULT);
        setOpacity(1.0);
        setupTextListener();
    }
    
    public StatusLabel(String text) {
        super(text);
        setStyle(STYLE_DEFAULT);
        setOpacity(1.0);
        setupTextListener();
    }
    
    /**
     * text 속성 변화를 감지하는 리스너 설정
     */
    private void setupTextListener() {
        this.textProperty().addListener((obs, oldVal, newVal) -> {
            // 내부에서 변경한 텍스트가 아니고, 임시 메시지 표시 중이 아니며, 새 값이 있을 때
            if (!isInternalTextChange && !isShowingTemporaryMessage && newVal != null && !newVal.equals(oldVal)) {
                autoShowMessage(newVal);
            }
            // 플래그 초기화
            isInternalTextChange = false;
        });
    }
    
    /**
     * ViewModel의 속성 구독 (바인딩 방식)
     */
    public void bindTo(StringProperty property) {
        // 기존 바인딩 제거
        if (boundProperty != null) {
            if (textProperty().isBound()) {
                textProperty().unbind();
            }
            boundProperty = null;
        }
        
        this.boundProperty = property;
        this.isBound = true;
        
        // text 속성을 ViewModel 속성에 바인딩
        textProperty().bind(property);
        
        // 초기값에 대해 스타일 적용
        if (property.get() != null) {
            autoShowMessage(property.get());
        }
    }
    
    /**
     * ViewModel의 속성 구독 (리스너 방식 - 바인딩 없이)
     */
    public void subscribeTo(StringProperty property) {
        if (boundProperty != null) {
            boundProperty.removeListener(bindingListener);
        }
        this.boundProperty = property;
        this.isBound = false;
        boundProperty.addListener(bindingListener);
    }
    
    /**
     * 바인딩 해제
     */
    public void unbindProperty() {
        if (textProperty().isBound()) {
            textProperty().unbind();
        }
        if (boundProperty != null) {
            boundProperty.removeListener(bindingListener);
            boundProperty = null;
        }
        isBound = false;
    }
    
    private final javafx.beans.value.ChangeListener<String> bindingListener = 
        (obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal) && !isShowingTemporaryMessage) {
                autoShowMessage(newVal);
            }
        };
    
    /**
     * 메시지 내용에 따라 자동으로 스타일과 사운드 적용
     */
    private void autoShowMessage(String message) {
        String lowerMsg = message.toLowerCase();
        
        if (lowerMsg.contains("success") || lowerMsg.contains("completed") || 
            lowerMsg.contains("완료") || lowerMsg.contains("성공") ||
            lowerMsg.contains("added") || lowerMsg.contains("추가") ||
            lowerMsg.contains("✓") || lowerMsg.contains("✅")) {
            applyStyleAndSound(message, STYLE_SUCCESS, true);
        } else if (lowerMsg.contains("error") || lowerMsg.contains("fail") || 
                   lowerMsg.contains("not found") || lowerMsg.contains("failed") || 
                   lowerMsg.contains("오류") || lowerMsg.contains("없음") ||
                   lowerMsg.contains("✗") || lowerMsg.contains("❌")) {
            applyStyleAndSound(message, STYLE_ERROR, false);
        } else if (lowerMsg.contains("warning") || lowerMsg.contains("주의") ||
                   lowerMsg.contains("⚠️") || lowerMsg.contains("경고")) {
            applyStyleAndSound(message, STYLE_WARNING, false);
        } else if (lowerMsg.contains("scan") || lowerMsg.contains("스캔") ||
                   lowerMsg.contains("info") || lowerMsg.contains("정보")) {
            applyStyleAndSound(message, STYLE_INFO, null);
        } else {
            applyStyleAndSound(message, STYLE_DEFAULT, null);
        }
    }
    
    /**
     * 스타일과 사운드만 적용 (텍스트는 이미 설정되어 있음)
     */
    private void applyStyleAndSound(String message, String style, Boolean isSuccess) {
        Platform.runLater(() -> {
            setStyle(style);
            
            // 사운드 재생
            if (soundEnabled && isSuccess != null) {
                if (isSuccess) {
                    SoundManager.playSuccess();
                } else {
                    SoundManager.playError();
                }
            }
            
            // 10초 후 페이드 아웃 (바인딩 상태일 때는 텍스트를 직접 변경하지 않음)
            if (fadeOutEnabled && !isBound) {
                applyFadeOutAnimation(DEFAULT_DURATION_SECONDS);
            } else if (fadeOutEnabled && isBound) {
                // 바인딩 상태에서는 스타일만 초기화
                applyStyleOnlyReset(DEFAULT_DURATION_SECONDS);
            } else if (!isBound) {
                applySimpleClearAnimation(DEFAULT_DURATION_SECONDS);
            }
        });
    }
    
    /**
     * 스타일만 초기화 (바인딩 상태용)
     */
    private void applyStyleOnlyReset(double durationSeconds) {
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(fadeOutDuration), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        SequentialTransition sequential = new SequentialTransition(pause, fadeOut);
        sequential.setOnFinished(event -> {
            setOpacity(1.0);  // 투명도 복원
            setStyle(STYLE_HIDDEN);  // 기본 스타일로 복원
            isShowingTemporaryMessage = false;
        });
        sequential.play();
    }
    
    /**
     * 성공 메시지 표시 (텍스트와 스타일 함께 변경)
     */
    public void showSuccess(String message) {
        showMessage(message, STYLE_SUCCESS, true, DEFAULT_DURATION_SECONDS);
    }
    
    public void showSuccess(String message, double durationSeconds) {
        showMessage(message, STYLE_SUCCESS, true, durationSeconds);
    }
    
    /**
     * 에러 메시지 표시
     */
    public void showError(String message) {
        showMessage(message, STYLE_ERROR, false, DEFAULT_DURATION_SECONDS);
    }
    
    public void showError(String message, double durationSeconds) {
        showMessage(message, STYLE_ERROR, false, durationSeconds);
    }
    
    /**
     * 정보 메시지 표시
     */
    public void showInfo(String message) {
        showMessage(message, STYLE_INFO, null, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 경고 메시지 표시
     */
    public void showWarning(String message) {
        showMessage(message, STYLE_WARNING, false, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 메시지 표시 (내부 텍스트 변경 시 플래그 설정)
     */
    private void showMessage(String message, String style, Boolean isSuccess, double durationSeconds) {
        Platform.runLater(() -> {
            isShowingTemporaryMessage = true;
            isInternalTextChange = true;  // 내부에서 텍스트 변경 중임을 표시
            
            setText(message);
            setStyle(style);
            setOpacity(1.0);
            
            // 사운드 재생
            if (soundEnabled && isSuccess != null) {
                if (isSuccess) {
                    SoundManager.playSuccess();
                } else {
                    SoundManager.playError();
                }
            }
            
            if (isBound) {
                // 바인딩 상태에서는 페이드아웃 후 텍스트 복원 없이 스타일만 초기화
                applyTemporaryMessageForBound(durationSeconds);
            } else if (fadeOutEnabled) {
                applyFadeOutAnimation(durationSeconds);
            } else {
                applySimpleClearAnimation(durationSeconds);
            }
        });
    }
    
    /**
     * 바인딩 상태에서 임시 메시지 표시
     */
    private void applyTemporaryMessageForBound(double durationSeconds) {
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(fadeOutDuration), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        SequentialTransition sequential = new SequentialTransition(pause, fadeOut);
        sequential.setOnFinished(event -> {
            setOpacity(1.0);
            setStyle(STYLE_DEFAULT);
            isShowingTemporaryMessage = false;
        });
        sequential.play();
    }
    
    private void applyFadeOutAnimation(double durationSeconds) {
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(fadeOutDuration), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        SequentialTransition sequential = new SequentialTransition(pause, fadeOut);
        sequential.setOnFinished(event -> {
            isInternalTextChange = true;
            setText("");
            setOpacity(1.0);
            setStyle(STYLE_DEFAULT);
            isShowingTemporaryMessage = false;
            isInternalTextChange = false;
        });
        sequential.play();
    }
    
    private void applySimpleClearAnimation(double durationSeconds) {
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        pause.setOnFinished(event -> {
            isInternalTextChange = true;
            setText("");
            setStyle(STYLE_DEFAULT);
            isShowingTemporaryMessage = false;
            isInternalTextChange = false;
        });
        pause.play();
    }
    
    /**
     * 현재 텍스트를 기준으로 스타일만 갱신
     */
    public void refreshStyle() {
        String currentText = getText();
        if (currentText != null && !currentText.isEmpty() && !isShowingTemporaryMessage) {
            autoShowMessage(currentText);
        }
    }
    
    public void clear() {
        Platform.runLater(() -> {
            if (isBound) {
                // 바인딩 상태에서는 텍스트를 지울 수 없음
                setStyle(STYLE_DEFAULT);
                setOpacity(1.0);
                isShowingTemporaryMessage = false;
            } else {
                isInternalTextChange = true;
                isShowingTemporaryMessage = false;
                setText("");
                setStyle(STYLE_DEFAULT);
                setOpacity(1.0);
                isInternalTextChange = false;
            }
        });
    }
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    public void setFadeOutEnabled(boolean enabled) {
        this.fadeOutEnabled = enabled;
    }
    
    public void setFadeOutDuration(double seconds) {
        this.fadeOutDuration = seconds;
    }
    
    public void dispose() {
        unbindProperty();
        clear();
    }
}