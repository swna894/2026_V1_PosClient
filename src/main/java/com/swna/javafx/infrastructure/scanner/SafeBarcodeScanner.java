package com.swna.javafx.infrastructure.scanner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * SafeBarcodeScanner
 * -------------------
 * JavaFX KeyEvent 기반 바코드 스캔 처리 컴포넌트.
 * - KeyEvent 입력 문자 큐에 저장
 * - 큐 처리 전용 Thread에서 ENTER 감지 시 바코드 완성
 * - ScanListener 또는 Consumer를 통해 바코드 전달
 * - 최소/최대 길이 검증 지원
 * 
 * @author POS Team
 * @version 2.1
 */
@Slf4j
@Component
@Scope("prototype")
public class SafeBarcodeScanner {

    // =========================
    // Constants
    // =========================
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    private static final int DEFAULT_MAX_BARCODE_LENGTH = 128;
    private static final int DEFAULT_MIN_BARCODE_LENGTH = 6;
    private static final int DEFAULT_WARNING_THRESHOLD = 100;

    // =========================
    // Fields
    // =========================
    /** 입력 문자 큐 (Thread-safe) */
    private final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
    
    /** 입력 중인 바코드 임시 버퍼 */
    private final StringBuilder buffer = new StringBuilder(DEFAULT_MAX_BARCODE_LENGTH);
    
    /** 바코드 스캔 완료 리스너 (레거시 지원) */
    private ScanListener legacyListener;
    
    /** 바코드 스캔 완료 컨슈머 (현대적 방식) */
    private Consumer<String> barcodeConsumer;
    
    /** 프로세서 스레드 참조 (정리 용도) */
    private final AtomicReference<Thread> processorThread = new AtomicReference<>();
    
    /** 스캐너 활성화 여부 */
    private volatile boolean enabled = true;
    
    /** 최대 바코드 길이 */
    private volatile int maxBarcodeLength = DEFAULT_MAX_BARCODE_LENGTH;
    
    /** 최소 바코드 길이 */
    private volatile int minBarcodeLength = DEFAULT_MIN_BARCODE_LENGTH;
    
    /** 경고 로그 임계값 */
    private volatile int warningThreshold = DEFAULT_WARNING_THRESHOLD;

    // =========================
    // Constructors
    // =========================
    
    /**
     * 생성자
     * - 이벤트 큐 처리 Thread 시작
     */
    public SafeBarcodeScanner() {
        startProcessorThread();
        log.info("SafeBarcodeScanner initialized (version 2.1) - min length: {}, max length: {}", 
            minBarcodeLength, maxBarcodeLength);
    }

    // =========================
    // Public API - Registration
    // =========================
    
    /**
     * Scene에 KeyEvent 필터 등록
     * @param scene 등록할 JavaFX Scene
     * @throws IllegalArgumentException scene이 null인 경우
     */
    public void register(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("Scene cannot be null");
        }
        
        try {
            scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                if (!enabled) return;
                
                String character = event.getCharacter();
                if (isValidCharacter(character)) {
                    enqueueCharacter(character);
                }
            });
            log.info("SafeBarcodeScanner registered to scene successfully");
        } catch (Exception e) {
            log.error("Failed to register SafeBarcodeScanner to scene", e);
            throw new RuntimeException("Scanner registration failed", e);
        }
    }
    
    /**
     * Scene 등록 후 자동으로 제거할 수 있는 버전
     * @deprecated 명시적 unregister 호출 권장
     */
    @Deprecated
    public void registerWithAutoCleanup(Scene scene) {
        register(scene);
    }
    
    /**
     * Scene에서 KeyEvent 필터 제거
     * @param scene 등록 해제할 Scene
     */
    public void unregister(Scene scene) {
        if (scene != null) {
            log.info("Unregister request received - implement if needed");
        }
    }

    // =========================
    // Public API - Listeners
    // =========================
    
    /**
     * 레거시 ScanListener 등록
     * @param listener ScanListener 구현체
     */
    public void setScanListener(ScanListener listener) {
        this.legacyListener = listener;
        this.barcodeConsumer = null;
        log.info("Legacy ScanListener registered");
    }
    
    /**
     * 현대적 Consumer 방식 리스너 등록 (권장)
     * @param consumer 바코드 처리 Consumer
     */
    public void setBarcodeConsumer(Consumer<String> consumer) {
        this.barcodeConsumer = consumer;
        this.legacyListener = null;
        log.info("Barcode Consumer registered");
    }
    
    /**
     * 편의 메서드: Consumer를 ScanListener로 변환하여 등록
     * @param consumer 바코드 처리 Consumer
     */
    public void setScanListener(Consumer<String> consumer) {
        setBarcodeConsumer(consumer);
    }
    
    /**
     * 리스너 제거
     */
    public void clearListener() {
        this.legacyListener = null;
        this.barcodeConsumer = null;
        log.info("All listeners cleared");
    }

    // =========================
    // Public API - Configuration
    // =========================
    
    /**
     * 최소 바코드 길이 설정 (짧은 입력 무시)
     * @param length 최소 길이 (1 이상, maxLength 이하)
     * @throws IllegalArgumentException 유효하지 않은 길이인 경우
     */
    public void setMinBarcodeLength(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Minimum length must be at least 1");
        }
        if (length > maxBarcodeLength) {
            throw new IllegalArgumentException(
                String.format("Minimum length (%d) cannot exceed maximum length (%d)", 
                    length, maxBarcodeLength));
        }
        
        this.minBarcodeLength = length;
        log.info("Minimum barcode length set to: {}", length);
    }
    
    /**
     * 최대 바코드 길이 설정
     * @param length 최대 길이 (minLength 이상)
     * @throws IllegalArgumentException 유효하지 않은 길이인 경우
     */
    public void setMaxBarcodeLength(int length) {
        if (length < minBarcodeLength) {
            throw new IllegalArgumentException(
                String.format("Maximum length (%d) cannot be less than minimum length (%d)", 
                    length, minBarcodeLength));
        }
        
        this.maxBarcodeLength = length;
        log.info("Maximum barcode length set to: {}", length);
        
        // 버퍼 용량 조정
        synchronized (buffer) {
            if (buffer.capacity() < maxBarcodeLength) {
                buffer.ensureCapacity(maxBarcodeLength);
            }
        }
    }
    
    /**
     * 현재 최소 바코드 길이 반환
     */
    public int getMinBarcodeLength() {
        return minBarcodeLength;
    }
    
    /**
     * 현재 최대 바코드 길이 반환
     */
    public int getMaxBarcodeLength() {
        return maxBarcodeLength;
    }
    
    /**
     * 경고 로그 임계값 설정 (디버깅용)
     */
    public void setWarningThreshold(int threshold) {
        this.warningThreshold = threshold;
    }

    // =========================
    // Public API - Control
    // =========================
    
    /**
     * 스캐너 일시 중지
     */
    public void disable() {
        this.enabled = false;
        log.info("Barcode scanner disabled");
    }
    
    /**
     * 스캐너 재개
     */
    public void enable() {
        this.enabled = true;
        log.info("Barcode scanner enabled");
    }
    
    /**
     * 현재 버퍼 내용 수동 초기화 (오작동 시)
     */
    public void resetBuffer() {
        synchronized (buffer) {
            buffer.setLength(0);
        }
        log.debug("Buffer manually reset");
    }
    
    /**
     * 스캐너 완전 종료 (리소스 정리)
     */
    public void shutdown() {
        enabled = false;
        Thread thread = processorThread.get();
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        eventQueue.clear();
        clearListener();
        log.info("Barcode scanner shutdown complete");
    }

    // =========================
    // Private Methods - Queue Processing
    // =========================
    
    /**
     * 프로세서 스레드 시작
     */
    private void startProcessorThread() {
        Thread thread = new Thread(this::processQueue, "BarcodeProcessorThread");
        thread.setDaemon(true);
        thread.start();
        processorThread.set(thread);
        log.debug("Processor thread started: {}", thread.getName());
    }
    
    /**
     * 큐 처리 Thread 메인 루프
     */
    private void processQueue() {
        try {
            while (!Thread.currentThread().isInterrupted() && enabled) {
                String character = eventQueue.take();
                handleCharacter(character);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Barcode processor thread interrupted");
        } catch (Exception e) {
            log.error("Unexpected error in barcode processor thread", e);
        } finally {
            log.debug("Processor thread exiting");
        }
    }
    
    /**
     * 문자를 큐에 안전하게 추가
     */
    private void enqueueCharacter(String character) {
        boolean offered = eventQueue.offer(character);
        if (!offered) {
            log.warn("Event queue full, dropping character: {}", character);
            eventQueue.poll();
            eventQueue.offer(character);
        }
        
        // 큐 크기 모니터링 (경고)
        int queueSize = eventQueue.size();
        if (queueSize > warningThreshold) {
            log.warn("Event queue size threshold exceeded: {}", queueSize);
        }
    }

    // =========================
    // Private Methods - Character Processing
    // =========================
    
    /**
     * 유효한 문자인지 확인
     */
    private boolean isValidCharacter(String character) {
        if (character == null || character.isEmpty()) {
            return false;
        }
        // 단일 문자만 처리
        return character.length() == 1;
    }
    
    /**
     * 단일 문자 처리 (Thread-safe)
     * @param character 입력 문자
     */
    private synchronized void handleCharacter(String character) {
        if (!enabled) return;
        
        try {
            if (isEnterKey(character)) {
                processCompleteBarcode();
            } else if (isValidBarcodeCharacter(character)) {
                appendToBuffer(character);
            } else {
                log.debug("Ignoring invalid barcode character: '{}' (code: {})", 
                    character, (int) character.charAt(0));
            }
        } catch (Exception e) {
            log.error("Error handling character: {}", character, e);
            resetBuffer();
        }
    }
    
    /**
     * Enter 키 확인
     */
    private boolean isEnterKey(String character) {
        return "\r".equals(character) || "\n".equals(character);
    }
    
    /**
     * 바코드에 허용되는 문자인지 확인
     * (선택적: 필요시 확장 가능)
     */
    private boolean isValidBarcodeCharacter(String character) {
        char c = character.charAt(0);
        // 기본 ASCII 범위: 숫자, 영문자, 일부 특수문자
        return (c >= '0' && c <= '9') ||
               (c >= 'A' && c <= 'Z') ||
               (c >= 'a' && c <= 'z') ||
               c == '-' || c == '_' || c == '.' || c == '/';
    }
    
    /**
     * 버퍼에 문자 추가 (길이 제한)
     */
    private void appendToBuffer(String character) {
        synchronized (buffer) {
            if (buffer.length() >= maxBarcodeLength) {
                log.warn("Buffer exceeded max length ({}), resetting", maxBarcodeLength);
                buffer.setLength(0);
            }
            buffer.append(character);
        }
    }
    
    /**
     * 완성된 바코드 처리 및 버퍼 초기화
     */
    private void processCompleteBarcode() {
        String barcode = extractAndClearBuffer();
        
        if (barcode.isEmpty()) {
            log.debug("Empty barcode ignored");
            return;
        }
        
        BarcodeValidationResult validationResult = validateBarcode(barcode);
        
        if (!validationResult.isValid()) {
            log.debug("Barcode validation failed: {} - {}", 
                validationResult.getReason(), barcode);
            return;
        }
        
        if (validationResult.isTruncated()) {
            barcode = validationResult.getTruncatedBarcode();
        }
        
        notifyListeners(barcode);
    }
    
    /**
     * 버퍼에서 바코드 추출 후 초기화
     */
    private String extractAndClearBuffer() {
        synchronized (buffer) {
            String barcode = buffer.toString().trim();
            buffer.setLength(0);
            return barcode;
        }
    }
    
    /**
     * 바코드 검증 수행
     */
    private BarcodeValidationResult validateBarcode(String barcode) {
        int length = barcode.length();
        
        // 최소 길이 검증
        if (length < minBarcodeLength) {
            return BarcodeValidationResult.invalid(
                String.format("length %d < minimum %d", length, minBarcodeLength));
        }
        
        // 최대 길이 검증
        if (length > maxBarcodeLength) {
            log.warn("Barcode too long ({} chars), truncating to {}: {}", 
                length, maxBarcodeLength, barcode);
            String truncated = barcode.substring(0, maxBarcodeLength);
            return BarcodeValidationResult.truncated(truncated);
        }
        
        return BarcodeValidationResult.valid();
    }
    
    /**
     * 등록된 모든 리스너에게 바코드 전달
     */
    private void notifyListeners(String barcode) {
        log.info("Barcode scanned: {} (length: {})", barcode, barcode.length());
        
        if (barcodeConsumer != null) {
            try {
                barcodeConsumer.accept(barcode);
                return;
            } catch (Exception e) {
                log.error("Consumer error for barcode: {}", barcode, e);
            }
        }
        
        if (legacyListener != null) {
            try {
                legacyListener.onScan(barcode);
            } catch (Exception e) {
                log.error("ScanListener error for barcode: {}", barcode, e);
            }
        } else {
            log.warn("No listener registered, barcode ignored: {}", barcode);
        }
    }

    // =========================
    // Public API - Status & Info
    // =========================
    
    /**
     * 스캐너 활성화 상태 확인
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 현재 버퍼 내용 확인 (디버깅용)
     */
    public String getCurrentBuffer() {
        synchronized (buffer) {
            return buffer.toString();
        }
    }
    
    /**
     * 큐에 대기 중인 문자 수 (디버깅용)
     */
    public int getQueueSize() {
        return eventQueue.size();
    }
    
    /**
     * 설정 정보 반환 (디버깅용)
     */
    public String getConfigurationInfo() {
        return String.format(
            "SafeBarcodeScanner[enabled=%s, minLen=%d, maxLen=%d, queueSize=%d, bufferLen=%d]",
            enabled, minBarcodeLength, maxBarcodeLength, getQueueSize(), getCurrentBuffer().length());
    }

    // =========================
    // Inner Classes
    // =========================
    
    /**
     * 바코드 스캔 완료 이벤트 리스너 인터페이스 (레거시 호환)
     */
    @FunctionalInterface
    public interface ScanListener {
        void onScan(String barcode);
    }
    
    /**
     * 바코드 검증 결과를 담는 내부 클래스
     */
    private static class BarcodeValidationResult {
        private final boolean valid;
        private final boolean truncated;
        private final String truncatedBarcode;
        private final String reason;
        
        private BarcodeValidationResult(boolean valid, boolean truncated, 
                                        String truncatedBarcode, String reason) {
            this.valid = valid;
            this.truncated = truncated;
            this.truncatedBarcode = truncatedBarcode;
            this.reason = reason;
        }
        
        public static BarcodeValidationResult valid() {
            return new BarcodeValidationResult(true, false, null, null);
        }
        
        public static BarcodeValidationResult invalid(String reason) {
            return new BarcodeValidationResult(false, false, null, reason);
        }
        
        public static BarcodeValidationResult truncated(String barcode) {
            return new BarcodeValidationResult(true, true, barcode, null);
        }
        
        public boolean isValid() { return valid; }
        public boolean isTruncated() { return truncated; }
        public String getTruncatedBarcode() { return truncatedBarcode; }
        public String getReason() { return reason; }
    }
}