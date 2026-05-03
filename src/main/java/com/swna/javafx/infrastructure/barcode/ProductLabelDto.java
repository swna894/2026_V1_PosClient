package com.swna.javafx.infrastructure.barcode;


public record ProductLabelDto(
        String code,
        String name,
        Integer price
) {
}