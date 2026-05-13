package com.swna.javafx.pos.viewmodel.handler;

import java.util.Optional;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.service.ScanService;
import com.swna.javafx.pos.viewmodel.manager.CartManager;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScanHandler {
    
    private final ScanService posService;
    private final CartManager cartManager;

    public static final String QUICK_ITEM_PREFIX = "QUICK";
    
    // 상태 콜백 (ViewModel과 통신)
    private Runnable onScanning;
    private java.util.function.Consumer<String> onSuccess;
    private Runnable onNotFound;
    private Runnable onError;
    
    public ScanHandler(ScanService posService, CartManager cartManager) {
        this.posService = posService;
        this.cartManager = cartManager;
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
                if (onError != null) onError.run();
                log.error("[Scan] Error: {}", error.getMessage());
            }),
            () -> Platform.runLater(() -> {
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