package com.swna.javafx.pos.viewmodel.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.swna.javafx.pos.model.PosItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HoldManager {
    private final CartManager cartManager;
    // Map: 카트 번호(Key) -> 해당 카트의 아이템 목록(Value)
    private final Map<Integer, List<PosItem>> holdCarts = new HashMap<>();
    
    public HoldManager(CartManager cartManager) { this.cartManager = cartManager; }
    
    // 카트 저장
    public void save(int cartId) {
        if (cartManager.isEmpty()) return;
        List<PosItem> savedItems = cartManager.getItems().stream().map(PosItem::new).toList();
        holdCarts.put(cartId, savedItems);
        cartManager.clear();
    }
    
    // 카트 불러오기
    public void resume(int cartId) {
        List<PosItem> itemsToResume = holdCarts.remove(cartId);
        if (itemsToResume != null) {
            cartManager.clear();
            cartManager.getItems().addAll(itemsToResume);
        }
    }

    // 카트에 물건이 있는지 확인 (버튼 색상 제어용)
    public boolean hasItems(int cartId) {
        return holdCarts.containsKey(cartId) && !holdCarts.get(cartId).isEmpty();
    }
}