package com.yasirkhan.ps.dtos;

import java.time.Instant;

/** What payment-service answers. status is APPROVED or DECLINED. */
public record PaymentResponse(
        String paymentId,
        Long orderId,
        String status,
        String reason,        // filled when DECLINED, otherwise null
        Instant processedAt) {}
