package com.swna.javafx.pos.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SaleRequest(
    @NotEmpty(message = "At least one sale item is required.")
    @Valid
    List<SaleItemRequest> items,

    @NotEmpty(message = "At least one payment record is required.")
    @Valid
    List<PaymentRequest> payments
) {}