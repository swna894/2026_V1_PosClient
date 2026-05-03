package com.swna.javafx.infrastructure.barcode;

import java.math.BigDecimal;

public record ProductLabelDto( String barcode, String description, BigDecimal price) {}