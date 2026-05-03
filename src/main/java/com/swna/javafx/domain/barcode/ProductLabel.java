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
    private final String code;

    /**
     * 상품명
     */
    private final String name;

    /**
     * 가격
     */
    private final Integer price;

    /**
     * 생성 시 유효성 검증
     */
    public ProductLabel(
            String code,
            String name,
            Integer price
    ) {

        validateCode(code);
        validateName(name);
        validatePrice(price);

        this.code = code;
        this.name = name;
        this.price = price;
    }

    /**
     * 바코드 검증
     */
    private void validateCode(String code) {

        if (code == null || code.isBlank()) {

            throw new IllegalArgumentException(
                    "Barcode code is required"
            );
        }

        if (code.length() > 30) {

            throw new IllegalArgumentException(
                    "Barcode code too long"
            );
        }
    }

    /**
     * 상품명 검증
     */
    private void validateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Product name is required"
            );
        }

        if (name.length() > 100) {

            throw new IllegalArgumentException(
                    "Product name too long"
            );
        }
    }

    /**
     * 가격 검증
     */
    private void validatePrice(Integer price) {

        if (price == null) {

            throw new IllegalArgumentException(
                    "Price is required"
            );
        }

        if (price < 0) {

            throw new IllegalArgumentException(
                    "Price must be positive"
            );
        }
    }

    /**
     * 표시용 가격 문자열
     */
    public String displayPrice() {

        return "₩" + String.format("%,d", price);
    }

    /**
     * 바코드 표시 가능 여부
     */
    public boolean printable() {

        return code != null
                && !code.isBlank();
    }

    /**
     * equals/hashCode
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof ProductLabel that)) {
            return false;
        }

        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {

        return Objects.hash(code);
    }
}