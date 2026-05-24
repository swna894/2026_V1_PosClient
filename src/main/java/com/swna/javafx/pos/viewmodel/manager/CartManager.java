package com.swna.javafx.pos.viewmodel.manager;

import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.viewmodel.handler.ScanHandler;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CartManager {
    
    private final ObservableList<PosItem> items = FXCollections.observableArrayList(item -> 
        new javafx.beans.Observable[] { 
            item.qtyProperty(), 
            item.finalAmountProperty(),
            item.discountTotalProperty(),
            item.unitDiscountProperty(),
            item.commentProperty(),
            item.sellingPriceProperty(),
            item.barcodeProperty()
        }
    );

    
    
    private final DoubleProperty totalAmount = new SimpleDoubleProperty(0);
    private final DoubleProperty totalDiscount = new SimpleDoubleProperty(0);
    private final IntegerProperty totalQty = new SimpleIntegerProperty(0);
    private final ObjectProperty<PosItem> selectedItem = new SimpleObjectProperty<>();
    
    public CartManager() {
        bindTotals();
    }
    
    private void bindTotals() {
        totalAmount.bind(Bindings.createDoubleBinding(
            () -> items.stream().mapToDouble(PosItem::getFinalAmount).sum(), items));
        totalQty.bind(Bindings.createIntegerBinding(
            () -> items.stream().mapToInt(PosItem::getQty).sum(), items));
        totalDiscount.bind(Bindings.createDoubleBinding(
            () -> items.stream().mapToDouble(PosItem::getDiscountTotal).sum(), items));
    }
    
    // ========== 기본 CRUD ==========
    
    public void addItem(PosItem item) {
        log.debug("[Cart] ADD Item: barcode={}, qty={}", item.getBarcode(), item.getQty());
        items.add(item);
        moveToTop(item);
        selectedItem.set(item);
        log.debug("[Cart] Added: {}", item.getBarcode());
    }
    
    public void removeItem(PosItem item) {
        if (items.remove(item)) {
            if (selectedItem.get() == item) selectedItem.set(null);
            log.debug("[Cart] Removed: {}", item.getBarcode());
        }
    }
    
    public void clear() {
        items.clear();
        selectedItem.set(null);
        log.debug("[Cart] Cleared");
    }
    
    // ========== 수량 조작 ==========
    
    public void increaseQty(PosItem item) {
        if (item == null) return;
        item.increaseQty();
        selectedItem.set(item);
    }
    
    public void decreaseQty(PosItem item) {
        if (item == null) return;
        item.decreaseQty();
        
        if (item.getQty() <= 0) {
            removeItem(item);
        } else {
            selectedItem.set(item);
        }
    }
    
    // ========== 아이템 찾기 ==========
    
    public Optional<PosItem> findByBarcode(String barcode) {
        return items.stream()
                .filter(i -> i.getBarcode().equals(barcode))
                .findFirst();
    }
    
    public Optional<PosItem> findQuickItemByAmount(double amount) {
        return items.stream()
                .filter(i -> i.getBarcode().startsWith(ScanHandler.QUICK_ITEM_PREFIX))
                .filter(i -> i.getSellingPrice() == amount)
                .findFirst();
    }
    
    // ========== 정렬/이동 ==========
    
    public void moveToTop(PosItem item) {
        if (item == null || !items.contains(item)) return;
        items.remove(item);
        items.add(0, item);
    }
    
    // ========== Getter ==========
    
    public ObservableList<PosItem> getItems() { return items; }
    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public DoubleProperty totalDiscountProperty() { return totalDiscount; }
    public IntegerProperty totalQtyProperty() { return totalQty; }
    public ObjectProperty<PosItem> selectedItemProperty() { return selectedItem; }
    public boolean isEmpty() { return items.isEmpty(); }
}
