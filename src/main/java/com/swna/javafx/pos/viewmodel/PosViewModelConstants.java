// PosViewModelConstants.java
package com.swna.javafx.pos.viewmodel;

public final class PosViewModelConstants {
    
    private PosViewModelConstants() {}
    
    // ========== 상태 메시지 ==========
    public static final String STATUS_READY = "Scan ready";
    public static final String STATUS_SCANNING = "Scanning...";
    public static final String STATUS_SCAN_SUCCESS = "Scan successful ✓ Code: %s";
    public static final String STATUS_ITEM_NOT_FOUND = "Item not found ❌";
    public static final String STATUS_SEARCH_FAILED = "Search failed ❌";
    public static final String STATUS_QUICK_ADD = "Add Quick Item : $%.2f";
    public static final String STATUS_HOLD_SAVED = "Cart saved";
    public static final String STATUS_HOLD_NO_ITEMS = "No items to hold";
    public static final String STATUS_HOLD_RESUMED = "Cart resumed";
    public static final String STATUS_HOLD_NO_CART = "No hold cart";
    public static final String STATUS_PAYMENT_SUCCESS = "Payment success ✓";
    public static final String STATUS_PAYMENT_FAIL = "Payment failed ❌";
    public static final String STATUS_INVALID_AMOUNT = "Invalid payment amounts";
    public static final String STATUS_INSUFFICIENT_CASH = "Insufficient cash received";
    public static final String STATUS_AMOUNT_MISMATCH = "Amount mismatch";
    
    // ========== 결제 타입 ==========
    public static final String PAY_CASH = "CASH";
    public static final String PAY_CARD = "CARD";
    
    // ========== 결제 설명 ==========
    public static final String PAYMENT_DESC_CASH = "Cash";
    public static final String PAYMENT_DESC_CASHOUT = "Cashout";
    public static final String PAYMENT_DESC_MIXED = "Mixed";
    
    // ========== 포맷 ==========
    public static final String CASHOUT_REF_PREFIX = "CASHOUT_";
}