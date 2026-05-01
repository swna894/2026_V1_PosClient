package com.swna.javafx.domain.pos;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * POS 아이템 도메인 클래스
 * TableColumnUtil과의 호환성을 위해 계산 필드를 DoubleProperty로 변경하고
 * 초기화 시 bind()를 통해 읽기 전용 로직을 구현했습니다.
 */
public class PosItem {

    // ============================================================
    // 1. 기본 정보 필드
    // ============================================================
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty comment = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> updated = new SimpleObjectProperty<>();

    // ============================================================
    // 2. 가격 및 수량 필드 (입력 값)
    // ============================================================
    private final DoubleProperty originalPrice = new SimpleDoubleProperty(0); // 정가
    private final DoubleProperty sellingPrice = new SimpleDoubleProperty(0);  // 판매가
    private final IntegerProperty qty = new SimpleIntegerProperty(0);         // 수량
    private final IntegerProperty stock = new SimpleIntegerProperty(0);       // 재고
    private final DoubleProperty discountAmount = new SimpleDoubleProperty(0); // 고정 할인액
    private final DoubleProperty discountRate = new SimpleDoubleProperty(0);   // 할인율 (%)

    // ============================================================
    // 3. 자동 계산 결과 필드 (SimpleDoubleProperty로 변경)
    // ============================================================
    // TableColumnUtil의 ClassCastException을 방지하기 위해 DoubleProperty 타입을 사용합니다.
    private final DoubleProperty originalAmount = new SimpleDoubleProperty();
    private final DoubleProperty sellingAmount = new SimpleDoubleProperty();
    private final DoubleProperty discountTotal = new SimpleDoubleProperty();
    private final DoubleProperty finalAmount = new SimpleDoubleProperty();

    // ============================================================
    // 4. 생성자 및 바인딩 설정
    // ============================================================
    public PosItem() {
        initBindings();
    }

    /**
     * 각 계산 필드에 대한 바인딩 로직을 정의합니다.
     * bind()가 호출된 Property는 외부에서 set()을 호출할 경우 예외가 발생하므로 
     * 논리적으로 읽기 전용 상태가 됩니다.
     */
    private void initBindings() {
        // 1. 정가 기준 합계 (정가 * 수량)
        originalAmount.bind(Bindings.createDoubleBinding(
                () -> getOriginalPrice() * getQty(),
                originalPrice, qty
        ));

        // 2. 판매가 기준 합계 (판매가 * 수량)
        sellingAmount.bind(Bindings.createDoubleBinding(
                () -> getSellingPrice() * getQty(),
                sellingPrice, qty
        ));

        // 3. 할인 총액 ( (판매가 * 수량 * 할인율) + 고정 할인액 )
        discountTotal.bind(Bindings.createDoubleBinding(
                () -> (getSellingPrice() * getQty() * (getDiscountRate() / 100.0)) + getDiscountAmount(),
                sellingPrice, qty, discountRate, discountAmount
        ));

        // 4. 최종 금액 (판매가 합계 - 할인 총액)
        finalAmount.bind(Bindings.createDoubleBinding(
                () -> getSellingAmount() - getDiscountTotal(),
                sellingAmount, discountTotal
        ));
    }

    // ============================================================
    // 5. 비즈니스 로직
    // ============================================================
    public void increaseQty() {
        setQty(getQty() + 1);
        setStock(getStock() - 1);
    }

    public void decreaseQty() {
        if (getQty() <= 0) return;
        setQty(getQty() - 1);
        setStock(getStock() + 1);
    }

    public void applyDiscount(double percent, double amount) {
        setDiscountRate(percent);
        setDiscountAmount(amount);
    }

    // ============================================================
    // 6. Getter / Setter / Property 메서드
    // ============================================================

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public String getCode() { return code.get(); }
    public void setCode(String v) { code.set(v); }
    public StringProperty codeProperty() { return code; }

    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String v) { barcode.set(v); }
    public StringProperty barcodeProperty() { return barcode; }

    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }
    public StringProperty descriptionProperty() { return description; }

    public String getComment() { return comment.get(); }
    public void setComment(String v) { comment.set(v); }
    public StringProperty commentProperty() { return comment; }

    public double getOriginalPrice() { return originalPrice.get(); }
    public void setOriginalPrice(double v) { originalPrice.set(v); }
    public DoubleProperty originalPriceProperty() { return originalPrice; }

    public double getSellingPrice() { return sellingPrice.get(); }
    public void setSellingPrice(double v) { sellingPrice.set(v); }
    public DoubleProperty sellingPriceProperty() { return sellingPrice; }

    public int getQty() { return qty.get(); }
    public void setQty(int v) { qty.set(v); }
    public IntegerProperty qtyProperty() { return qty; }

    public int getStock() { return stock.get(); }
    public void setStock(int v) { stock.set(v); }
    public IntegerProperty stockProperty() { return stock; }

    public double getDiscountAmount() { return discountAmount.get(); }
    public void setDiscountAmount(double v) { discountAmount.set(v); }
    public DoubleProperty discountAmountProperty() { return discountAmount; }

    public double getDiscountRate() { return discountRate.get(); }
    public void setDiscountRate(double v) { discountRate.set(v); }
    public DoubleProperty discountRateProperty() { return discountRate; }

    public LocalDateTime getUpdated() { return updated.get(); }
    public void setUpdated(LocalDateTime v) { updated.set(v); }
    public ObjectProperty<LocalDateTime> updatedProperty() { return updated; }

    // ============================================================
    // 7. 계산 결과 Property (DoubleProperty 반환)
    // ============================================================
    // 반환 타입을 DoubleProperty로 통일하여 TableColumnUtil에서 메서드 참조 시 에러를 방지합니다.

    public double getOriginalAmount() { return originalAmount.get(); }
    public DoubleProperty originalAmountProperty() { return originalAmount; }

    public double getSellingAmount() { return sellingAmount.get(); }
    public DoubleProperty sellingAmountProperty() { return sellingAmount; }

    public double getDiscountTotal() { return discountTotal.get(); }
    public DoubleProperty discountTotalProperty() { return discountTotal; }

    public double getFinalAmount() { return finalAmount.get(); }
    public DoubleProperty finalAmountProperty() { return finalAmount; }
}