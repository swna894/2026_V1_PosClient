package com.swna.javafx.pos.dto.response;

public record ProductResponse(
        String code,
        String barcode,
        String description,
        double sellingPrice,
        double originalPrice,
        int stock
) {}