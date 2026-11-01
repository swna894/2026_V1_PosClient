package com.swna.javafx.pos.manager;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 화면별 바코드 스캐너 연결 관리.
 *
 * <p>이 매니저는 싱글톤이므로 pause 상태를 매니저 자체에 두지 않고,
 * {@link #setup} 호출마다 반환되는 {@link ScanHandle}에 둡니다.
 * (POS 화면과 언패킹 화면이 서로의 일시 정지 상태에 영향을 받지 않도록)</p>
 *
 * <p>기존 호출부({@code scannerManager.setup(...)})는 반환값을 무시해도 그대로 컴파일/동작합니다.</p>
 */
@Slf4j
@Component
public class BarcodeScannerManager {

    /**
     * @param focusTarget 스캔 입력을 받을 노드 (예: TableView). 포커스 복원 대상이기도 함
     * @param scanner     스캐너
     * @param onBarcode   스캔 완료 콜백 (pause 중에는 호출되지 않음)
     * @return 일시 정지/재개/해제를 제어하는 핸들
     */
    public ScanHandle setup(Node focusTarget, SafeBarcodeScanner scanner, Consumer<String> onBarcode) {
        ScanHandle handle = new ScanHandle(focusTarget);

        Platform.runLater(() -> {
            Scene scene = focusTarget.getScene();
            if (scene != null) {
                attach(scene, focusTarget, scanner, onBarcode, handle);
                return;
            }

            // initialize() 시점에는 아직 Scene에 붙지 않은 경우가 있으므로 붙을 때까지 대기
            log.debug("[SCANNER] Scene not ready yet - waiting for scene attach");
            focusTarget.sceneProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Scene> obs, Scene oldScene, Scene newScene) {
                    if (newScene != null) {
                        obs.removeListener(this);
                        attach(newScene, focusTarget, scanner, onBarcode, handle);
                    }
                }
            });
        });

        return handle;
    }

    private void attach(Scene scene, Node focusTarget, SafeBarcodeScanner scanner,
                        Consumer<String> onBarcode, ScanHandle handle) {

        // 1. 스캐너 등록 — pause/dispose 상태면 콜백을 전달하지 않음
        // 람다를 직접 넘기면 setScanListener(Consumer<String>) / setScanListener(ScanListener)
        // 오버로드가 모호해지므로, 타입을 명시한 변수로 넘긴다.
        Consumer<String> guardedListener = code -> {
            if (!handle.isActive()) {
                log.debug("[SCANNER] Scan dropped (paused or disposed): {}", code);
                return;
            }
            onBarcode.accept(code);
        };
        scanner.setScanListener(guardedListener);
        scanner.register(scene);

        // 2. 키 입력 시 포커스 자동 이동 (사용자가 입력 컨트롤에 타이핑 중이면 제외)
        EventHandler<KeyEvent> focusFilter = event -> {
            if (!handle.isActive()) return;

            boolean isUserTypingInInput = scene.getFocusOwner() instanceof TextInputControl;
            if (!focusTarget.isFocused() && !isUserTypingInInput) {
                Platform.runLater(focusTarget::requestFocus);
            }
        };
        scene.addEventFilter(KeyEvent.KEY_TYPED, focusFilter);

        // dispose 시 필터 제거
        handle.setDisposer(() -> scene.removeEventFilter(KeyEvent.KEY_TYPED, focusFilter));

        log.info("[SCANNER] Registered successfully with auto-focus filter");
    }

    // ------------------------------------------------------------------

    /**
     * 화면 하나에 대한 스캔 제어 핸들. pause/resume은 어느 스레드에서 호출해도 됩니다.
     */
    public static final class ScanHandle {

        private final Node focusTarget;
        private volatile boolean paused;
        private volatile boolean disposed;
        private Runnable disposer = () -> { };

        private ScanHandle(Node focusTarget) {
            this.focusTarget = focusTarget;
        }

        private void setDisposer(Runnable disposer) {
            this.disposer = disposer;
            if (disposed) {
                disposer.run();
            }
        }

        /** 스캔 입력 일시 정지. 정지 중 들어온 스캔은 버려집니다. */
        public void pause() {
            paused = true;
        }

        /** 스캔 재개 + 포커스 대상 노드로 포커스 복원. */
        public void resume() {
            paused = false;
            Platform.runLater(focusTarget::requestFocus);
        }

        /** 화면을 떠날 때 호출: 키 필터 제거, 이후 스캔 무시. */
        public void dispose() {
            disposed = true;
            Platform.runLater(disposer);
        }

        public boolean isPaused() {
            return paused;
        }

        public boolean isActive() {
            return !paused && !disposed;
        }
    }
}
