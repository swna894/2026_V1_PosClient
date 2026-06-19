package com.swna.javafx.pos.viewmodel;

import static com.swna.javafx.pos.viewmodel.PosViewModelConstants.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swna.javafx.pos.api.PosApiService;
import com.swna.javafx.pos.dialog.BalanceDialogController;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.manager.PosDialogManager;
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
    private final PosDialogManager posDialogManager;
    
    // ========== UI 상태 ==========
    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);
    
    public PosViewModel(ApplicationEventPublisher eventPublisher, 
                        ScanService posService, 
                        PosApiService posApiService, PosDialogManager posDialogManager) {
        this.eventPublisher = eventPublisher;
        this.cartManager = new CartManager();
        this.discountManager = new DiscountManager(cartManager);
        this.holdManager = new HoldManager(cartManager);
        this.posDialogManager = posDialogManager;
        this.scanHandler = new ScanHandler(posService, cartManager, posDialogManager);
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
    
    /**
     * 장바구니의 모든 아이템 가격(판매가 * 수량)의 합계를 BigDecimal로 안전하게 계산합니다.
     */
    public BigDecimal calculateActualTotal() {
        return getPosItems().stream()
            .map(item -> {
                // 1. Double을 BigDecimal로 명확히 변환
                BigDecimal price = BigDecimal.valueOf(item.getOriginalPrice());
                BigDecimal qty = BigDecimal.valueOf(item.getQty());
                
                // 2. 연산 수행 후 반환
                return price.multiply(qty);
            })
            // 3. 초기값을 BigDecimal.ZERO로 명시하여 타입 추론 오류 방지
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== Volume Discount Methods ==========

    /**
     * 금액 기준 볼륨 할인 적용
     */
    public void applyVolumeDiscountByAmount(BigDecimal amountAfterDC, Consumer<Boolean> callback) {
        try {
            // [중요] 현재 총액이 아닌, 할인 전의 '진짜 원금'을 가져와야 합니다.
            BigDecimal originalTotal = calculateActualTotal(); 
            
            if (originalTotal.compareTo(BigDecimal.ZERO) <= 0) {
                callback.accept(false);
                return;
            }
            
            // 역산 로직 수정
            BigDecimal discountPercent = originalTotal.subtract(amountAfterDC)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(originalTotal, 4, RoundingMode.HALF_UP); // 정밀도 상향
            
            // 적용 전 기존 할인 초기화 로직이 DiscountManager에 있는지 확인 필요
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
    public void saveCart(int cartId) {
        holdManager.save(cartId); // 수정된 HoldManager의 save(int) 호출
        log.info("Cart {} saved.", cartId);
    }

    public void resumeCart(int cartId) {
        holdManager.resume(cartId); // 수정된 HoldManager의 resume(int) 호출
        log.info("Cart {} resumed.", cartId);
    }

    // UI 버튼 상태 체크용 (버튼 색상 변경 시 필요)
    public boolean isCartHoldingItems(int cartId) {
        return holdManager.hasItems(cartId);
    }

    // 기존 단일 save/resume 메서드가 있다면 삭제하거나, 
    // 혹은 호환성을 위해 아래와 같이 기본값(1번 카트)으로 리다이렉트
    public void holdCart() { saveCart(1); }
    public void resumeCart() { resumeCart(1); }
    
    public CartManager getCartManager() {
        return this.cartManager;
    }

    public HoldManager getHoldManager() {
        return this.holdManager;
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

    
    
    // 프린터 출력 조정을 위해 handleProcessedPayment에 boolean을 추가 
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash, Consumer<Boolean> onComplete) {
        posProcessor.processCashPayment(totalAmount, receivedCash, 
            processed -> {
                BigDecimal balance = receivedCash.subtract(totalAmount);
                
                // 잔액이 있을 경우 다이얼로그 호출
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    Platform.runLater(() -> {
                        // BalanceDialogController.BalanceResult 타입을 명시하여 타입 에러 방지
                        posDialogManager.showBalanceDialog(balance, (BalanceDialogController.BalanceResult result) -> {
                            if (result.isPrint()) {
                                // [Print] 선택: 영수증 출력(true)
                                handleProcessedPayment(processed, onComplete, false);
                            } else if (result.isComplete()) {
                                // [Complete] 선택: 출력 없이 완료(false)
                                handleProcessedPayment(processed, onComplete, true);
                            }
                            // [Cancel] 시에는 결제 단계가 이미 처리된 후이므로 별도 로직 없음
                        });
                    });
                } else {
                    // 잔액이 없으면 즉시 완료
                    handleProcessedPayment(processed, onComplete, false);
                }
            },
            createResultHandler()
        );
    }
    // ========== 취소 진행 ==========
    public void processCancelPayment(BigDecimal totalAmount, BigDecimal receivedCash,  Consumer<Boolean> onComplete) {
        posProcessor.processCancelPayment(totalAmount, receivedCash, 
            processed -> handleProcessedPayment(processed, onComplete, true),
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
            processed -> handleProcessedPayment(processed, onComplete, false),
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
            processed -> handleProcessedPayment(processed, onComplete, false),
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
            processed -> handleProcessedPayment(processed, onComplete, false),
            createResultHandler()
        );
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * ProcessedPayment 결과 처리
     */
    private void handleProcessedPayment(PosProcessor.ProcessedPayment processed, 
                                        Consumer<Boolean> onComplete,
                                        boolean skipPrinting) {
        List<PosItem> posItemsList = List.copyOf(getPosItems());

        if (processed.success()) {
            // SaleRequest를 이벤트로 발행
            // ✨ skipPrinting이 false 일 때만 프린트 이벤트를 발행하여 출력을 원천 차단합니다.
            // Canecl( CAEAR ALL ) 경우에 print 방지위해 추가 함
            if (!skipPrinting) {
                eventPublisher.publishEvent(new PaymentSuccessEvent(
                    this, 
                    processed.saleRequest(), 
                    processed.paymentResult(),
                    posItemsList
                ));
            }
            
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
            log.info("Payment Success: " + processed.saleRequest().getPaymentTypeCode());
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
                Platform.runLater(() -> scanStatus.set(STATUS_PAYMENT_FAIL + ": " + message));
            }
            
            @Override
            public void handleSuccess(String successMessage) {
                log.info("[VM] {}", successMessage);
            }
            
            @Override
            public void handleFailure(String errorMessage) {
                Platform.runLater(() ->  scanStatus.set(STATUS_PAYMENT_FAIL + ": " + errorMessage));
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