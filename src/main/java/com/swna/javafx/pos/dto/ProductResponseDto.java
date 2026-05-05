package com.swna.javafx.pos.dto;

public record ProductResponseDto(
        String code,
        String barcode,
        String description,
        double sellingPrice,
        double originalPrice,
        int stock
) {}