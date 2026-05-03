package com.swna.javafx.infrastructure.scanner;

import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class BarcodeInputEngine {

    private boolean allowSameBarcodeRepeat = true;

    private final StringBuilder buffer = new StringBuilder();
    private long lastTime = 0;
    private long startTime = 0;

    private String lastCode = "";
    private long lastProcessedTime = 0;

    private static final long MAX_INTERVAL = 30;
    private static final long MAX_DURATION = 500;
    private static final long DUPLICATE_IGNORE_MS = 1000;

    private Consumer<String> onBarcode;

    public void setOnBarcode(Consumer<String> onBarcode) {
        this.onBarcode = onBarcode;
    }

    public void attach(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    }

    // 🔥 1. 이벤트 분리
    private void handleKeyEvent(KeyEvent e) {
        long now = System.currentTimeMillis();

        if (buffer.isEmpty()) {  startTime = now;  }
        boolean fast = isFastInput(now);
        lastTime = now;

        if (isEnter(e)) { handleEnter(now, fast);
        } else { appendInput(e); }
    }

    // 🔥 2. Enter 처리 분리
    private void handleEnter(long now, boolean fast) {

        String code = buffer.toString();
        buffer.setLength(0);
        long duration = now - startTime;
        if (!isBarcode(code, duration, fast)) { return; }
        if (shouldIgnoreDuplicate(code, now)) {  return; }
        processBarcode(code, now);
    }

    // 🔥 3. 처리 로직 분리
    private void processBarcode(String code, long now) {
        lastCode = code;
        lastProcessedTime = now;

        if (onBarcode != null) { onBarcode.accept(code); }
    }

    // 🔥 4. 작은 메서드로 분리 (가독성 + complexity 감소)
    private boolean isEnter(KeyEvent e) {
        return e.getCode() == KeyCode.ENTER;
    }

    private void appendInput(KeyEvent e) {
        buffer.append(e.getText());
    }

    private boolean isFastInput(long now) {
        return (now - lastTime) < MAX_INTERVAL;
    }

    private boolean shouldIgnoreDuplicate(String code, long now) {
        return !allowSameBarcodeRepeat && isDuplicate(code, now);
    }

    private boolean isBarcode(String code, long duration, boolean fast) {
        return code.length() >= 8
                && code.length() <= 20
                && duration < MAX_DURATION
                && fast;
    }

    private boolean isDuplicate(String code, long now) {
        return code.equals(lastCode)
                && (now - lastProcessedTime) < DUPLICATE_IGNORE_MS;
    }
}
