package com.swna.javafx.domain.barcode;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Domain Model
 *
 * 라벨 출력용 상품 모델
 */
@Getter
@ToString
@Builder
public class ProductLabel {

    /**
     * 바코드 번호
     */
    private final String barcode;

    /**
     * 상품명
     */
    private final String description;

    /**
     * 가격
     */
    private final Integer price;

    /**
     * 생성 시 유효성 검증
     */
    public ProductLabel(
            String barcode,
            String description,
            Integer price
    ) {

        validateCode(barcode);
        validateName(description);
        validatePrice(price);

        this.barcode = barcode;
        this.description = description;
        this.price = price;
    }

    /**
     * 바코드 검증
     */
    private void validateCode(String barcode) {

        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException(  "Barcode code is required" );
        }

        if (barcode.length() > 30) {
            throw new IllegalArgumentException( "Barcode code too long" );
        }
    }

    /**
     * 상품명 검증
     */
    private void validateName(String description) {

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(  "Product name is required" );
        }

        if (description.length() > 100) {
            throw new IllegalArgumentException( "Product name too long"  );
        }
    }

    /**
     * 가격 검증
     */
    private void validatePrice(Integer price) {

        if (price == null) {
            throw new IllegalArgumentException( "Price is required"  );
        }

        if (price < 0) {
            throw new IllegalArgumentException( "Price must be positive" );
        }
    }

    /**
     * 표시용 가격 문자열
     */
    public String displayPrice() { return "$" + String.format("%,d", price);
    }

    /**
     * 바코드 표시 가능 여부
     */
    public boolean printable() {
        return barcode != null && !barcode.isBlank();
    }

    /**
     * equals/hashCode
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {  return true;}
        if (!(o instanceof ProductLabel that)) {return false;}
        return Objects.equals(barcode, that.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }
}