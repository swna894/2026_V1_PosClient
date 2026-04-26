package com.swna.javafx.domain.pos;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;

import java.time.LocalDateTime;

public class PosItem {

    // =========================
    // 기본 정보
    // =========================
    private final LongProperty id = new SimpleLongProperty();

    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    // =========================
    // 가격
    // =========================
    private final DoubleProperty originalPrice = new SimpleDoubleProperty(0); // 정가
    private final DoubleProperty sellingPrice = new SimpleDoubleProperty(0);  // 판매가

    // =========================
    // 수량 / 재고
    // =========================
    private final IntegerProperty qty = new SimpleIntegerProperty(0);
    private final IntegerProperty stock = new SimpleIntegerProperty(0);

    // =========================
    // 할인
    // =========================
    private final DoubleProperty discountAmount = new SimpleDoubleProperty(0); // 고정 할인
    private final DoubleProperty discountRate = new SimpleDoubleProperty(0);   // %

    // =========================
    // 시간
    // =========================
    private final ObjectProperty<LocalDateTime> updated = new SimpleObjectProperty<>();

    // =========================
    // 계산 Binding (🔥 핵심)
    // =========================

    // 1️⃣ 정가 기준 합계
    private final DoubleBinding originalAmount = originalPrice.multiply(qty);

    // 2️⃣ 판매가 기준 합계
    private final DoubleBinding sellingAmount = sellingPrice.multiply(qty);

    // 3️⃣ 할인 계산
    private final DoubleBinding discountTotal =
            Bindings.createDoubleBinding(() ->
                            (sellingPrice.get() * qty.get()) * (discountRate.get() / 100.0)
                                    + discountAmount.get(),
                    sellingPrice, qty, discountRate, discountAmount
            );

    // 4️⃣ 최종 금액
    private final DoubleBinding finalAmount =
 sellingAmount.subtract(discountTotal);

    // =========================
    // 비즈니스 로직
    // =========================
    public void increaseQty() {
        qty.set(qty.get() + 1);
        stock.set(stock.get() - 1);
    }

    public void decreaseQty() {
        if (qty.get() <= 0) return;
        qty.set(qty.get() - 1);
        stock.set(stock.get() + 1);
    }

    public void applyDiscount(double percent, double amount) {
        discountRate.set(percent);
        discountAmount.set(amount);
    }

    // =========================
    // Getter / Setter / Property
    // =========================

    // ID
    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    // 기본 정보
    public String getCode() { return code.get(); }
    public void setCode(String v) { code.set(v); }
    public StringProperty codeProperty() { return code; }

    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String v) { barcode.set(v); }
    public StringProperty barcodeProperty() { return barcode; }

    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }
    public StringProperty descriptionProperty() { return description; }

    // 가격
    public double getOriginalPrice() { return originalPrice.get(); }
    public void setOriginalPrice(double v) { originalPrice.set(v); }
    public DoubleProperty originalPriceProperty() { return originalPrice; }

    public double getSellingPrice() { return sellingPrice.get(); }
    public void setSellingPrice(double v) { sellingPrice.set(v); }
    public DoubleProperty sellingPriceProperty() { return sellingPrice; }

    // 수량
    public int getQty() { return qty.get(); }
    public void setQty(int v) { qty.set(v); }
    public IntegerProperty qtyProperty() { return qty; }

    public int getStock() { return stock.get(); }
    public void setStock(int v) { stock.set(v); }
    public IntegerProperty stockProperty() { return stock; }

    // 할인
    public double getDiscountAmount() { return discountAmount.get(); }
    public DoubleProperty discountAmountProperty() { return discountAmount; }

    public double getDiscountRate() { return discountRate.get(); }
    public DoubleProperty discountRateProperty() { return discountRate; }

    // =========================
    // 계산 결과 Property
    // =========================
    public DoubleBinding originalAmountProperty() {
        return originalAmount;
    }

    public DoubleBinding sellingAmountProperty() {
        return sellingAmount;
    }

    public DoubleBinding discountTotalProperty() {
        return discountTotal;
    }

    public DoubleBinding finalAmountProperty() {
        return finalAmount;
    }

    // =========================
    // 기타
    // =========================
    public LocalDateTime getUpdated() { return updated.get(); }
    public void setUpdated(LocalDateTime v) { updated.set(v); }
    public ObjectProperty<LocalDateTime> updatedProperty() { return updated; }
}