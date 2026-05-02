package com.swna.javafx.domain.pos;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * POS 아이템 도메인 클래스
 * TableColumnUtil과의 호환성을 위해 계산 필드를 DoubleProperty로 변경하고
 * 초기화 시 bind()를 통해 읽기 전용 로직을 구현했습니다.
 * 
 * // 초기: 단가 $100, 수량 1, 단가할인 $20
    // discountTotal = $20 × 1 = $20
    // finalAmount = $100 - $20 = $80

    // increaseQty() 호출: 수량 2로 증가
    // discountTotal = $20 × 2 = $40 (자동 증가)
    // finalAmount = $200 - $40 = $160 (자동 계산)

    // decreaseQty() 호출: 수량 1로 감소  
    // discountTotal = $20 × 1 = $20 (자동 감소)
    // finalAmount = $100 - $20 = $80 (자동 계산)
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
    private final DoubleProperty discountAmount = new SimpleDoubleProperty(0); // 고정 할인액 (총액 기준)
    private final DoubleProperty discountRate = new SimpleDoubleProperty(0);   // 할인율 (%)
    private final DoubleProperty unitDiscount = new SimpleDoubleProperty(0);   // 단가 기준 할인액 (개당 할인)

    // ============================================================
    // 3. 자동 계산 결과 필드 (SimpleDoubleProperty로 변경)
    // ============================================================
    // TableColumnUtil의 ClassCastException을 방지하기 위해 DoubleProperty 타입을 사용합니다.
    //===================================
    // originalAmount = 정가 × 수량
    // sellingAmount = 판매가 × 수량
    // discountTotal = (판매가 × 수량 × 할인율%) + 고정할인액 + (단가할인 × 수량)
    // finalAmount = sellingAmount - discountTotal
    //===================================
    private final DoubleProperty originalAmount = new SimpleDoubleProperty();
    private final DoubleProperty sellingAmount = new SimpleDoubleProperty();
    private final DoubleProperty discountTotal = new SimpleDoubleProperty();
    private final DoubleProperty finalAmount = new SimpleDoubleProperty();

    // ============================================================
    // 4. 생성자 및 바인딩 설정
    // ============================================================
    public PosItem() {
        initBindings();
        initCommentBinding();
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

        // 3. 할인 총액 ( (판매가 * 수량 * 할인율) + 고정 할인액 + (단가할인 * 수량) )
        discountTotal.bind(Bindings.createDoubleBinding(
                () -> (getSellingPrice() * getQty() * (getDiscountRate() / 100.0)) 
                      + getDiscountAmount() 
                      + (getUnitDiscount() * getQty()),
                sellingPrice, qty, discountRate, discountAmount, unitDiscount
        ));

        // 4. 최종 금액 (판매가 합계 - 할인 총액)
        finalAmount.bind(Bindings.createDoubleBinding(
                () -> getSellingAmount() - getDiscountTotal(),
                sellingAmount, discountTotal
        ));
    }

    /**
     * 단가 할인 정보를 comment에 자동으로 표시하는 바인딩
     */
/**
     * 가격 변경 시 기존 가격과 새 가격을 comment에 자동으로 표시하는 바인딩
     */
    private void initCommentBinding() {
        // originalPrice, sellingPrice, unitDiscount 중 하나라도 변경되면 업데이트
        comment.bind(Bindings.createStringBinding(
            () -> {
                double original = getOriginalPrice();
                double current = getSellingPrice();
                double uDiscount = getUnitDiscount();

                // 1. 단가 할인(D/C)이 적용된 경우 우선 표시
                if (uDiscount > 0) {
                    return String.format("D/C: -$%.2f/ea", uDiscount);
                } 
                
                // 2. 정가와 현재 판매가가 다른 경우 (가격 변경 발생)
                else if (Double.compare(original, current) != 0) {
                    // 원래 가격(original)을 포함하여 변경 이력을 표시
                    return String.format("Changed: $%.2f → $%.2f", original, current);
                }

                // 3. 변경 사항이 없는 경우 빈 문자열
                return "";
            },
            originalPrice, sellingPrice, unitDiscount
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
    
    /**
     * 단가 기준 할인 적용 (개당 할인)
     */
    public void applyUnitDiscount(double unitDiscountAmount) {
        setUnitDiscount(unitDiscountAmount);
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
    
    public double getUnitDiscount() { return unitDiscount.get(); }
    public void setUnitDiscount(double v) { unitDiscount.set(v); }
    public DoubleProperty unitDiscountProperty() { return unitDiscount; }

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