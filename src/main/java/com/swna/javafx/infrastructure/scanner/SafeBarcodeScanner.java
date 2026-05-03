package com.swna.javafx.infrastructure.scanner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 * - ScanListener를 통해 바코드 전달
 */

 @Slf4j
@Component
public class SafeBarcodeScanner {

    /** 입력 문자 큐 */
    private final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>();

    /** 입력 중인 바코드 임시 버퍼 */
    private final StringBuilder buffer = new StringBuilder();

    /** 바코드 스캔 완료 이벤트 리스너 */
    private ScanListener listener;

    

    /**
     * 생성자
     * - 이벤트 큐 처리 Thread 시작
     */
    public SafeBarcodeScanner() {
        try {
            Thread processorThread = new Thread(this::processQueue, "BarcodeProcessorThread");
            processorThread.setDaemon(true);
            processorThread.start();
            log.info("SafeBarcodeScanner initialized and processor thread started.");
        } catch (Exception e) {
            log.error("Error initializing SafeBarcodeScanner", e);
        }
    }

    /**
     * Scene에 KeyEvent 필터 등록
     * - 이벤트 큐에 입력 문자 안전하게 저장
     */
    public void register(Scene scene) {
        try {
            scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                String character = event.getCharacter();
                if (character != null && !character.isEmpty()) {
                    boolean offered = eventQueue.offer(character);
                    if (!offered) {
                        log.warn("Failed to enqueue character: {}", character);
                    }
                }
            });
            log.info("SafeBarcodeScanner registered to scene successfully.");
        } catch (Exception e) {
            log.error("Error registering SafeBarcodeScanner to scene", e);
        }
    }

    /**
     * 큐 처리 Thread
     * - 큐에서 문자 가져와 handleCharacter 호출
     */
    private void processQueue() {
        try {
            while (true) {
                String character = eventQueue.take(); // 블록 대기
                handleCharacter(character);
            }
        } catch (InterruptedException e) {
            // 
            Thread.currentThread().interrupt();
            log.warn("Barcode processor thread interrupted", e);
        } catch (Exception e) {
            
            log.error("Unexpected error in barcode processor thread", e);
        }
    }

    /**
     * 단일 문자 처리
     * - ENTER 감지 시 버퍼를 바코드로 간주하고 listener 호출
     * - synchronized로 Thread-safe 보장
     */
   private synchronized void handleCharacter(String character) {
        try {
            if ("\r".equals(character) || "\n".equals(character)) {
                processBuffer();
            } else {
                buffer.append(character);
            }
        } catch (Exception e) {
            log.error("Error handling character input: {}", character, e);
        }
    }

    private void processBuffer() {
        String barcode = buffer.toString().trim();
        buffer.setLength(0);

        if (barcode.isEmpty()) {
            return;
        }

        if (listener != null) {
            notifyListener(barcode);
        } else {
            log.warn("ScanListener not set, barcode ignored: {}", barcode);
        }
    }

    private void notifyListener(String barcode) {
        try {
            listener.onScan(barcode);
        } catch (Exception e) {
            log.error("Error in ScanListener handling barcode: {}", barcode, e);
        }
    }

    /**
     * 바코드 스캔 완료 이벤트 리스너 등록
     */
    public void setScanListener(ScanListener listener) {
        this.listener = listener;
        log.info("ScanListener has been set.");
    }

    /**
     * 바코드 스캔 완료 이벤트 리스너 인터페이스
     */
    public interface ScanListener {
        void onScan(String barcode);
    }
}



