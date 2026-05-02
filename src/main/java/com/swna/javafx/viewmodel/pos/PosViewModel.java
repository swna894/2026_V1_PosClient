package com.swna.javafx.viewmodel.pos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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

@Log4j2
@Component
@Scope("prototype")
public class PosViewModel {

    private final PosService posService;

    // =========================
    // 상태
    // =========================
    // 1. [핵심] Extractor 설정: 아이템 내부의 값이 바뀔 때 리스트가 반응하도록 합니다.
    private final ObservableList<PosItem> items = FXCollections.observableArrayList(item -> 
        new javafx.beans.Observable[] { 
            item.qtyProperty(), 
            item.finalAmountProperty(),
            item.discountTotalProperty(),
            item.unitDiscountProperty()  // unitDiscount 변경도 감지
        }
    );

    private final DoubleProperty totalAmount = new SimpleDoubleProperty(0);
    private final DoubleProperty discount = new SimpleDoubleProperty(0);
    private final IntegerProperty totalQty = new SimpleIntegerProperty(0);

    private final StringProperty scannedCode = new SimpleStringProperty("");
    private final StringProperty scanStatus = new SimpleStringProperty("Scan ready");

    private final ObjectProperty<PosItem> selectedItem = new SimpleObjectProperty<>();

    // =========================
    // 생성자
    // =========================
    public PosViewModel(PosService posService) {
        this.posService = posService;
        // 2. 바인딩 초기화 호출
        initTotalBinding();
    }

    /**
     * 🔥 [핵심] 수동 recalc() 대신 바인딩 사용
     * items 리스트에 변화(추가, 삭제, 내부 값 변경)가 생기면 자동으로 계산됩니다.
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
        
        // 전체 할인액 합계도 바인딩 가능
        discount.bind(Bindings.createDoubleBinding(
            () -> items.stream().mapToDouble(PosItem::getDiscountTotal).sum(),
            items
        ));
    }
    
    // =========================
    // 🔥 핵심: 비동기 스캔
    // =========================
    public void scan(String barcode) {

        if (barcode == null || barcode.isBlank()) return;

        scanStatus.set("Scanning...");

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
                    scanStatus.set("Scan successful ✓ Code: " + barcode);

                }, error -> {
                    scanStatus.set("상품 조회 실패 ❌");
                }, () -> {
                    if (items.isEmpty()) {
                        scanStatus.set("상품 없음 ❌");
                    }
                });
    }

    public void addQuickAmountItem(double amount) {
        // 1. 기존 리스트 확인 (동일 가격의 수동 상품)
        Optional<PosItem> existing = items.stream()
                .filter(i -> i.getBarcode().startsWith("M-"))
                .filter(i -> i.getSellingPrice() == amount)
                .findFirst();

        PosItem target;

        if (existing.isPresent()) {
            target = existing.get();
            target.increaseQty();
        } else {
            target = new PosItem();
            
            // 2. 날짜/시간 포맷 설정 (연월일시분)
            // 예: 2026년 5월 1일 21시 10분 -> 2605012110
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMddHHmm"));
            
            target.setBarcode("M-" + timestamp + "-" + amount);
            target.setDescription(String.format("Open Quick Item ($%.2f)", amount));
            target.setSellingPrice(amount);
            target.setOriginalPrice(amount);
            target.increaseQty();
            
            items.add(target);
        }

        // [변경 부분] 정렬 로직 적용
        sortItems(target);

        selectedItem.set(target);
        scanStatus.set(String.format("Add Quick Item : $%.2f", amount));
    }

    private void sortItems(PosItem topItem) {
        items.sort((a, b) -> {
            // 1. 방금 입력/수정된 아이템(topItem)을 최상단으로
            if (a == topItem) return -1;
            if (b == topItem) return 1;

            // 2. 나머지는 바코드 알파벳 순으로 정렬
            return a.getBarcode().compareTo(b.getBarcode());
        });
    }

    /**
     * 아이템의 단가를 변경하고 전체 합계를 재계산합니다.
     * @param item 가격을 변경할 대상 아이템
     * @param newPrice 다이얼로그로부터 전달받은 새로운 단가
     */
    public void updateItemPrice(PosItem item, double newPrice) {
        if (item == null) return;
        log.info("[VM] Updating price for item: {} -> {}", item.getBarcode(), newPrice);
        
        double originalPrice = item.getSellingPrice(); // 원래 가격 가져오기
        double priceDifference = originalPrice - newPrice; // 가격 차이 (단가 할인 금액)
        
        // 새로운 판매 가격 설정
        item.setSellingPrice(newPrice);
        
        // 단가 기준 할인 설정 (수량과 무관하게 단가 차이를 저장)
        if (priceDifference > 0) {
            // 가격이 인하된 경우: 단가 할인으로 설정
            item.setUnitDiscount(priceDifference);
            log.info("[VM] Set unit discount: {} (per unit)", priceDifference);
        } else if (priceDifference < 0) {
            // 가격이 인상된 경우: 단가 할인 제거
            item.setUnitDiscount(0);
            log.info("[VM] Removed unit discount due to price increase");
        }
        
        log.info("[VM] Price update completed. Unit discount: {}", item.getUnitDiscount());
    }
    
        // =========================
    // 수량
    // =========================
    public void increaseQty(PosItem item) {
        if (item == null) return;
        item.increaseQty();
        selectedItem.set(item);
        // discountTotal과 finalAmount는 바인딩을 통해 자동 업데이트됨
        log.info("[VM] Increased qty to {}, unit discount: {}, total discount: {}", 
                 item.getQty(), item.getUnitDiscount(), item.getDiscountTotal());
    }

    public void decreaseQty(PosItem item) {
        if (item == null) return;

        item.decreaseQty();

        if (item.getQty() <= 0) {
            items.remove(item);
            selectedItem.set(null);
        } else {
            selectedItem.set(item);
        }
        // discountTotal과 finalAmount는 바인딩을 통해 자동 업데이트됨
        log.info("[VM] Decreased qty to {}, unit discount: {}, total discount: {}", 
                 item != null ? item.getQty() : 0, 
                 item != null ? item.getUnitDiscount() : 0, 
                 item != null ? item.getDiscountTotal() : 0);
    }

    public void removeItem(PosItem item) {
        if (item != null) {
            items.remove(item);
            // 필요 시 여기서 전체 합계(totalAmount) 등을 다시 계산하는 로직 수행
        }
    }

    // =========================
    // 할인
    // =========================
    public void applyDiscountPercent(double percent) {
        PosItem item = selectedItem.get();
        if (item == null) return;
        item.applyDiscount(percent, 0);
    }

    public void applyDiscountAmount(double amount) {
        PosItem item = selectedItem.get();
        if (item == null) return;
        item.applyDiscount(0, amount);
    }
    
    /**
     * 단가 기준 할인 적용 (개당 할인)
     */
    public void applyUnitDiscount(double unitDiscountAmount) {
        PosItem item = selectedItem.get();
        if (item == null) return;
        item.applyUnitDiscount(unitDiscountAmount);
    }

    // =========================
    // 초기화
    // =========================
    public void clear() {
        items.clear();
        selectedItem.set(null);
        scannedCode.set("");
        scanStatus.set("Scan ready");
    }

    // =========================
    // Getter
    // =========================
    public ObservableList<PosItem> getItems() { return items; }

    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public DoubleProperty discountProperty() { return discount; }
    public IntegerProperty totalQtyProperty() { return totalQty; }

    public StringProperty scannedCodeProperty() { return scannedCode; }
    public StringProperty scanStatusProperty() { return scanStatus; }

    public ObjectProperty<PosItem> selectedItemProperty() { return selectedItem; }
}