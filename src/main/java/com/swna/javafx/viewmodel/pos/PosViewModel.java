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
            item.discountTotalProperty() 
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

    // =========================
    // 수량
    // =========================
    public void increaseQty(PosItem item) {
        if (item == null) return;
        item.increaseQty();
        selectedItem.set(item);
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
    // 계산
    // =========================
    // private void recalc() {
    //     totalAmount.set(items.stream().mapToDouble(i -> i.finalAmountProperty().get()).sum() );
    //     totalQty.set( items.stream() .mapToInt(PosItem::getQty).sum() );
    // }

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