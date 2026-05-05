package com.swna.javafx.pos.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PaymentRequest(

    @NotBlank(message = "Payment type is required. (e.g., CASH, CARD)")
    String type,

    @NotNull(message = "Payment amount is required.")
    @PositiveOrZero(message = "Amount must be zero or positive.")
    BigDecimal amount, // Actual portion of the total amount being paid

    @PositiveOrZero(message = "Received amount must be zero or positive.")
    BigDecimal receivedAmount, // Total cash handed over by the customer

    @PositiveOrZero(message = "Cash-out amount must be zero or positive.")
    BigDecimal cashOutAmount, // Extra cash requested during card transaction

    String approvalNo // Card authorization number

) {}
