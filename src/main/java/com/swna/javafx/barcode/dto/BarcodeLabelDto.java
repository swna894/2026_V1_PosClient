package com.swna.javafx.barcode.dto;

import java.math.BigDecimal;

public record BarcodeLabelDto( String barcode, String description, BigDecimal price) {}