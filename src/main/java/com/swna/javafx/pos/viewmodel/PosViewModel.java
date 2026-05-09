package com.swna.javafx.pos.viewmodel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.DiscountRequest;
import com.swna.javafx.pos.dto.request.DiscountType;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleItemRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.service.PaymentResult;
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

@Slf4j
@Component
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
    private static final String STATUS_PAYMENT_SUCCESS = "Payment success ✓";
    private static final String STATUS_PAYMENT_FAIL = "Payment failed ❌";
    
    // ========== 결제 타입 상수 ==========
    private static final String PAY_CASH = "CASH";
    private static final String PAY_CARD = "CARD";
    
    // ========== Managers ==========
    private final ApplicationEventPublisher eventPublisher;
    
    private final CartManager cartManager;
    private final DiscountManager discountManager;
    private final HoldManager holdManager;
    private final ScanHandler scanHandler;
    private final PaymentService paymentService;
    
    // ========== UI 상태 ==========
    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);
    
    public PosViewModel(ApplicationEventPublisher eventPublisher, PosService posService, PaymentService paymentService) {
        this.eventPublisher = eventPublisher;
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
    // 결제 메서드 (SaleRequest를 ViewModel에서 생성하여 PaymentService에 전달)
    // =========================================================================
    
    /**
     * 현금 결제 처리 (비동기)
     */
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash, 
                                    java.util.function.Consumer<Boolean> onComplete) {
        if (receivedCash == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            scanStatus.set(STATUS_PAYMENT_FAIL + ": Invalid payment amounts");
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        BigDecimal change = receivedCash.subtract(totalAmount);
        if (change.compareTo(BigDecimal.ZERO) < 0) {
            scanStatus.set(STATUS_PAYMENT_FAIL + ": Insufficient cash received");
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        // 1. 기본 SaleRequest 생성 (items + discounts)
        SaleRequest baseRequest = buildBaseSaleRequest();
        
        // 2. 결제 정보 추가
        List<PaymentRequest> payments = List.of(
            new PaymentRequest(PAY_CASH, totalAmount, receivedCash, BigDecimal.ZERO, null)
        );
        
        // 3. 최종 SaleRequest 생성 (items, payments, discounts 모두 포함)
        SaleRequest finalRequest = new SaleRequest(baseRequest.items(), payments, baseRequest.discounts());
        
        // 4. 결제 실행
        paymentService.executePayment(finalRequest, "Cash")
            .subscribe(result -> {
                Platform.runLater(() -> {
                    boolean success = handlePaymentResult(result, "Cash payment success. Change: " + change);
                    if (onComplete != null) onComplete.accept(success);
                });
            });
    }

    /**
     * 현금 인출(Cashout) 결제 처리 (비동기)
     */
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                    java.util.function.Consumer<Boolean> onComplete) {
        if (cashoutAmount == null || cashoutAmount.compareTo(BigDecimal.ZERO) < 0) {
            scanStatus.set(STATUS_PAYMENT_FAIL + ": Invalid cashout amount");
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        if (totalCardAmount == null || totalCardAmount.compareTo(BigDecimal.ZERO) <= 0) {
            totalCardAmount = getTotalAfterDiscount();
        }

        String refNo = "CASHOUT_" + System.currentTimeMillis();
        
        // 1. 기본 SaleRequest 생성 (items + discounts)
        SaleRequest baseRequest = buildBaseSaleRequest();
        
        // 2. 결제 정보 추가 (CARD 타입으로 cashout 처리)
        List<PaymentRequest> payments = List.of(
            new PaymentRequest(PAY_CARD, totalCardAmount, totalCardAmount, cashoutAmount, refNo)
        );
        
        // 3. 최종 SaleRequest 생성
        SaleRequest finalRequest = new SaleRequest(baseRequest.items(), payments, baseRequest.discounts());
        
        // 4. 결제 실행
        paymentService.executePayment(finalRequest, "Cashout")
            .subscribe(result -> 
                Platform.runLater(() -> {
                    boolean success = handlePaymentResult(result, "Cashout payment success");
                    if (onComplete != null) onComplete.accept(success);
                })
            );
    }

    /**
     * 혼합 결제 처리 (비동기)
     */
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart,
                                    java.util.function.Consumer<Boolean> onComplete) {
        BigDecimal originalTotalAmount = getTotalAfterDiscount();
        
        BigDecimal totalPayment = cashPart.add(creditPart);
        if (totalPayment.compareTo(originalTotalAmount) != 0) {
            scanStatus.set(STATUS_PAYMENT_FAIL + ": Amount mismatch");
            if (onComplete != null) onComplete.accept(false);
            return;
        }
        
        // 1. 결제 정보 리스트 구성
        List<PaymentRequest> payments = new ArrayList<>();
        if (cashPart.compareTo(BigDecimal.ZERO) > 0) {
            payments.add(new PaymentRequest(PAY_CASH, cashPart, cashPart, BigDecimal.ZERO, null));
        }
        if (creditPart.compareTo(BigDecimal.ZERO) > 0) {
            payments.add(new PaymentRequest(PAY_CARD, creditPart, creditPart, BigDecimal.ZERO, "CREDIT_" + System.currentTimeMillis()));
        }
        
        // payments가 비어있을 수 없으므로 (위에서 cashPart 또는 creditPart가 0보다 큼)
        // 2. 기본 SaleRequest 생성
        SaleRequest baseRequest = buildBaseSaleRequest();
        
        // 3. 최종 SaleRequest 생성 (items + payments + discounts)
        SaleRequest finalRequest = new SaleRequest(baseRequest.items(), payments, baseRequest.discounts());
        
        // 4. 결제 실행
        paymentService.executePayment(finalRequest, "Mixed")
            .subscribe(result -> {
                Platform.runLater(() -> {
                    boolean success = handlePaymentResult(result, "Mixed payment success");
                    if (onComplete != null) onComplete.accept(success);
                });
            });
    }

    // ========== Private Helper Methods ==========

    /**
     * 기본 SaleRequest 생성 (items + discounts만 포함, payments는 제외)
     * 
     * @return payments가 null인 SaleRequest
     */
    private SaleRequest buildBaseSaleRequest() {
        // 1. 아이템 리스트 변환
        List<SaleItemRequest> itemRequests = toItemRequests(cartManager.getItems());
        
        // 2. 전체 할인 금액 계산
        BigDecimal totalDiscount = BigDecimal.valueOf(cartManager.totalDiscountProperty().get());
        
        // 3. 할인 정보 추가 (할인이 있는 경우에만)
        List<DiscountRequest> discounts = new ArrayList<>();
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            discounts.add(DiscountRequest.fixed(totalDiscount, "Cart total discount"));
        }
        
        // payments는 null로 설정 (호출하는 곳에서 반드시 추가할 예정)
        return new SaleRequest(itemRequests, null, discounts);
    }
    
    /**
     * PosItem 리스트를 SaleItemRequest 리스트로 변환
     */
    private List<SaleItemRequest> toItemRequests(ObservableList<PosItem> items) {
        return items.stream()
            .map(item -> new SaleItemRequest(
                item.getBarcode(),
                item.getQty(),
                BigDecimal.valueOf(item.getOriginalPrice()),
                BigDecimal.valueOf(item.getSellingPrice()),
                BigDecimal.valueOf(item.getUnitDiscount()),
                item.getUnitDiscount() > 0 ? item.getDiscountType() : DiscountType.NONE,
                Objects.requireNonNullElse(item.getComment(), "")
            ))
            .toList();
    }

    /**
     * 할인 적용된 최종 결제 금액 계산
     */
    private BigDecimal getTotalAfterDiscount() {
        BigDecimal total = BigDecimal.valueOf(cartManager.totalAmountProperty().get());
        BigDecimal disc = BigDecimal.valueOf(cartManager.totalDiscountProperty().get());
        return total.subtract(disc);
    }
    
    // 오버로드 메서드 (콜백 없음)
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash) {
        processCashPayment(totalAmount, receivedCash, null);
    }

    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount) {
        processCashoutPayment(cashoutAmount, totalCardAmount, null);
    }

    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart) {
        processMixedPayment(cashPart, creditPart, null);
    }

    /**
     * 결제 결과 처리
     */
    private boolean handlePaymentResult(PaymentResult result, String successLogMessage) {
        if (result.isSuccess()) {
            List<PosItem> soldItems = new ArrayList<>(cartManager.getItems());
            eventPublisher.publishEvent(new PaymentSuccessEvent(soldItems, result));
            
            log.info("[VM] {}", successLogMessage);
            scanStatus.set(STATUS_PAYMENT_SUCCESS + ": " + result.getSaleResponse().receiptNo());
            clear();
            return true;
        } else {
            log.warn("[VM] Payment failed: {}", result.getMessage());
            scanStatus.set(STATUS_PAYMENT_FAIL + ", " + result.getMessage());
            return false;
        }
    }

    /**
     * 프린트 실패 이벤트 리스너
     */
    @EventListener
    public void handlePrintFailure(PrintFailureEvent event) {
        Platform.runLater(() -> {
            scanStatus.set(event.errorMessage()); 
            log.error("[PRINT ERROR] Receipt No: {}, Msg: {}", event.receiptNo(), event.errorMessage());
        });
    }
}