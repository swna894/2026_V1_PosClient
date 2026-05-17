package com.swna.javafx.pos.viewmodel.manager;

import com.swna.javafx.pos.model.PosItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscountManager {
    
    private final CartManager cartManager;
    
    public DiscountManager(CartManager cartManager) {
        this.cartManager = cartManager;
    }
    
    // ========== 아이템 가격 변경 ==========
    
    public void changeItemPrice(PosItem item, double newPrice) {
        if (item == null) return;
        item.setUnitDiscount(0);
        item.setSellingPrice(newPrice);
        log.debug("[Discount] Price changed to ${}", newPrice);
    }
    
    public void discountItemPrice(PosItem item, double newPrice) {
        if (item == null) return;
        double originalPrice = item.getSellingPrice();
        double discountAmount = originalPrice - newPrice;
        
        item.setSellingPrice(newPrice);
        item.setUnitDiscount(Math.max(0, discountAmount));
        
        log.debug("[Discount] Price discounted: ${} -> ${}", originalPrice, newPrice);
    }
    
    // ========== 선택된 아이템 할인 ==========
    
    public void applyPercentToSelected(double percent) {
        PosItem item = cartManager.selectedItemProperty().get();
        if (item != null) {
            item.applyDiscount(percent, 0);
            log.debug("[Discount] Applied {}% to selected", percent);
        }
    }
    
    public void applyAmountToSelected(double amount) {
        PosItem item = cartManager.selectedItemProperty().get();
        if (item != null) {
            item.applyDiscount(0, amount);
            log.debug("[Discount] Applied ${} to selected", amount);
        }
    }
    
    public void applyUnitDiscountToSelected(double unitDiscountAmount) {
        PosItem item = cartManager.selectedItemProperty().get();
        if (item != null) {
            item.applyUnitDiscount(unitDiscountAmount);
            log.debug("[Discount] Applied unit discount ${}", unitDiscountAmount);
        }
    }
}
