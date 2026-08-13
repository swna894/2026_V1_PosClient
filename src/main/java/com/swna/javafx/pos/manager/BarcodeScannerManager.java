package com.swna.javafx.pos.manager;

import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BarcodeScannerManager {

    public void setup(Node sceneRoot, SafeBarcodeScanner scanner, Consumer<String> onBarcode) {
        Platform.runLater(() -> {
            var scene = sceneRoot.getScene();
            if (scene != null) {
                // 1. 바코드 스캐너 등록
                scanner.setScanListener(onBarcode);
                scanner.register(scene);

                // 2. Scene 키 이벤트 가로채기 (포커스 자동 이동)
                scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                    // 현재 포커스를 가진 요소 확인
                    Node focusOwner = scene.getFocusOwner();

                    // 사용자가 'TextField' 등에 직접 타이핑 중이 아닐 때만 TableView로 포커스 이동
                    boolean isUserTypingInInput = focusOwner instanceof TextInputControl;

                    if (!sceneRoot.isFocused() && !isUserTypingInInput) {
                        Platform.runLater(sceneRoot::requestFocus);
                    }
                });

                log.info("[SCANNER] Registered successfully with auto-focus filter");
            } else {
                log.error("[SCANNER] Scene is null");
            }
        });
    }
}