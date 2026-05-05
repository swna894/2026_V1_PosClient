package com.swna.javafx.pos.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DiscountRequest(
    @NotNull(message = "Discount type is required.")
    DiscountType type,

    @NotNull(message = "Discount value is required.")
    @PositiveOrZero(message = "Discount value must be 0 or greater.")
    BigDecimal value,

    String reason
) {
    
    public static DiscountRequest percent(BigDecimal value, String reason) {
        return new DiscountRequest(DiscountType.PERCENT, value, reason);
    }
    
    public static DiscountRequest fixed(BigDecimal value, String reason) {
        return new DiscountRequest(DiscountType.AMOUNT, value, reason);
    }
    
    public boolean isPercent() {
        return type == DiscountType.PERCENT;
    }
    
    public boolean isFixed() {
        return type == DiscountType.AMOUNT;
    }

       public Discount toDomain() {
        return Discount.builder().type(type).value(value) .reason(reason).build();
    }
    
}