package com.swna.javafx.pos.viewmodel;

import static com.swna.javafx.pos.viewmodel.PosViewModelConstants.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.api.PosApiService;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.service.ScanService;
import com.swna.javafx.pos.viewmodel.handler.ScanHandler;
import com.swna.javafx.pos.viewmodel.manager.CartManager;
import com.swna.javafx.pos.viewmodel.manager.DiscountManager;
import com.swna.javafx.pos.viewmodel.manager.HoldManager;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PosViewModel {

    // ========== Managers ==========
    private final ApplicationEventPublisher eventPublisher;
    private final CartManager cartManager;
    private final DiscountManager discountManager;
    private final HoldManager holdManager;
    private final ScanHandler scanHandler;
    private final PosProcessor posProcessor;
    
    // ========== UI 상태 ==========
    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);
    
    public PosViewModel(ApplicationEventPublisher eventPublisher, 
                        ScanService posService, 
                        PosApiService posApiService) {
        this.eventPublisher = eventPublisher;
        this.cartManager = new CartManager();
        this.discountManager = new DiscountManager(cartManager);
        this.holdManager = new HoldManager(cartManager);
        this.scanHandler = new ScanHandler(posService, cartManager);
        this.posProcessor = new PosProcessor(cartManager, posApiService);
        
        setupScanCallbacks();
    }
    
    private void setupScanCallbacks() {
        scanHandler.setCallbacks(
            () -> scanStatus.set(STATUS_SCANNING),
            (barcode) -> {
                scannedCode.set(barcode);
                scanStatus.set(String.format(STATUS_SCAN_SUCCESS, barcode));
            },
            () -> scanStatus.set(STATUS_ITEM_NOT_FOUND),
            () -> scanStatus.set(STATUS_SEARCH_FAILED)
        );
    }
    
    // ========== CartManager Delegate ==========
    public ObservableList<PosItem> getPosItems() { 
        return cartManager.getItems(); 
    }
    
    public DoubleProperty totalAmountProperty() { 
        return cartManager.totalAmountProperty(); 
    }
    
    public DoubleProperty discountProperty() { 
        return cartManager.totalDiscountProperty(); 
    }
    
    public IntegerProperty totalQtyProperty() { 
        return cartManager.totalQtyProperty(); 
    }
    
    public ObjectProperty<PosItem> selectedItemProperty() { 
        return cartManager.selectedItemProperty(); 
    }
    
    public void increaseQty(PosItem item) { 
        cartManager.increaseQty(item); 
    }
    
    public void decreaseQty(PosItem item) { 
        cartManager.decreaseQty(item); 
    }
    
    public void removeItem(PosItem item) { 
        cartManager.removeItem(item); 
    }
    
    public void clear() { 
        cartManager.clear();
        scannedCode.set("");
    }
    
    public boolean hasItems() { 
        return !cartManager.isEmpty(); 
    }
    
    // ========== DiscountManager Delegate ==========
    public void discountItemPrice(PosItem item, double newPrice) { 
        discountManager.discountItemPrice(item, newPrice); 
    }
    
    public void changeItemPrice(PosItem item, double newPrice) { 
        discountManager.changeItemPrice(item, newPrice); 
    }
    
    public void applyDiscountPercent(double percent) { 
        discountManager.applyPercentToSelected(percent); 
    }
    
    public void applyDiscountAmount(double amount) { 
        discountManager.applyAmountToSelected(amount); 
    }
    
    public void applyUnitDiscount(double unitDiscountAmount) { 
        discountManager.applyUnitDiscountToSelected(unitDiscountAmount); 
    }
    
    // ========== Volume Discount Methods ==========

    /**
     * 금액 기준 볼륨 할인 적용
     */
    public void applyVolumeDiscountByAmount(BigDecimal amountAfterDC, Consumer<Boolean> callback) {
        try {
            BigDecimal currentTotal = BigDecimal.valueOf(totalAmountProperty().get());
            
            if (currentTotal.compareTo(BigDecimal.ZERO) <= 0) {
                callback.accept(false);
                return;
            }
            
            if (amountAfterDC.compareTo(currentTotal) >= 0) {
                callback.accept(false);
                return;
            }
            
            // 할인율 계산 후 DiscountManager에 위임
            BigDecimal discountPercent = currentTotal.subtract(amountAfterDC)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(currentTotal, 2, BigDecimal.ROUND_HALF_UP);
            
            discountManager.applyPercentToAll(discountPercent.doubleValue());
            
            callback.accept(true);
        } catch (Exception e) {
            log.error("Failed to apply amount discount", e);
            callback.accept(false);
        }
    }

    /**
     * 퍼센트 기준 볼륨 할인 적용
     */
    public void applyVolumeDiscountByPercent(BigDecimal percent, Consumer<Boolean> callback) {
        try {
            BigDecimal currentTotal = BigDecimal.valueOf(totalAmountProperty().get());
            
            if (currentTotal.compareTo(BigDecimal.ZERO) <= 0) {
                callback.accept(false);
                return;
            }
            
            if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
                callback.accept(false);
                return;
            }
            
            discountManager.applyPercentToAll(percent.doubleValue());
            
            callback.accept(true);
        } catch (Exception e) {
            log.error("Failed to apply percent discount", e);
            callback.accept(false);
        }
    }

    // ========== HoldManager Delegate ==========
    public void holdCart() {
        if (holdManager.save()) {
            scanStatus.set(STATUS_HOLD_SAVED);
        } else {
            scanStatus.set(STATUS_HOLD_NO_ITEMS);
        }
    }
    
    public void resumeCart() {
        if (holdManager.resume()) {
            scanStatus.set(STATUS_HOLD_RESUMED);
        } else {
            scanStatus.set(STATUS_HOLD_NO_CART);
        }
    }
    
    public boolean hasHoldItems() { 
        return holdManager.hasHoldItems(); 
    }
    
    // ========== ScanHandler Delegate ==========
    public void scan(String barcode) { 
        scanHandler.scan(barcode); 
    }
    
    public void addQuickAmountItem(double amount) { 
        scanHandler.addQuickAmountItem(amount);
        scanStatus.set(String.format(STATUS_QUICK_ADD, amount));
    }
    
    // ========== UI Properties ==========
    public StringProperty scannedCodeProperty() { 
        return scannedCode; 
    }
    
    public StringProperty scanStatusProperty() { 
        return scanStatus; 
    }
    
    // ========== 결제 관련 Public API ==========
    
    /**
     * 할인 적용된 최종 결제 금액
     */
    public BigDecimal getTotalAfterDiscount() {
        return posProcessor.getTotalAfterDiscount();
    }
    
    // ========== 현금 결제 ==========
    
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash) {
        processCashPayment(totalAmount, receivedCash, (Consumer<Boolean>) null);
    }
    
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash, 
                                   Consumer<Boolean> onComplete) {
        posProcessor.processCashPayment(totalAmount, receivedCash, 
            processed -> handleProcessedPayment(processed, onComplete),
            createResultHandler()
        );
    }
    
    // ========== 현금인출 결제 (카드번호 포함) ==========
    
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount) {
        processCashoutPayment(cashoutAmount, totalCardAmount, null, null);
    }
    
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      String cardNumber) {
        processCashoutPayment(cashoutAmount, totalCardAmount, cardNumber, null);
    }
    
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      Consumer<Boolean> onComplete) {
        processCashoutPayment(cashoutAmount, totalCardAmount, null, onComplete);
    }
    
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      String cardNumber, Consumer<Boolean> onComplete) {
        posProcessor.processCashoutPayment(cashoutAmount, totalCardAmount, cardNumber,
            processed -> handleProcessedPayment(processed, onComplete),
            createResultHandler()
        );
    }
    
    // ========== 순수 카드 결제 ==========
    
    public void processCreditPayment(BigDecimal cardAmount) {
        processCreditPayment(cardAmount, null, null);
    }
    
    public void processCreditPayment(BigDecimal cardAmount, String cardNumber) {
        processCreditPayment(cardAmount, cardNumber, null);
    }
    
    public void processCreditPayment(BigDecimal cardAmount, Consumer<Boolean> onComplete) {
        processCreditPayment(cardAmount, null, onComplete);
    }
    
    public void processCreditPayment(BigDecimal cardAmount, String cardNumber, 
                                     Consumer<Boolean> onComplete) {
        posProcessor.processCreditPayment(cardAmount, cardNumber,
            processed -> handleProcessedPayment(processed, onComplete),
            createResultHandler()
        );
    }
    
    // ========== 혼합 결제 (현금 + 카드) ==========
    
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart) {
        processMixedPayment(cashPart, creditPart, null, null);
    }
    
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart, String cardNumber) {
        processMixedPayment(cashPart, creditPart, cardNumber, null);
    }
    
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart, Consumer<Boolean> onComplete) {
        processMixedPayment(cashPart, creditPart, null, onComplete);
    }
    
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart,  String cardNumber, Consumer<Boolean> onComplete) {
        posProcessor.processMixedPayment(cashPart, creditPart, cardNumber,
            processed -> handleProcessedPayment(processed, onComplete),
            createResultHandler()
        );
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * ProcessedPayment 결과 처리
     */
    private void handleProcessedPayment(PosProcessor.ProcessedPayment processed, 
                                        Consumer<Boolean> onComplete) {
        List<PosItem> itemsSnapshot = List.copyOf(getPosItems());

        if (processed.success()) {
            // SaleRequest를 이벤트로 발행
            eventPublisher.publishEvent(new PaymentSuccessEvent(
                this, 
                processed.saleRequest(), 
                processed.paymentResult(),
                itemsSnapshot
            ));
            
            updateUIBeforeComplete(processed);
            
            if (onComplete != null) {
                onComplete.accept(true);
            }
        } else {
            // 실패 시 이미 resultHandler에서 UI 업데이트 완료
            if (onComplete != null) {
                onComplete.accept(false);
            }
        }
    }
    
    private void updateUIBeforeComplete(PosProcessor.ProcessedPayment processed) {
        Platform.runLater(() -> {
            scanStatus.set(processed.saleRequest().getPaymentTypeCode() + " " + STATUS_PAYMENT_SUCCESS   );
            clear();
        });
    }

    /**
     * PaymentResultHandler 생성 (UI 업데이트용)
     */
    private PosProcessor.PaymentResultHandler createResultHandler() {
        return new PosProcessor.PaymentResultHandler() {
            @Override
            public void onFailure(String message) {
                Platform.runLater(() -> {
                    scanStatus.set(STATUS_PAYMENT_FAIL + ": " + message);
                });
            }
            
            @Override
            public void handleSuccess(String successMessage) {
                log.info("[VM] {}", successMessage);
            }
            
            @Override
            public void handleFailure(String errorMessage) {
                Platform.runLater(() -> {
                    scanStatus.set(STATUS_PAYMENT_FAIL + ": " + errorMessage);
                });
            }
        };
    }
    
    // ========== Event Listeners ==========
    
    @EventListener
    public void handlePrintFailure(PrintFailureEvent event) {
        Platform.runLater(() -> {
            scanStatus.set(event.errorMessage()); 
            log.error("[PRINT ERROR] Receipt No: {}, Msg: {}", event.receiptNo(), event.errorMessage());
        });
    }
}