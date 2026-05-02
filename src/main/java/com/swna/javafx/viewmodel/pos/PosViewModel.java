package com.swna.javafx.viewmodel.pos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.application.pos.PosService;
import com.swna.javafx.domain.pos.PosItem;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.log4j.Log4j2;

/**
 * POS ViewModel 클래스
 * 
 * MVC 패턴에서 Model과 View 사이의 중간 계층으로,
 * 비즈니스 로직 처리를 담당하고 UI 바인딩을 위한 속성들을 제공합니다.
 * 
 * @author POS Team
 * @version 1.0
 */
@Log4j2
@Component
@Scope("prototype")
public class PosViewModel {

    // ============================================================
    // 1. Constants (상수 정의)
    // ============================================================
    
    /** 수동 상품 바코드 접두사 */
    private static final String MANUAL_ITEM_PREFIX = "M-";
    
    /** 수동 상품 설명 포맷 */
    private static final String MANUAL_ITEM_DESC_FORMAT = "Open Quick Item ($%.2f)";
    
    /** 수동 상품 생성용 타임스탬프 포맷 (MMddHHmm) */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("MMddHHmm");
    
    /** 스캔 준비 상태 메시지 */
    private static final String STATUS_READY = "Scan ready";
    
    /** 스캔 중 상태 메시지 */
    private static final String STATUS_SCANNING = "Scanning...";
    
    /** 스캔 성공 상태 메시지 포맷 */
    private static final String STATUS_SUCCESS = "Scan successful ✓ Code: %s";
    
    /** 스캔 실패 상태 메시지 */
    private static final String STATUS_FAILED = "상품 조회 실패 ❌";
    
    /** 장바구니 비어있음 상태 메시지 */
    private static final String STATUS_EMPTY = "상품 없음 ❌";
    
    /** 수동 상품 추가 상태 메시지 포맷 */
    private static final String STATUS_QUICK_ITEM = "Add Quick Item : $%.2f";

    // ============================================================
    // 2. Dependencies (의존성 주입)
    // ============================================================
    
    /** POS 비즈니스 로직 처리를 위한 서비스 */
    private final PosService posService;

    // ============================================================
    // 3. Observable Properties (UI 바인딩 속성)
    // ============================================================
    
    /** 장바구니 아이템 리스트 (내부 속성 변경 감지 가능) */
    private final ObservableList<PosItem> items = FXCollections.observableArrayList(item -> 
        new javafx.beans.Observable[] { 
            item.qtyProperty(),
            item.finalAmountProperty(),
            item.discountTotalProperty(),
            item.unitDiscountProperty()
        }
    );
    
    /** 총 결제 금액 */
    private final DoubleProperty totalAmount = new SimpleDoubleProperty(0);
    
    /** 총 할인 금액 */
    private final DoubleProperty discount = new SimpleDoubleProperty(0);
    
    /** 총 상품 수량 */
    private final IntegerProperty totalQty = new SimpleIntegerProperty(0);
    
    /** 마지막으로 스캔된 바코드 */
    private final StringProperty scannedCode = new SimpleStringProperty("");
    
    /** 스캔 처리 상태 메시지 */
    private final StringProperty scanStatus = new SimpleStringProperty(STATUS_READY);
    
    /** 현재 선택된 아이템 */
    private final ObjectProperty<PosItem> selectedItem = new SimpleObjectProperty<>();

    // ============================================================
    // 4. Constructor (생성자)
    // ============================================================
    
    /**
     * PosViewModel 생성자
     * 
     * @param posService POS 비즈니스 로직 서비스 (의존성 주입)
     */
    public PosViewModel(PosService posService) {
        this.posService = posService;
        initializeBindings();
    }

    // ============================================================
    // 5. Initialization Methods (초기화 메서드)
    // ============================================================
    
    /**
     * 모든 바인딩을 초기화합니다.
     * 총액, 총 수량, 총 할인액에 대한 자동 계산 바인딩을 설정합니다.
     */
    private void initializeBindings() {
        initializeTotalAmountBinding();
        initializeTotalQuantityBinding();
        initializeTotalDiscountBinding();
    }
    
    /**
     * 총 결제 금액 바인딩을 초기화합니다.
     * 장바구니 아이템 리스트에 변경이 있을 때마다 자동으로 총액이 재계산됩니다.
     */
    private void initializeTotalAmountBinding() {
        totalAmount.bind(Bindings.createDoubleBinding(
            this::calculateTotalAmount, items
        ));
    }
    
    /**
     * 총 상품 수량 바인딩을 초기화합니다.
     * 장바구니 아이템 리스트에 변경이 있을 때마다 자동으로 총 수량이 재계산됩니다.
     */
    private void initializeTotalQuantityBinding() {
        totalQty.bind(Bindings.createIntegerBinding(
            this::calculateTotalQuantity, items
        ));
    }
    
    /**
     * 총 할인 금액 바인딩을 초기화합니다.
     * 장바구니 아이템 리스트에 변경이 있을 때마다 자동으로 총 할인액이 재계산됩니다.
     */
    private void initializeTotalDiscountBinding() {
        discount.bind(Bindings.createDoubleBinding(
            this::calculateTotalDiscount, items
        ));
    }

    // ============================================================
    // 6. Calculation Methods (계산 메서드)
    // ============================================================
    
    /**
     * 장바구니 전체 상품의 총 결제 금액을 계산합니다.
     * 
     * @return 총 결제 금액
     */
    private double calculateTotalAmount() {
        return items.stream().mapToDouble(PosItem::getFinalAmount).sum();
    }
    
    /**
     * 장바구니 전체 상품의 총 수량을 계산합니다.
     * 
     * @return 총 상품 수량
     */
    private int calculateTotalQuantity() {
        return items.stream().mapToInt(PosItem::getQty).sum();
    }
    
    /**
     * 장바구니 전체 상품의 총 할인액을 계산합니다.
     * 
     * @return 총 할인 금액
     */
    private double calculateTotalDiscount() {
        return items.stream().mapToDouble(PosItem::getDiscountTotal).sum();
    }

    // ============================================================
    // 7. Public Business Methods (공개 비즈니스 메서드)
    // ============================================================
    
    // ----- 7.1 Scan Operations (스캔 관련) -----
    
    /**
     * 바코드를 스캔하고 해당 상품을 장바구니에 추가합니다.
     * 
     * 기존에 동일한 상품이 있는 경우 수량만 증가시키고,
     * 없는 경우 새로 추가합니다.
     * 
     * @param barcode 스캔된 바코드 문자열
     */
    public void scan(String barcode) {
        if (isInvalidBarcode(barcode)) return;
        
        updateStatus(STATUS_SCANNING);
        
        posService.scan(barcode).subscribe(
            item -> handleScanSuccess(barcode, item),
            error -> handleScanError(),
            this::handleScanCompletion
        );
    }
    
    // ----- 7.2 Quick Amount Operations (빠른 금액 입력 관련) -----
    
    /**
     * 빠른 금액 입력 버튼으로 수동 상품을 추가합니다.
     * 
     * 금액만 입력받아 임시 상품을 생성하며,
     * 동일한 금액의 수동 상품이 이미 있는 경우 수량만 증가시킵니다.
     * 
     * @param amount 상품 금액
     */
    public void addQuickAmountItem(double amount) {
        PosItem target = findOrCreateManualItem(amount);
        sortItemsWithTop(target);
        selectItem(target);
        updateStatus(String.format(STATUS_QUICK_ITEM, amount));
        log.info("[QUICK] Added manual item: ${}", amount);
    }
    
    // ----- 7.3 Quantity Operations (수량 관련) -----
    
    /**
     * 아이템의 수량을 1 증가시킵니다.
     * 
     * @param item 수량을 증가시킬 아이템
     */
    public void increaseQty(PosItem item) {
        if (isInvalidItem(item)) return;
        item.increaseQty();
        selectItem(item);
        log.debug("[QTY] Increased - {} (Qty: {})", item.getBarcode(), item.getQty());
    }
    
    /**
     * 아이템의 수량을 1 감소시킵니다.
     * 
     * 수량이 0이 되면 장바구니에서 아이템을 제거합니다.
     * 
     * @param item 수량을 감소시킬 아이템
     */
    public void decreaseQty(PosItem item) {
        if (isInvalidItem(item)) return;
        
        item.decreaseQty();
        
        if (item.getQty() <= 0) {
            removeItem(item);
            selectItem(null);
        } else {
            selectItem(item);
        }
        log.debug("[QTY] Decreased - {} (Qty: {})", item.getBarcode(), item.getQty());
    }
    
    /**
     * 장바구니에서 아이템을 제거합니다.
     * 
     * @param item 제거할 아이템
     */
    public void removeItem(PosItem item) {
        if (item != null) {
            items.remove(item);
            log.debug("[REMOVE] Removed - {}", item.getBarcode());
        }
    }
    
    // ----- 7.4 Price Operations (가격 관련) -----
    
    /**
     * 아이템의 단가를 변경하고 할인 정보를 업데이트합니다.
     * (할인 적용 시 사용 - 가격이 인하되는 경우)
     * 
     * @param item 가격을 변경할 대상 아이템
     * @param newPrice 새로운 단가
     */
    public void discountItemPrice(PosItem item, double newPrice) {
        if (isInvalidItem(item)) return;
        
        double originalPrice = item.getSellingPrice();
        double priceDifference = originalPrice - newPrice;
        
        log.info("[PRICE] Discount: {} ${} -> ${}", item.getBarcode(), originalPrice, newPrice);
        
        item.setSellingPrice(newPrice);
        
        if (priceDifference > 0) {
            item.setUnitDiscount(priceDifference);
        } else if (priceDifference < 0) {
            item.setUnitDiscount(0);
        }
    }
    
    /**
     * 아이템의 가격을 순수하게 변경합니다 (할인 정보 초기화).
     * (일반 가격 변경 시 사용)
     * 
     * @param item 가격을 변경할 대상 아이템
     * @param newPrice 새로운 단가
     */
    public void changeItemPrice(PosItem item, double newPrice) {
        if (isInvalidItem(item)) return;
        
        log.info("[PRICE] Change: {} ${} -> ${}", 
                 item.getBarcode(), item.getSellingPrice(), newPrice);
        
        item.setUnitDiscount(0);
        item.setSellingPrice(newPrice);
    }
    
    // ----- 7.5 Discount Operations (할인 관련) -----
    
    /**
     * 선택된 아이템에 퍼센트 할인을 적용합니다.
     * 
     * @param percent 할인 퍼센트 (0~100)
     */
    public void applyDiscountPercent(double percent) {
        applyToSelectedItem(item -> item.applyDiscount(percent, 0));
    }
    
    /**
     * 선택된 아이템에 금액 할인을 적용합니다.
     * 
     * @param amount 할인 금액
     */
    public void applyDiscountAmount(double amount) {
        applyToSelectedItem(item -> item.applyDiscount(0, amount));
    }
    
    /**
     * 선택된 아이템에 단가 기준 할인을 적용합니다 (개당 할인).
     * 
     * @param unitDiscountAmount 개당 할인 금액
     */
    public void applyUnitDiscount(double unitDiscountAmount) {
        applyToSelectedItem(item -> item.applyUnitDiscount(unitDiscountAmount));
    }
    
    // ----- 7.6 Reset Operations (초기화 관련) -----
    
    /**
     * 모든 상태를 초기화합니다.
     * 장바구니를 비우고 선택된 아이템, 스캔 정보를 리셋합니다.
     */
    public void clear() {
        items.clear();
        selectItem(null);
        updateScannedCode("");
        updateStatus(STATUS_READY);
        log.info("[CLEAR] All state cleared");
    }

    // ============================================================
    // 8. Private Helper Methods (내부 헬퍼 메서드)
    // ============================================================
    
    // ----- 8.1 Validation Methods (검증 관련) -----
    
    /**
     * 바코드가 유효하지 않은지 확인합니다.
     * 
     * @param barcode 확인할 바코드
     * @return null이거나 빈 문자열이면 true
     */
    private boolean isInvalidBarcode(String barcode) {
        return barcode == null || barcode.isBlank();
    }
    
    /**
     * 아이템이 유효하지 않은지 확인합니다.
     * 
     * @param item 확인할 아이템
     * @return null이면 true
     */
    private boolean isInvalidItem(PosItem item) {
        return item == null;
    }
    
    // ----- 8.2 Scan Helper Methods (스캔 헬퍼 관련) -----
    
    /**
     * 스캔 성공 시 처리 로직을 수행합니다.
     * 
     * @param barcode 스캔된 바코드
     * @param item 스캔된 상품 정보
     */
    private void handleScanSuccess(String barcode, PosItem item) {
        PosItem target = findOrCreateItem(item);
        selectItem(target);
        updateScannedCode(barcode);
        updateStatus(String.format(STATUS_SUCCESS, barcode));
        log.info("[SCAN] Success: {} (Qty: {})", barcode, target.getQty());
    }
    
    /**
     * 스캔 실패 시 처리 로직을 수행합니다.
     */
    private void handleScanError() {
        updateStatus(STATUS_FAILED);
        log.warn("[SCAN] Failed");
    }
    
    /**
     * 스캔 완료 후 장바구니가 비어있을 때 상태를 업데이트합니다.
     */
    private void handleScanCompletion() {
        if (items.isEmpty()) {
            updateStatus(STATUS_EMPTY);
        }
    }
    
    /**
     * 기존 아이템을 찾거나 새로 생성하여 반환합니다.
     * 
     * @param item 스캔된 상품 정보
     * @return 장바구니에 추가/갱신된 아이템
     */
    private PosItem findOrCreateItem(PosItem item) {
        Optional<PosItem> existing = findItemByCode(item.getCode());
        
        if (existing.isPresent()) {
            existing.get().increaseQty();
            return existing.get();
        } else {
            item.increaseQty();
            items.add(item);
            return item;
        }
    }
    
    // ----- 8.3 Search Methods (검색 관련) -----
    
    /**
     * 상품 코드로 기존 아이템을 검색합니다.
     * 
     * @param code 상품 코드
     * @return Optional로 래핑된 기존 아이템
     */
    private Optional<PosItem> findItemByCode(String code) {
        return items.stream()
                    .filter(i -> i.getCode().equals(code))
                    .findFirst();
    }
    
    /**
     * 동일한 금액의 기존 수동 상품을 검색합니다.
     * 
     * @param amount 검색할 금액
     * @return Optional로 래핑된 기존 수동 상품
     */
    private Optional<PosItem> findExistingManualItem(double amount) {
        return items.stream()
                    .filter(i -> i.getBarcode().startsWith(MANUAL_ITEM_PREFIX))
                    .filter(i -> i.getSellingPrice() == amount)
                    .findFirst();
    }
    
    // ----- 8.4 Manual Item Methods (수동 상품 관련) -----
    
    /**
     * 수동 상품을 찾거나 새로 생성합니다.
     * 
     * @param amount 상품 금액
     * @return 찾거나 생성된 수동 상품
     */
    private PosItem findOrCreateManualItem(double amount) {
        Optional<PosItem> existing = findExistingManualItem(amount);
        
        if (existing.isPresent()) {
            existing.get().increaseQty();
            return existing.get();
        } else {
            return createNewManualItem(amount);
        }
    }
    
    /**
     * 새로운 수동 상품을 생성합니다.
     * 
     * @param amount 상품 금액
     * @return 생성된 수동 상품
     */
    private PosItem createNewManualItem(double amount) {
        PosItem newItem = new PosItem();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        newItem.setBarcode(String.format("%s%s-%.0f", MANUAL_ITEM_PREFIX, timestamp, amount));
        newItem.setDescription(String.format(MANUAL_ITEM_DESC_FORMAT, amount));
        newItem.setSellingPrice(amount);
        newItem.setOriginalPrice(amount);
        newItem.increaseQty();
        
        return newItem;
    }
    
    // ----- 8.5 Sort Methods (정렬 관련) -----
    
    /**
     * 장바구니 아이템을 정렬합니다.
     * 지정된 아이템을 최상단으로 배치하고 나머지는 바코드순으로 정렬합니다.
     * 
     * @param topItem 최상단에 위치시킬 아이템
     */
    private void sortItemsWithTop(PosItem topItem) {
        items.sort(createTopItemComparator(topItem));
    }
    
    /**
     * 아이템 정렬을 위한 Comparator를 생성합니다.
     * 
     * @param topItem 최상단에 위치시킬 아이템
     * @return 정렬 규칙이 정의된 Comparator
     */
    private Comparator<PosItem> createTopItemComparator(PosItem topItem) {
        return (a, b) -> {
            if (a == topItem) return -1;
            if (b == topItem) return 1;
            return a.getBarcode().compareTo(b.getBarcode());
        };
    }
    
    // ----- 8.6 State Update Methods (상태 업데이트 관련) -----
    
    /**
     * 스캔 상태 메시지를 업데이트합니다.
     * 
     * @param status 새로운 상태 메시지
     */
    private void updateStatus(String status) {
        scanStatus.set(status);
    }
    
    /**
     * 스캔된 바코드 코드를 업데이트합니다.
     * 
     * @param barcode 스캔된 바코드
     */
    private void updateScannedCode(String barcode) {
        scannedCode.set(barcode);
    }
    
    /**
     * 선택된 아이템을 업데이트합니다.
     * 
     * @param item 새로 선택할 아이템
     */
    private void selectItem(PosItem item) {
        selectedItem.set(item);
    }
    
    // ----- 8.7 Common Helper Methods (공통 헬퍼 관련) -----
    
    /**
     * 선택된 아이템에 작업을 적용하는 헬퍼 메서드
     * 
     * @param operation 적용할 작업 (Consumer)
     */
    private void applyToSelectedItem(Consumer<PosItem> operation) {
        PosItem item = selectedItem.get();
        if (item == null) {
            log.warn("[DISCOUNT] No item selected");
            return;
        }
        operation.accept(item);
    }

    // ============================================================
    // 9. Getters for UI Binding (UI 바인딩용 Getter)
    // ============================================================
    
    /**
     * 장바구니 아이템 리스트를 반환합니다.
     * 
     * @return ObservableList of PosItem
     */
    public ObservableList<PosItem> getItems() { 
        return items; 
    }
    
    /**
     * 총 결제 금액 속성을 반환합니다.
     * 
     * @return 총 금액 DoubleProperty
     */
    public DoubleProperty totalAmountProperty() { 
        return totalAmount; 
    }
    
    /**
     * 총 할인액 속성을 반환합니다.
     * 
     * @return 총 할인액 DoubleProperty
     */
    public DoubleProperty discountProperty() { 
        return discount; 
    }
    
    /**
     * 총 수량 속성을 반환합니다.
     * 
     * @return 총 수량 IntegerProperty
     */
    public IntegerProperty totalQtyProperty() { 
        return totalQty; 
    }
    
    /**
     * 스캔된 바코드 속성을 반환합니다.
     * 
     * @return 스캔 코드 StringProperty
     */
    public StringProperty scannedCodeProperty() { 
        return scannedCode; 
    }
    
    /**
     * 스캔 상태 메시지 속성을 반환합니다.
     * 
     * @return 스캔 상태 StringProperty
     */
    public StringProperty scanStatusProperty() { 
        return scanStatus; 
    }
    
    /**
     * 선택된 아이템 속성을 반환합니다.
     * 
     * @return 선택된 아이템 ObjectProperty
     */
    public ObjectProperty<PosItem> selectedItemProperty() { 
        return selectedItem; 
    }
}