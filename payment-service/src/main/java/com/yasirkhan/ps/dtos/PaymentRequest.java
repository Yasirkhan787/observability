package com.yasirkhan.ps.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** What order-service sends to ask for a payment. */
public record PaymentRequest(
        @NotNull Long orderId,
        @NotBlank String customerId,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {}
