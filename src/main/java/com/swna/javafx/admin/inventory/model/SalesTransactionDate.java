package com.swna.javafx.admin.inventory.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SalesTransactionDate {

    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final IntegerProperty count = new SimpleIntegerProperty();

    public SalesTransactionDate(String barcode, String date, Integer count) {
        setBarcode(barcode);
        setDate(date);
        setCount(count != null ? count : 0);
    }

    /**
     * API Response DTO를 UI 바인딩용 SalesData로 변환
     */
    public static SalesTransactionDate fromResponse(SalesTransationResponse response) {
        if (response == null) return new SalesTransactionDate("", "", 0);
        return new SalesTransactionDate(
                response.getBarcode(),
                response.getDate(),
                response.getCount()
        );
    }

    // ==========================================
    // Properties & Getters/Setters
    // ==========================================
    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    public StringProperty barcodeProperty() { return barcode; }

    public String getDate() { return date.get(); }
    public void setDate(String date) { this.date.set(date); }
    public StringProperty dateProperty() { return date; }

    public int getCount() { return count.get(); }
    public void setCount(int count) { this.count.set(count); }
    public IntegerProperty countProperty() { return count; }
}