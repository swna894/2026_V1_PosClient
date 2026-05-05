package com.swna.javafx.barcode.infrastructre;

import java.math.BigDecimal;

public record ProductLabelDto( String barcode, String description, BigDecimal price) {}