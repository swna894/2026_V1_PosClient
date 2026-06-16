package com.swna.javafx.pos.viewmodel.handler;

import java.util.Optional;

import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.service.ScanService;
import com.swna.javafx.pos.viewmodel.manager.CartManager;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
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
                // ScanService가 던진 ApiException이 이제 여기로 정상 도달합니다.
                log.error("[ScanHandler] 에러 감지 - 메시지: {}", error.getMessage());

                String msg = error.getMessage();
                // 미등록 상품 에러인 경우 팝업 오픈
                if (msg != null && (msg.contains("Product not found") || msg.contains("ApiException"))) {
                    showManualInputDialog(barcode);
                } else {
                    // 진짜 네트워크 다운 등 시스템 에러 발생 시
                    if (onError != null) onError.run();
                }
            }),
            () -> Platform.runLater(() -> {
                // 상품을 찾았거나, 캐시 hit 되었을 때 정상 종료 시 실행
                if (cartManager.isEmpty() && onNotFound != null) onNotFound.run();
            })
        );
    }

    /**
     * [신규 메서드] 미등록 바코드 감지 시 수동 금액 입력 창을 띄웁니다.
     */
    private void showManualInputDialog(String barcode) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("미등록 상품 등록");
        dialog.setHeaderText("시스템에 등록되지 않은 바코드입니다.\n바코드: " + barcode);
        dialog.setContentText("판매 금액(숫자만)을 입력하세요:");

        // 사용자가 입력을 마치고 OK를 누를 때까지 UI 스레드가 블록킹(대기)됩니다.
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr.trim());
                if (amount <= 0) {
                    throw new NumberFormatException("금액은 0보다 커야 합니다.");
                }
                
                // 임시 상품 생성 (기존 제공되었던 임시 상품 추가 로직 활용)
                PosItem manualItem = PosItem.createUnknowItem(barcode, amount);
                manualItem.setQty(1);
            
                cartManager.addItem(manualItem);
                
                log.info("[Scan] 미등록 바코드({}) 수동 금액(${})으로 장바구니 추가 완료", barcode, amount);
                if (onSuccess != null) onSuccess.accept(barcode);
                
            } catch (NumberFormatException e) {
                // 잘못된 금액 입력 시 경고창 처리 등을 여기에 추가할 수 있습니다.
                log.error("[Scan] 수동 금액 입력 오류: {}", amountStr);
                if (onError != null) onError.run();
            }
        });
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