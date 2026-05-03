package com.swna.javafx.viewmodel.pos;

import java.awt.Toolkit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.application.pos.PosService;
import com.swna.javafx.common.util.SoundManager;
import com.swna.javafx.domain.pos.PosItem;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Scope("prototype")
public class PosViewModel {

    // =========================================================================
    // 상수 (Constants)
    // =========================================================================
    private static final String STATUS_READY = "Scan ready";
    private static final String STATUS_SCANNING = "Scanning...";
    private static final String STATUS_SUCCESS = "Scan successful ✓ Code: %s";
    private static final String STATUS_FAIL_NOT_FOUND = "Item not found ❌"; // 상품 없음 ❌
    private static final String STATUS_FAIL_ERROR = "Search failed ❌";      // 상품 조회 실패 ❌
    private static final String STATUS_QUICK_ADD = "Add Quick Item : $%.2f";
    
    private static final String MANUAL_BARCODE_PREFIX = "M-";
    private static final String TIMESTAMP_PATTERN = "MMddHHmm";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    // =========================================================================
    // 필드 및 상태 (Fields & Properties)
    // =========================================================================
    private final PosService posService;

    /** 판매 아이템 리스트 (내부 속성 변경 감지 포함) */
    private final ObservableList<PosItem> items = FXCollections.observableArrayList(item -> 
        new javafx.beans.Observable[] { 
            item.qtyProperty(), 
            item.finalAmountProperty(),
            item.discountTotalProperty(),
            item.unitDiscountProperty() 
        }
    );

    private final DoubleProperty totalAmount = new SimpleDoubleProperty(0);
    private final DoubleProperty discount = new SimpleDoubleProperty(0);
    private final IntegerProperty totalQty = new SimpleIntegerProperty(0);

    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);

    private final ObjectProperty<PosItem> selectedItem = new SimpleObjectProperty<>();

    // =========================================================================
    // 생성자 및 초기화 (Constructor & Initialization)
    // =========================================================================
    public PosViewModel(PosService posService) {
        this.posService = posService;
        initTotalBinding();
    }

    /**
     * 리스트 변경에 따른 합계 및 수량 자동 계산 바인딩 설정
     */
    private void initTotalBinding() {
        totalAmount.bind(Bindings.createDoubleBinding(
            () -> items.stream().mapToDouble(PosItem::getFinalAmount).sum(),
            items
        ));

        totalQty.bind(Bindings.createIntegerBinding(
            () -> items.stream().mapToInt(PosItem::getQty).sum(),
            items
        ));
        
        discount.bind(Bindings.createDoubleBinding(
            () -> items.stream().mapToDouble(PosItem::getDiscountTotal).sum(),
            items
        ));
    }

    // =========================================================================
    // 핵심 비즈니스 로직 (Core Logic - Scan & Add)
    // =========================================================================
    
    /**
     * 바코드를 이용한 상품 스캔 (비동기)
     * @param barcode 스캔된 바코드 문자열
     */
    public void scan(String barcode) {
        if (barcode == null || barcode.isBlank()) return;

        scanStatus.set(STATUS_SCANNING);

        posService.scan(barcode)
                .subscribe(item -> {
                    Optional<PosItem> existing = items.stream()
                            .filter(i -> i.getCode().equals(item.getCode()))
                            .findFirst();

                    PosItem target;
                    if (existing.isPresent()) {
                        target = existing.get();
                        target.increaseQty();
                    } else {
                        item.increaseQty();
                        items.add(item);
                        target = item;
                    }

                    selectedItem.set(target);
                    scannedCode.set(barcode);
                    scanStatus.set(String.format(STATUS_SUCCESS, barcode));

                }, error -> scanStatus.set(STATUS_FAIL_ERROR)
                , () -> {
                    if (items.isEmpty()) {
                        scanStatus.set(STATUS_FAIL_NOT_FOUND);
                    }
                });
    }

    /**
     * 금액 기반 퀵 아이템(Open Item) 추가
     * @param amount 설정할 금액
     */
    public void addQuickAmountItem(double amount) {
        Optional<PosItem> existing = items.stream()
                .filter(i -> i.getBarcode().startsWith(MANUAL_BARCODE_PREFIX))
                .filter(i -> i.getSellingPrice() == amount)
                .findFirst();

        PosItem target;
        if (existing.isPresent()) {
            target = existing.get();
            target.increaseQty();
        } else {
            target = new PosItem();
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            
            target.setBarcode(String.format("%s%s-%.2f", MANUAL_BARCODE_PREFIX, timestamp, amount));
            target.setDescription(String.format("Open Quick Item ($%.2f)", amount));
            target.setSellingPrice(amount);
            target.setOriginalPrice(amount);
            target.increaseQty();
            
            items.add(target);
        }

        sortItems(target);
        selectedItem.set(target);
        scanStatus.set(String.format(STATUS_QUICK_ADD, amount));
    }

    // =========================================================================
    // 아이템 편집 로직 (Item Operations - Qty, Price, Discount)
    // =========================================================================

    /** 아이템 수량 증가 */
    public void increaseQty(PosItem item) {
        if (item == null) return;
        item.increaseQty();
        selectedItem.set(item);
        log.info("[VM] Increased qty: {}", item.getBarcode());
    }

    /** 아이템 수량 감소 (0이 될 경우 리스트에서 제거) */
    public void decreaseQty(PosItem item) {
        if (item == null) return;
        item.decreaseQty();

        if (item.getQty() <= 0) {
            items.remove(item);
            selectedItem.set(null);
        } else {
            selectedItem.set(item);
        }
        log.info("[VM] Decreased qty: {}", item.getBarcode());
    }

    /** 아이템 수동 제거 */
    public void removeItem(PosItem item) {
        if (item != null) {
            items.remove(item);
            Platform.runLater(SoundManager::playError); 
        }
    }

    /** 단가 변경 (할인 차액 기록 포함) */
    public void discountItemPrice(PosItem item, double newPrice) {
        if (item == null) return;
        
        double originalPrice = item.getSellingPrice();
        double priceDifference = originalPrice - newPrice;
        
        item.setSellingPrice(newPrice);
        
        if (priceDifference > 0) {
            item.setUnitDiscount(priceDifference);
        } else {
            item.setUnitDiscount(0);
        }
        log.info("[VM] Price discounted: {} -> {}", originalPrice, newPrice);
    }
    
    /** 아이템 가격 직접 수정 (할인 초기화) */
    public void changeItemPrice(PosItem item, double newPrice) {
        if (item == null) return;
        item.setUnitDiscount(0);
        item.setSellingPrice(newPrice);
        log.info("[VM] Price changed: {}", newPrice);
    }

    /** 퍼센트 할인 적용 */
    public void applyDiscountPercent(double percent) {
        PosItem item = selectedItem.get();
        if (item != null) item.applyDiscount(percent, 0);
    }

    /** 고정 금액 할인 적용 */
    public void applyDiscountAmount(double amount) {
        PosItem item = selectedItem.get();
        if (item != null) item.applyDiscount(0, amount);
    }
    
    /** 단가 기준 개별 할인 적용 */
    public void applyUnitDiscount(double unitDiscountAmount) {
        PosItem item = selectedItem.get();
        if (item != null) item.applyUnitDiscount(unitDiscountAmount);
    }

    // =========================================================================
    // 유틸리티 및 초기화 (Utility & Clear)
    // =========================================================================

    /** 리스트 초기화 */
    public void clear() {
        items.clear();
        selectedItem.set(null);
        scannedCode.set("");
        scanStatus.set(STATUS_READY);
    }

    /** 최근 작업 아이템을 최상단으로 정렬 */
    private void sortItems(PosItem topItem) {
        items.sort((a, b) -> {
            if (a == topItem) return -1;
            if (b == topItem) return 1;
            return a.getBarcode().compareTo(b.getBarcode());
        });
    }

    // =========================================================================
    // Getter Properties
    // =========================================================================
    public ObservableList<PosItem> getItems() { return items; }
    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public DoubleProperty discountProperty() { return discount; }
    public IntegerProperty totalQtyProperty() { return totalQty; }
    public StringProperty scannedCodeProperty() { return scannedCode; }
    public StringProperty scanStatusProperty() { return scanStatus; }
    public ObjectProperty<PosItem> selectedItemProperty() { return selectedItem; }
}