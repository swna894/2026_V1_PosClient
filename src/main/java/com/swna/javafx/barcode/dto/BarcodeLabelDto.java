package com.swna.javafx.barcode.dto;

import java.math.BigDecimal;

public record BarcodeLabelDto( Long id, String barcode, String company, String code, String description, BigDecimal price) {}