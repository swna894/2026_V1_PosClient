package com.swna.javafx.dto.pos;

public record ProductResponse(
        String code,
        String barcode,
        String description,
        double sellingPrice,
        double originalPrice,
        int stock
) {}