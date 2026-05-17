package com.swna.javafx.pos.viewmodel.manager;

import com.swna.javafx.pos.model.PosItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HoldManager {
    
    private final CartManager cartManager;
    private final ObservableList<PosItem> holdItems = FXCollections.observableArrayList();
    
    public HoldManager(CartManager cartManager) {
        this.cartManager = cartManager;
    }
    
    public boolean save() {
        if (cartManager.isEmpty()) {
            return false;
        }
        
        holdItems.clear();
        for (PosItem item : cartManager.getItems()) {
            holdItems.add(new PosItem(item)); // Deep copy
        }
        
        cartManager.clear();
        log.info("[Hold] Saved {} items", holdItems.size());
        return true;
    }
    
    public boolean resume() {
        if (holdItems.isEmpty()) {
            return false;
        }
        
        cartManager.clear();
        cartManager.getItems().addAll(holdItems);
        holdItems.clear();
        
        log.info("[Hold] Resumed cart");
        return true;
    }
    
    public boolean hasHoldItems() {
        return !holdItems.isEmpty();
    }
    
    public int getHoldCount() {
        return holdItems.size();
    }
}