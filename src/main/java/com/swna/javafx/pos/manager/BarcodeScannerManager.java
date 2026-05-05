package com.swna.javafx.pos.manager;

import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;
import javafx.application.Platform;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

@Slf4j
@Component
public class BarcodeScannerManager {

    public void setup(Node sceneRoot, SafeBarcodeScanner scanner, Consumer<String> onBarcode) {
        Platform.runLater(() -> {
            var scene = sceneRoot.getScene();
            if (scene != null) {
                scanner.setScanListener(onBarcode);
                scanner.register(scene);
                log.info("[SCANNER] Registered successfully");
            } else {
                log.error("[SCANNER] Scene is null");
            }
        });
    }
}