package com.swna.javafx.pos.viewmodel;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.service.PosService;
import com.swna.javafx.pos.viewmodel.handler.ScanHandler;
import com.swna.javafx.pos.viewmodel.manager.CartManager;
import com.swna.javafx.pos.viewmodel.manager.DiscountManager;
import com.swna.javafx.pos.viewmodel.manager.HoldManager;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Scope("prototype")
public class PosViewModel {
    
    // ========== Managers ==========
    private final CartManager cartManager;
    private final DiscountManager discountManager;
    private final HoldManager holdManager;
    private final ScanHandler scanHandler;
    
    // ========== UI 상태 ==========
    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty("Scan ready");
    
    public PosViewModel(PosService posService) {
        this.cartManager = new CartManager();
        this.discountManager = new DiscountManager(cartManager);
        this.holdManager = new HoldManager(cartManager);
        this.scanHandler = new ScanHandler(posService, cartManager);
        
        setupScanCallbacks();
    }
    
    private void setupScanCallbacks() {
        scanHandler.setCallbacks(
            () -> scanStatus.set("Scanning..."),
            (barcode) -> {
                scannedCode.set(barcode);
                scanStatus.set(String.format("Scan successful ✓ Code: %s", barcode));
            },
            () -> scanStatus.set("Item not found ❌"),
            () -> scanStatus.set("Search failed ❌")
        );
    }
    
    // ========== Delegate to CartManager ==========
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
        scanStatus.set("Scan ready");
        scannedCode.set("");
    }
    
    // ========== Delegate to DiscountManager ==========
    public void discountItemPrice(PosItem item, double newPrice) { discountManager.discountItemPrice(item, newPrice); }
    public void changeItemPrice(PosItem item, double newPrice) { discountManager.changeItemPrice(item, newPrice); }
    public void applyDiscountPercent(double percent) { discountManager.applyPercentToSelected(percent); }
    public void applyDiscountAmount(double amount) { discountManager.applyAmountToSelected(amount); }
    public void applyUnitDiscount(double unitDiscountAmount) { discountManager.applyUnitDiscountToSelected(unitDiscountAmount); }
    
    // ========== Delegate to HoldManager ==========
    public void holdCart() {
        if (holdManager.save()) {
            scanStatus.set("Cart saved");
        } else {
            scanStatus.set("No items to hold");
        }
    }
    
    public void resumeCart() {
        if (holdManager.resume()) {
            scanStatus.set("Cart resumed");
        } else {
            scanStatus.set("No hold cart");
        }
    }
    
    public boolean hasItems() { return !cartManager.isEmpty(); }
    public boolean hasHoldItems() { return holdManager.hasHoldItems(); }
    
    // ========== Delegate to ScanHandler ==========
    public void scan(String barcode) { scanHandler.scan(barcode); }
    public void addQuickAmountItem(double amount) { 
        scanHandler.addQuickAmountItem(amount);
        scanStatus.set(String.format("Add Quick Item : $%.2f", amount));
    }
    
    // ========== UI Properties ==========
    public StringProperty scannedCodeProperty() { return scannedCode; }
    public StringProperty scanStatusProperty() { return scanStatus; }
    
    // ========== 결제 (추후 분리 예정) ==========
    public boolean processCashPayment(BigDecimal receivedCash) {
        System.err.println("Unimplemented method 'processCashPayment'" + receivedCash);
        return false;
    }
    
    public boolean processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCredit) {
        System.err.println("Unimplemented method 'processCashoutPayment'" + cashoutAmount + ", " + totalCredit);
        return false;
    }
    
    public boolean processMixedPayment(BigDecimal cashPart, BigDecimal creditPart) {
        System.err.println("Unimplemented method 'processMixedPayment'" + cashPart + ", " + creditPart);
        return false;
    }
}