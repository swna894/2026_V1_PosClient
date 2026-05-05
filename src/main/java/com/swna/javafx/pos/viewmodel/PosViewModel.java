package com.swna.javafx.pos.viewmodel;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleItemRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.service.PaymentService;
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
import java.util.List;

@Slf4j
@Component
@Scope("prototype")
public class PosViewModel {
    
    // ========== 상수 (Constants) ==========
    private static final String STATUS_READY = "Scan ready";
    private static final String STATUS_SCANNING = "Scanning...";
    private static final String STATUS_SCAN_SUCCESS = "Scan successful ✓ Code: %s";
    private static final String STATUS_ITEM_NOT_FOUND = "Item not found ❌";
    private static final String STATUS_SEARCH_FAILED = "Search failed ❌";
    private static final String STATUS_QUICK_ADD = "Add Quick Item : $%.2f";
    private static final String STATUS_HOLD_SAVED = "Cart saved";
    private static final String STATUS_HOLD_NO_ITEMS = "No items to hold";
    private static final String STATUS_HOLD_RESUMED = "Cart resumed";
    private static final String STATUS_HOLD_NO_CART = "No hold cart";
    private static final String STATUS_PAYMENT_SUCCESS = "Payment completed ✓";
    private static final String STATUS_PAYMENT_FAIL = "Payment failed ❌";
    
    // ========== Managers ==========
    private final CartManager cartManager;
    private final DiscountManager discountManager;
    private final HoldManager holdManager;
    private final ScanHandler scanHandler;
    private final PaymentService paymentService;
    
    // ========== UI 상태 ==========
    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);
    
    public PosViewModel(PosService posService, PaymentService paymentService) {
        this.cartManager = new CartManager();
        this.discountManager = new DiscountManager(cartManager);
        this.holdManager = new HoldManager(cartManager);
        this.scanHandler = new ScanHandler(posService, cartManager);
        this.paymentService = paymentService;
        
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
        scanStatus.set(STATUS_READY);
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
    
    public boolean hasItems() { return !cartManager.isEmpty(); }
    public boolean hasHoldItems() { return holdManager.hasHoldItems(); }
    
    // ========== Delegate to ScanHandler ==========
    public void scan(String barcode) { scanHandler.scan(barcode); }
    public void addQuickAmountItem(double amount) { 
        scanHandler.addQuickAmountItem(amount);
        scanStatus.set(String.format(STATUS_QUICK_ADD, amount));
    }
    
    // ========== UI Properties ==========
    public StringProperty scannedCodeProperty() { return scannedCode; }
    public StringProperty scanStatusProperty() { return scanStatus; }

    // =========================================================================
    // 결제 메서드 (PaymentService 위임)
    // =========================================================================
    /**
     * 현금 결제 처리
     * 
     * @param receivedCash 고객이 받은 현금액
     * @return 결제 성공 여부
     */
    public boolean processCashPayment(BigDecimal receivedCash) {
        BigDecimal totalAfterDiscount = getTotalAfterDiscount();
        
        PaymentService.PaymentResult result = paymentService.processCashPayment(
            cartManager.getItems(),
            totalAfterDiscount,
            receivedCash
        );
        
        return handlePaymentResult(result, "Cash payment success. Change: " + result.getChange());
    }

    /**
     * 현금 인출 결제 처리 (카드 결제 + 현금 인출)
     * 
     * @param cashoutAmount 고객이 요청한 인출 현금액
     * @param totalCardAmount 카드로 결제할 총 금액 (할인 적용 후)
     * @return 결제 성공 여부
     */
    public boolean processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount) {
        // totalCardAmount가 null이거나 음수인 경우 전체 금액으로 간주
        if (totalCardAmount == null || totalCardAmount.compareTo(BigDecimal.ZERO) <= 0) {
            totalCardAmount = getTotalAfterDiscount();
        }

        PaymentService.PaymentResult result = paymentService.processCashoutPayment(
            cartManager.getItems(),
            totalCardAmount,
            cashoutAmount
        );
        
        return handlePaymentResult(result, "Cashout payment success - Card: " + totalCardAmount + ", Cashout: " + cashoutAmount);
    }

    /**
     * 현금 + 카드 혼합 결제 처리
     * 
     * @param cashPart 현금 결제 금액
     * @param creditPart 카드 결제 금액
     * @return 결제 성공 여부
     */
    public boolean processMixedPayment(BigDecimal cashPart, BigDecimal creditPart) {
        BigDecimal originaltotalAmount =  BigDecimal.valueOf(cartManager.totalAmountProperty().get());
        
        // 결제 금액 합계가 최종 금액과 일치하는지 검증
        BigDecimal totalPayment = cashPart.add(creditPart);
        if (totalPayment.compareTo(originaltotalAmount) != 0) {
            scanStatus.set(STATUS_PAYMENT_FAIL + ": Amount mismatch");
            return false;
        }
        
        PaymentService.PaymentResult result = paymentService.processMixedPayment(
            cartManager.getItems(),
            totalPayment,
            cashPart,
            creditPart
        );
        
        return handlePaymentResult(result, "Mixed payment success - Cash: " + cashPart + ", Credit: " + creditPart);
    }

    // ========== Private Helper Methods ==========

    /**
     * 할인 적용된 최종 결제 금액 계산
     */
    private BigDecimal getTotalAfterDiscount() {
        BigDecimal total = BigDecimal.valueOf(cartManager.totalAmountProperty().get());
        BigDecimal disc = BigDecimal.valueOf(cartManager.totalDiscountProperty().get());
        return total.subtract(disc);
    }

    /**
     * 결제 결과를 일관되게 처리
     * 
     * @param result 결제 서비스 결과
     * @param successLogMessage 성공 시 로그 메시지
     * @return 결제 성공 여부
     */
    private boolean handlePaymentResult(PaymentService.PaymentResult result, String successLogMessage) {
        if (result.isSuccess()) {
            log.info("[VM] {}", successLogMessage);
            scanStatus.set(STATUS_PAYMENT_SUCCESS);
            clear();
            return true;
        } else {
            log.warn("[VM] Payment failed: {}", result.getMessage());
            scanStatus.set(STATUS_PAYMENT_FAIL + ": " + result.getMessage());
            return false;
        }
    }
}