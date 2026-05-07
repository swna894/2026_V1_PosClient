package com.swna.javafx.common.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatusLabelManager {
    
    private Label statusLabel;
    private boolean soundEnabled = true;
    private boolean fadeOutEnabled = true;  // 페이드 아웃 효과 사용 여부
    private double fadeOutDuration = 0.5;   // 페이드 아웃 지속 시간 (초)
    private StringProperty boundProperty;   // 바인딩된 속성 저장
    private boolean isBindingMode = false;  // 바인딩 모드 여부
    
    // 기본 지속 시간 (5초)
    private static final double DEFAULT_DURATION_SECONDS = 5.0;
    
    // CSS 스타일 정의
    private static final String STYLE_SUCCESS = "-fx-text-fill: #2ecc71; -fx-font-weight: bold; ";
    private static final String STYLE_ERROR = "-fx-text-fill: #e74c3c; -fx-font-weight: bold; ";
    private static final String STYLE_INFO = "-fx-text-fill: #3498db; -fx-font-weight: normal;";
    private static final String STYLE_WARNING = "-fx-text-fill: #f39c12; -fx-font-weight: bold; ";
    private static final String STYLE_DEFAULT = "-fx-text-fill: #2c3e50; -fx-background-color: transparent; -fx-padding: 3 6 3 6;";
    
    public void initialize(Label statusLabel) {
        this.statusLabel = statusLabel;
        
        if (statusLabel != null) {
            statusLabel.setStyle(STYLE_DEFAULT);
            statusLabel.setOpacity(1.0);
        }
    }
    
    /**
     * ViewModel의 속성과 바인딩 연결
     * 바인딩된 속성의 변경을 감지하여 자동으로 메시지 표시
     */
    public void bindTo(StringProperty property) {
        if (boundProperty != null) {
            boundProperty.removeListener(bindingListener);
        }
        
        this.boundProperty = property;
        this.isBindingMode = true;
        
        // 바인딩된 속성의 변경 리스너 추가
        boundProperty.addListener(bindingListener);
    }
    
    /**
     * 바인딩 해제
     */
    public void unbind() {
        if (boundProperty != null) {
            boundProperty.removeListener(bindingListener);
            boundProperty = null;
        }
        isBindingMode = false;
    }
    
    /**
     * 바인딩 속성 변경 리스너
     */
    private final javafx.beans.value.ChangeListener<String> bindingListener = 
        (obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                // 바인딩된 값이 변경되면 자동으로 메시지 표시
                autoShowMessage(newVal);
            }
        };
    
    /**
     * 바인딩된 값에 따라 자동으로 메시지 표시
     */
    private void autoShowMessage(String message) {
        String lowerMsg = message.toLowerCase();
        
        if (lowerMsg.contains("success") || lowerMsg.contains("완료") || 
            lowerMsg.contains("성공") || lowerMsg.contains("added") ||
            lowerMsg.contains("✓") || lowerMsg.contains("✅")) {
            showSuccess(message);
        } else if (lowerMsg.contains("error") || lowerMsg.contains("fail") || 
                   lowerMsg.contains("실패") || lowerMsg.contains("오류") ||
                   lowerMsg.contains("✗") || lowerMsg.contains("❌")) {
            showError(message);
        } else if (lowerMsg.contains("warning") || lowerMsg.contains("주의") ||
                   lowerMsg.contains("⚠️")) {
            showWarning(message);
        } else {
            showInfo(message);
        }
    }
    
    /**
     * 성공 메시지 (기본 5초 후 페이드 아웃)
     */
    public void showSuccess(String message) {
        showMessageWithFadeOut(message, STYLE_SUCCESS, true, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 성공 메시지 (지속 시간 지정)
     */
    public void showSuccess(String message, double durationSeconds) {
        showMessageWithFadeOut(message, STYLE_SUCCESS, true, durationSeconds);
    }
    
    /**
     * 에러 메시지 (기본 5초 후 페이드 아웃)
     */
    public void showError(String message) {
        showMessageWithFadeOut(message, STYLE_ERROR, false, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 에러 메시지 (지속 시간 지정)
     */
    public void showError(String message, double durationSeconds) {
        showMessageWithFadeOut(message, STYLE_ERROR, false, durationSeconds);
    }
    
    /**
     * 정보 메시지 (기본 5초 후 페이드 아웃)
     */
    public void showInfo(String message) {
        showMessageWithFadeOut(message, STYLE_INFO, null, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 경고 메시지 (기본 5초 후 페이드 아웃)
     */
    public void showWarning(String message) {
        showMessageWithFadeOut(message, STYLE_WARNING, false, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 기본 메시지 (페이드 아웃 없이 바로 사라짐)
     */
    public void showDefault(String message) {
        showMessageSimple(message, STYLE_DEFAULT, null, DEFAULT_DURATION_SECONDS);
    }
    
    /**
     * 페이드 아웃 효과가 있는 메시지 표시
     * 
     * @param message 표시할 메시지
     * @param style CSS 스타일
     * @param isSuccess true=성공사운드, false=에러사운드, null=사운드 없음
     * @param durationSeconds 표시 시간(초)
     */
    private void showMessageWithFadeOut(String message, String style, Boolean isSuccess, double durationSeconds) {
        if (statusLabel == null) return;
        
        Platform.runLater(() -> {
            // 기존 애니메이션 중지 및 정리
            stopCurrentAnimation();
            
            // 바인딩이 걸려있으면 일시적으로 해제
            boolean wasBound = statusLabel.textProperty().isBound();
            if (wasBound) {
                statusLabel.textProperty().unbind();
            }
            
            // 메시지와 스타일 설정
            statusLabel.setText(message);
            statusLabel.setStyle(style);
            statusLabel.setOpacity(1.0);  // 완전 불투명으로 초기화
            
            // 사운드 재생
            if (soundEnabled && isSuccess != null) {
                if (isSuccess) {
                    SoundManager.playSuccess();
                } else {
                    SoundManager.playError();
                }
            }
            
            if (fadeOutEnabled) {
                // 페이드 아웃 효과 적용
                applyFadeOutAnimation(durationSeconds, wasBound);
            } else {
                // 단순 지연 후 제거
                applySimpleClearAnimation(durationSeconds, wasBound);
            }
        });
    }
    
    /**
     * 페이드 아웃 애니메이션 적용
     */
    private void applyFadeOutAnimation(double durationSeconds, boolean restoreBinding) {
        // 대기 애니메이션 (지정된 시간 동안 표시)
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        
        // 페이드 아웃 애니메이션
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(fadeOutDuration), statusLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        // 순차적으로 실행: 대기 -> 페이드아웃
        SequentialTransition sequential = new SequentialTransition(pause, fadeOut);
        sequential.setOnFinished(event -> {
            // 애니메이션 완료 후 메시지 완전히 제거
            statusLabel.setText("");
            statusLabel.setOpacity(1.0);  // opacity 복원 (다음 메시지를 위해)
            statusLabel.setStyle(STYLE_DEFAULT);
            
            // 바인딩 복원
            if (restoreBinding && boundProperty != null) {
                statusLabel.textProperty().bind(boundProperty);
            }
        });
        
        sequential.play();
    }
    
    /**
     * 단순 제거 애니메이션 (페이드 아웃 없음)
     */
    private void applySimpleClearAnimation(double durationSeconds, boolean restoreBinding) {
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        pause.setOnFinished(event -> {
            statusLabel.setText("");
            statusLabel.setStyle(STYLE_DEFAULT);
            
            // 바인딩 복원
            if (restoreBinding && boundProperty != null) {
                statusLabel.textProperty().bind(boundProperty);
            }
        });
        pause.play();
    }
    
    /**
     * 간단한 메시지 표시 (애니메이션 없음)
     */
    private void showMessageSimple(String message, String style, Boolean isSuccess, double durationSeconds) {
        if (statusLabel == null) return;
        
        Platform.runLater(() -> {
            stopCurrentAnimation();
            
            // 바인딩이 걸려있으면 일시적으로 해제
            boolean wasBound = statusLabel.textProperty().isBound();
            if (wasBound) {
                statusLabel.textProperty().unbind();
            }
            
            statusLabel.setText(message);
            statusLabel.setStyle(style);
            
            if (soundEnabled && isSuccess != null) {
                if (isSuccess) {
                    SoundManager.playSuccess();
                } else {
                    SoundManager.playError();
                }
            }
            
            // 단순 지연 후 제거
            PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
            pause.setOnFinished(event -> {
                statusLabel.setText("");
                statusLabel.setStyle(STYLE_DEFAULT);
                
                // 바인딩 복원
                if (wasBound && boundProperty != null) {
                    statusLabel.textProperty().bind(boundProperty);
                }
            });
            pause.play();
        });
    }
    
    /**
     * 현재 실행 중인 애니메이션 중지
     */
    private void stopCurrentAnimation() {
        // 현재 실행 중인 모든 애니메이션을 중지하는 방법은
        // 실제로는 각 애니메이션을 참조로 저장해야 함
        // 간단한 구현을 위해 별도로 처리하지 않음
    }
    
    /**
     * 즉시 메시지 초기화
     */
    public void clear() {
        if (statusLabel == null) return;
        
        Platform.runLater(() -> {
            // 바인딩 상태 확인
            boolean wasBound = statusLabel.textProperty().isBound();
            if (wasBound) {
                statusLabel.textProperty().unbind();
            }
            
            statusLabel.setText("");
            statusLabel.setStyle(STYLE_DEFAULT);
            statusLabel.setOpacity(1.0);
            
            // 바인딩 복원
            if (wasBound && boundProperty != null) {
                statusLabel.textProperty().bind(boundProperty);
            }
        });
    }
    
    /**
     * 페이드 아웃 효과 활성화/비활성화
     */
    public void setFadeOutEnabled(boolean enabled) {
        this.fadeOutEnabled = enabled;
    }
    
    /**
     * 페이드 아웃 지속 시간 설정
     */
    public void setFadeOutDuration(double seconds) {
        this.fadeOutDuration = seconds;
    }
    
    /**
     * 사운드 활성화/비활성화
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    /**
     * 바인딩 모드 확인
     */
    public boolean isBindingMode() {
        return isBindingMode;
    }
    
    /**
     * 리소스 정리
     */
    public void dispose() {
        unbind();  // 바인딩 해제
        clear();
        log.debug("StatusLabelManager disposed");
    }
}