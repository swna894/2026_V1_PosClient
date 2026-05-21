package com.swna.javafx.pos.model;

import java.time.LocalDateTime;

import com.swna.javafx.pos.dto.request.DiscountType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.ToString;

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
@ToString
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
    
    // ✅ 추가: 할인 유형 필드 (NONE, AMOUNT, PERCENT)
    private final ObjectProperty<DiscountType> discountType = new SimpleObjectProperty<>(DiscountType.NONE);

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

    public PosItem(PosItem source) {
        // 1. 상태 값 복사
        this.id.set(source.getId());
        this.code.set(source.getCode());
        this.barcode.set(source.getBarcode());
        this.description.set(source.getDescription());
        this.qty.set(source.getQty());
        this.stock.set(source.getStock());
        this.originalPrice.set(source.getOriginalPrice());
        this.sellingPrice.set(source.getSellingPrice());
        this.unitDiscount.set(source.getUnitDiscount());
        this.discountRate.set(source.getDiscountRate());
        this.discountAmount.set(source.getDiscountAmount());
        this.comment.set(source.getComment());
        this.updated.set(source.getUpdated());
        
        // ✅ 할인 유형 복사
        this.discountType.set(source.getDiscountType());

        // 2. [핵심] 복구 후 액션(가격변경/할인) 시 계산 로직이 작동하도록 바인딩 재연결
        initBindings(); 
        initCommentBinding();
    }

    /**
     * Quick/Open Item 생성
     */
    public static PosItem createQuickItem(String manualBarcodePrefix, double amount) {
        PosItem item = new PosItem();
        item.setBarcode(String.format("%s_%.2f", manualBarcodePrefix, amount));
        item.setDescription(String.format("Temporary Item ($%.2f)", amount));
        item.setOriginalPrice(amount);
        item.setSellingPrice(amount);
        item.setDiscountType(DiscountType.NONE);  // ✅ 할인 없음으로 설정
        item.increaseQty();
        return item;
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
        finalAmount.bind(sellingAmount);
    }

    /**
     * 단가 할인 정보를 comment에 자동으로 표시하는 바인딩
     * 가격 변경 시 기존 가격과 새 가격을 comment에 자동으로 표시하는 바인딩
     */
    private void initCommentBinding() {
        comment.bind(Bindings.createStringBinding(
            () -> {
                double original = getOriginalPrice();
                double current = getSellingPrice();
                double uDiscount = getUnitDiscount();
                DiscountType type = getDiscountType();

                // 1. 단가 할인(D/C)이 적용된 경우 우선 표시
                if (uDiscount > 0 && type == DiscountType.AMOUNT) {
                    return String.format("D/C: $%.2f/ea", uDiscount);
                }
                
                // 2. 퍼센트 할인이 적용된 경우
                if (type == DiscountType.PERCENT && getDiscountRate() > 0) {
                    return String.format("%.0f%% OFF", getDiscountRate());
                }
                
                // 3. 정가와 현재 판매가가 다른 경우 (가격 변경 발생)
                else if (Double.compare(original, current) != 0 && type == DiscountType.NONE) {
                    return String.format("Changed: $%.2f → $%.2f", original, current);
                }

                // 4. 변경 사항이 없는 경우 빈 문자열
                return "";
            },
            originalPrice, sellingPrice, unitDiscount, discountType, discountRate
        ));
    }

    // ============================================================
    // 5. 비즈니스 로직 (DiscountType 자동 설정)
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

    /**
     * 할인 적용 (퍼센트 또는 금액)
     * ✅ DiscountType 자동 설정
     */
    public void applyDiscount(double percent, double amount) {
        if (percent > 0) {
            applyPercentDiscount(percent);
        } else if (amount > 0) {
            applyAmountDiscount(amount);
        } else {
            clearDiscount();
        }
    }
    
    /**
     * 퍼센트 할인 적용
     */
    public void applyPercentDiscount(double percent) {
        if (percent <= 0) {
            clearDiscount();
            return;
        }
        
        setDiscountRate(percent);
        setDiscountAmount(0);
        
        // 판매가 = 정가 - (정가 * 할인율)
        double discountedPrice = getOriginalPrice() * (1 - (percent / 100.0));
        setSellingPrice(Math.max(0, discountedPrice));
        
        // 단가 할인액 계산 (comment 표시용)
        setUnitDiscount(getOriginalPrice() - getSellingPrice());
        
        // ✅ 할인 유형 설정
        setDiscountType(DiscountType.PERCENT);
    }
    
    /**
     * 금액 할인 적용
     */
    public void applyAmountDiscount(double amount) {
        if (amount <= 0) {
            clearDiscount();
            return;
        }
        
        setDiscountAmount(amount);
        setDiscountRate(0);
        
        // 판매가 = 정가 - 할인액
        double discountedPrice = getOriginalPrice() - amount;
        setSellingPrice(Math.max(0, discountedPrice));
        
        // 단가 할인액 설정
        setUnitDiscount(amount);
        
        // ✅ 할인 유형 설정
        setDiscountType(DiscountType.AMOUNT);
    }
    
    /**
     * 단가 기준 할인 적용 (개당 할인)
     * ✅ DiscountType을 AMOUNT로 설정
     */
    public void applyUnitDiscount(double unitDiscountAmount) {
        if (unitDiscountAmount <= 0) {
            clearDiscount();
            return;
        }
        
        setUnitDiscount(unitDiscountAmount);
        setDiscountRate(0);
        setDiscountAmount(0);
        
        // 판매가 = 정가 - 단가할인
        double discountedPrice = getOriginalPrice() - unitDiscountAmount;
        setSellingPrice(Math.max(0, discountedPrice));
        
        // ✅ 할인 유형 설정
        setDiscountType(DiscountType.AMOUNT);
    }
    
    /**
     * 할인 초기화
     * ✅ DiscountType을 NONE으로 설정
     */
    public void clearDiscount() {
        setDiscountRate(0);
        setDiscountAmount(0);
        setUnitDiscount(0);
        setSellingPrice(getOriginalPrice());
        setDiscountType(DiscountType.NONE);
    }
    
    /**
     * 가격 직접 변경 (할인 초기화)
     */
    public void changePrice(double newPrice) {
        if (newPrice <= 0) return;
        
        // 할인 정보 초기화
        clearDiscount();
        
        // 새 가격 설정
        setSellingPrice(newPrice);
        setOriginalPrice(newPrice);
    }
    
    /**
     * 할인 여부 확인
     */
    public boolean hasDiscount() {
        return getDiscountType() != DiscountType.NONE && getUnitDiscount() > 0;
    }
    
    /**
     * 퍼센트 할인인지 확인
     */
    public boolean isPercentDiscount() {
        return getDiscountType() == DiscountType.PERCENT;
    }
    
    /**
     * 금액 할인인지 확인
     */
    public boolean isAmountDiscount() {
        return getDiscountType() == DiscountType.AMOUNT;
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
    
    // ✅ DiscountType Getter/Setter
    public DiscountType getDiscountType() { return discountType.get(); }
    public void setDiscountType(DiscountType v) { discountType.set(v != null ? v : DiscountType.NONE); }
    public ObjectProperty<DiscountType> discountTypeProperty() { return discountType; }

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