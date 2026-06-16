package com.swna.javafx.pos.viewmodel.handler;

import java.util.Optional;

import com.swna.javafx.pos.manager.PosDialogManager;
import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.service.ScanService;
import com.swna.javafx.pos.viewmodel.manager.CartManager;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScanHandler {
    
    private final ScanService posService;
    private final CartManager cartManager;
    private final PosDialogManager posDialogManager;

    public static final String QUICK_ITEM_PREFIX = "QUICK";
    
    // 상태 콜백 (ViewModel과 통신)
    private Runnable onScanning;
    private java.util.function.Consumer<String> onSuccess;
    private Runnable onNotFound;
    private Runnable onError;
    
    public ScanHandler(ScanService posService, CartManager cartManager, PosDialogManager posDialogManager) {
        this.posService = posService;
        this.cartManager = cartManager;
        this.posDialogManager = posDialogManager;
    }
    
    public void setCallbacks(Runnable onScanning, 
                             java.util.function.Consumer<String> onSuccess,
                             Runnable onNotFound, 
                             Runnable onError) {
        this.onScanning = onScanning;
        this.onSuccess = onSuccess;
        this.onNotFound = onNotFound;
        this.onError = onError;
    }
    
    public void scan(String barcode) {
        if (barcode == null || barcode.isBlank()) return;
        
        if (onScanning != null) onScanning.run();
        
        posService.scan(barcode).subscribe(
            item -> Platform.runLater(() -> {
                addOrUpdateItem(item);
                if (onSuccess != null) onSuccess.accept(barcode);
            }),
            error -> Platform.runLater(() -> {
                log.error("[ScanHandler] 에러 감지: {}", error.getMessage());
                String msg = error.getMessage();
                
                if (msg != null && (msg.contains("Product not found") || msg.contains("ApiException"))) {
                    // PosDialogManager를 통해 다이얼로그 호출
                    posDialogManager.showManualRegisterDialog(barcode, amount -> {
                        PosItem manualItem = PosItem.createUnknowItem(barcode, amount);
                        manualItem.setQty(1);
                        cartManager.addItem(manualItem);
                        
                        log.info("[Scan] 미등록 바코드({}) 수동 금액(${}) 추가", barcode, amount);
                        if (onSuccess != null) onSuccess.accept(barcode);
                    });
                } else if (onError != null) {
                    onError.run();
                }
            }),
            () -> Platform.runLater(() -> {
                // 상품을 찾았거나, 캐시 hit 되었을 때 정상 종료 시 실행
                if (cartManager.isEmpty() && onNotFound != null) onNotFound.run();
            })
        );
    }

    public void addQuickAmountItem(double amount) {
        Optional<PosItem> existing = cartManager.findQuickItemByAmount(amount);
        
        if (existing.isPresent()) {
            cartManager.increaseQty(existing.get());
        } else {
            PosItem newItem = PosItem.createQuickItem(QUICK_ITEM_PREFIX, amount);
            newItem.setQty(1);
            cartManager.addItem(newItem);
        }
        
        log.debug("[Scan] Quick item added: ${}", amount);
    }
    
    private void addOrUpdateItem(PosItem newItem) {
        Optional<PosItem> existing = cartManager.findByBarcode(newItem.getBarcode());
        
        if (existing.isPresent()) {
            cartManager.increaseQty(existing.get());
        } else {
            newItem.increaseQty();
            cartManager.addItem(newItem);
        }
    }
}