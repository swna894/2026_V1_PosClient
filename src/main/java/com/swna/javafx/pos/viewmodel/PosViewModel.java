// PosViewModel.java (리팩토링 버전)
package com.swna.javafx.pos.viewmodel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.service.PaymentService;
import com.swna.javafx.pos.service.PosService;
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

import static com.swna.javafx.pos.viewmodel.PosViewModelConstants.*;

@Slf4j
@Component
public class PosViewModel {

    // ========== Managers ==========
    private final ApplicationEventPublisher eventPublisher;
    private final CartManager cartManager;
    private final DiscountManager discountManager;
    private final HoldManager holdManager;
    private final ScanHandler scanHandler;
    private final PaymentProcessor paymentProcessor;
    
    // ========== UI 상태 ==========
    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);
    
    public PosViewModel(ApplicationEventPublisher eventPublisher, 
                        PosService posService, 
                        PaymentService paymentService) {
        this.eventPublisher = eventPublisher;
        this.cartManager = new CartManager();
        this.discountManager = new DiscountManager(cartManager);
        this.holdManager = new HoldManager(cartManager);
        this.scanHandler = new ScanHandler(posService, cartManager);
        this.paymentProcessor = new PaymentProcessor(cartManager, paymentService);
        
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
    public ObservableList<PosItem> getPosItems() { return cartManager.getItems(); }
    public DoubleProperty totalAmountProperty() { return cartManager.totalAmountProperty(); }
    public DoubleProperty discountProperty() { return cartManager.totalDiscountProperty(); }
    public IntegerProperty totalQtyProperty() { return cartManager.totalQtyProperty(); }
    public ObjectProperty<PosItem> selectedItemProperty() { return cartManager.selectedItemProperty(); }
    
    public void increaseQty(PosItem item) { cartManager.increaseQty(item); }
    public void decreaseQty(PosItem item) { cartManager.decreaseQty(item); }
    public void removeItem(PosItem item) { cartManager.removeItem(item); }
    public void clear() { 
        cartManager.clear();
        scannedCode.set("");
    }
    public boolean hasItems() { return !cartManager.isEmpty(); }
    
    // ========== DiscountManager Delegate ==========
    public void discountItemPrice(PosItem item, double newPrice) { discountManager.discountItemPrice(item, newPrice); }
    public void changeItemPrice(PosItem item, double newPrice) { discountManager.changeItemPrice(item, newPrice); }
    public void applyDiscountPercent(double percent) { discountManager.applyPercentToSelected(percent); }
    public void applyDiscountAmount(double amount) { discountManager.applyAmountToSelected(amount); }
    public void applyUnitDiscount(double unitDiscountAmount) { discountManager.applyUnitDiscountToSelected(unitDiscountAmount); }
    
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
    
    public boolean hasHoldItems() { return holdManager.hasHoldItems(); }
    
    // ========== ScanHandler Delegate ==========
    public void scan(String barcode) { scanHandler.scan(barcode); }
    public void addQuickAmountItem(double amount) { 
        scanHandler.addQuickAmountItem(amount);
        scanStatus.set(String.format(STATUS_QUICK_ADD, amount));
    }
    
    // ========== UI Properties ==========
    public StringProperty scannedCodeProperty() { return scannedCode; }
    public StringProperty scanStatusProperty() { return scanStatus; }
    
    // ========== 결제 메서드 (위임) ==========
    
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash) {
        processCashPayment(totalAmount, receivedCash, null);
    }
    
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash, 
                                   Consumer<Boolean> onComplete) {
        paymentProcessor.processCashPayment(totalAmount, receivedCash, onComplete, createResultHandler());
    }
    
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount) {
        processCashoutPayment(cashoutAmount, totalCardAmount, null);
    }
    
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      Consumer<Boolean> onComplete) {
        paymentProcessor.processCashoutPayment(cashoutAmount, totalCardAmount, onComplete, createResultHandler());
    }
    
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart) {
        processMixedPayment(cashPart, creditPart, null);
    }
    
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart,
                                    Consumer<Boolean> onComplete) {
        paymentProcessor.processMixedPayment(cashPart, creditPart, onComplete, createResultHandler());
    }
    
    // ========== Private Helper Methods ==========
    private PaymentProcessor.PaymentResultHandler createResultHandler() {
        return new PaymentProcessor.PaymentResultHandler() {
            @Override
            public boolean handleResult(com.swna.javafx.pos.service.PaymentResult result, String successMessage) {
                if (result.isSuccess()) {
                    List<PosItem> soldItems = new ArrayList<>(cartManager.getItems());
                    eventPublisher.publishEvent(new PaymentSuccessEvent(soldItems, result));

                    log.info("[VM] {}", successMessage);
                    scanStatus.set(STATUS_PAYMENT_SUCCESS + ": " + result.getSaleResponse().receiptNo());
                    clear();
                    return true;
                } else {
                    log.warn("[VM] Payment failed: {}", result.getMessage());
                    scanStatus.set(STATUS_PAYMENT_FAIL + ": " + result.getMessage());
                    return false;
                }
            }

            @Override
            public void onFailure(String message) {
                scanStatus.set(STATUS_PAYMENT_FAIL + ": " + message);
            }
        };
    }
    
    @EventListener
    public void handlePrintFailure(PrintFailureEvent event) {
        Platform.runLater(() -> {
            scanStatus.set(event.errorMessage()); 
            log.error("[PRINT ERROR] Receipt No: {}, Msg: {}", event.receiptNo(), event.errorMessage());
        });
    }
}