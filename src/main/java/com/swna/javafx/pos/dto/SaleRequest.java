package com.swna.javafx.pos.dto;

import java.util.List;


public record SaleRequest(
        List<SaleItemRequestDto> items

) {}