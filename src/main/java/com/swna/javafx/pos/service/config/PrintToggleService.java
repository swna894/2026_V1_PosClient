package com.swna.javafx.pos.service.config;

import org.springframework.stereotype.Service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.extern.slf4j.Slf4j;

/**
 * 프린트 ON/OFF 상태를 전역적으로 관리하는 서비스
 * - PosViewController와 ReceiptPrintListener에서 공통으로 사용
 */
@Slf4j
@Service
public class PrintToggleService {
    
    private final BooleanProperty printEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty barcodeEnabled = new SimpleBooleanProperty(false); // 기본값 설정
    private final BooleanProperty isCashedBalance = new SimpleBooleanProperty(false); // 기본값 설정

    public boolean isBarcodeEnabled() { return barcodeEnabled.get(); }
    public void setBarcodeEnabled(boolean enabled) { barcodeEnabled.set(enabled); }
    public BooleanProperty barcodeEnabledProperty() { return barcodeEnabled; }
    
    /**
     * 프린트 활성화 여부 확인
     */
    public boolean isPrintEnabled() {return printEnabled.get();}
    public boolean isCashedBalance() {return isCashedBalance.get();}
    
    /**
     * 프린트 활성화 설정
     */
    public void setPrintEnabled(boolean enabled) { printEnabled.set(enabled);}
    public void setCashBalance(boolean enabled) { isCashedBalance.set(enabled);}
    
    /**
     * 프린트 상태 Property (바인딩용)
     */
    public BooleanProperty printEnabledProperty() {
        return printEnabled;
    }
    
    /**
     * 프린트 상태 토글
     */
    public void toggle() {
        setPrintEnabled(!isPrintEnabled());
    }
    
    /**
     * 상태에 따른 스타일 클래스 반환 (CSS용)
     */
    public String getStatusStyleClass() {
        return isPrintEnabled() ? "print-on" : "print-off";
    }
    
    /**
     * 상태에 따른 텍스트 반환
     */
    public String getStatusText() {
        return isPrintEnabled() ? "ON" : "OFF";
    }
}