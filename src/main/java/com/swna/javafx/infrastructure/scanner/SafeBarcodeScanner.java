package com.swna.javafx.infrastructure.scanner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
 * 
 * @author POS Team
 * @version 2.0
 */
@Slf4j
@Component
public class SafeBarcodeScanner {

    // =========================
    // Constants
    // =========================
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    private static final int MAX_BARCODE_LENGTH = 128;

    // =========================
    // Fields
    // =========================
    /** 입력 문자 큐 (Thread-safe) */
    private final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
    
    /** 입력 중인 바코드 임시 버퍼 */
    private final StringBuilder buffer = new StringBuilder(MAX_BARCODE_LENGTH);
    
    /** 바코드 스캔 완료 리스너 (레거시 지원) */
    private ScanListener legacyListener;
    
    /** 바코드 스캔 완료 컨슈머 (현대적 방식) */
    private Consumer<String> barcodeConsumer;
    
    /** 프로세서 스레드 참조 (정리 용도) */
    private final AtomicReference<Thread> processorThread = new AtomicReference<>();
    
    /** 스캐너 활성화 여부 */
    private volatile boolean enabled = true;

    // =========================
    // Constructors
    // =========================
    
    /**
     * 생성자
     * - 이벤트 큐 처리 Thread 시작
     */
    public SafeBarcodeScanner() {
        startProcessorThread();
        log.info("SafeBarcodeScanner initialized (version 2.0)");
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
                if (character != null && !character.isEmpty() && character.length() == 1) {
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
     * Scene 등록 후 자동으로 제거할 수 있는 버전 (WeakReference 기반 선택)
     * @deprecated 명시적 unregister 호출 권장
     */
    public void registerWithAutoCleanup(Scene scene) {
        register(scene);
        // Scene이 닫힐 때 자동 정리 로직 (선택 구현)
    }
    
    /**
     * Scene에서 KeyEvent 필터 제거
     * @param scene 등록 해제할 Scene
     */
    public void unregister(Scene scene) {
        if (scene != null) {
            // Note: JavaFX에서는 등록된 필터를 직접 제거해야 함
            // 현재 구조에서는 scene 참조를 저장해야 가능
            log.info("Unregister request received - implement if needed");
        }
    }

    // =========================
    // Public API - Listeners (Multiple ways)
    // =========================
    
    /**
     * 레거시 ScanListener 등록
     * @param listener ScanListener 구현체
     */
    public void setScanListener(ScanListener listener) {
        this.legacyListener = listener;
        this.barcodeConsumer = null; // Consumer 우선순위: 명시적 설정 시 덮어쓰지 않음
        log.info("Legacy ScanListener registered");
    }
    
    /**
     * 현대적 Consumer 방식 리스너 등록 (권장)
     * @param consumer 바코드 처리 Consumer
     */
    public void setBarcodeConsumer(Consumer<String> consumer) {
        this.barcodeConsumer = consumer;
        this.legacyListener = null; // Consumer 우선순위
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
                String character = eventQueue.take(); // 블로킹 대기
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
            // 큐 오버플로우 시 오래된 항목 제거 후 재시도
            eventQueue.poll();
            eventQueue.offer(character);
        }
    }

    // =========================
    // Private Methods - Character Processing
    // =========================
    
    /**
     * 단일 문자 처리 (Thread-safe)
     * @param character 입력 문자
     */
    private synchronized void handleCharacter(String character) {
        if (!enabled) return;
        
        try {
            // ENTER 키 감지 (스캐너 종료 신호)
            if ("\r".equals(character) || "\n".equals(character)) {
                processCompleteBarcode();
            } else {
                appendToBuffer(character);
            }
        } catch (Exception e) {
            log.error("Error handling character: {}", character, e);
            resetBuffer(); // 오류 시 버퍼 초기화
        }
    }
    
    /**
     * 버퍼에 문자 추가 (길이 제한)
     */
    private void appendToBuffer(String character) {
        synchronized (buffer) {
            if (buffer.length() >= MAX_BARCODE_LENGTH) {
                log.warn("Buffer exceeded max length ({}), resetting", MAX_BARCODE_LENGTH);
                buffer.setLength(0);
            }
            buffer.append(character);
        }
    }
    
    /**
     * 완성된 바코드 처리 및 버퍼 초기화
     */
    private void processCompleteBarcode() {
        String barcode;
        synchronized (buffer) {
            barcode = buffer.toString().trim();
            buffer.setLength(0);
        }
        
        if (barcode.isEmpty()) {
            log.debug("Empty barcode ignored");
            return;
        }
        
        if (barcode.length() > MAX_BARCODE_LENGTH) {
            log.warn("Barcode too long ({} chars), truncating", barcode.length());
            barcode = barcode.substring(0, MAX_BARCODE_LENGTH);
        }
        
        notifyListeners(barcode);
    }
    
    /**
     * 등록된 모든 리스너에게 바코드 전달
     */
    private void notifyListeners(String barcode) {
        log.debug("Barcode scanned: {}", barcode);
        
        // Consumer 우선 (현대적 방식)
        if (barcodeConsumer != null) {
            try {
                barcodeConsumer.accept(barcode);
                return;
            } catch (Exception e) {
                log.error("Consumer error for barcode: {}", barcode, e);
            }
        }
        
        // Fallback: 레거시 리스너
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

    // =========================
    // Inner Interfaces
    // =========================
    
    /**
     * 바코드 스캔 완료 이벤트 리스너 인터페이스 (레거시 호환)
     */
    @FunctionalInterface
    public interface ScanListener {
        void onScan(String barcode);
    }
}