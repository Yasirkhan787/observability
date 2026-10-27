package com.yasirkhan.ps.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Knobs that make this fake payment gateway misbehave on purpose,
 * so you have something interesting to find in a trace.
 * Change them in application.yaml (restart to apply).
 */
@ConfigurationProperties(prefix = "payment.simulation")
public record SimulationProperties(
        @DefaultValue("50") long minDelayMs, // fastest the "gateway" answers
        @DefaultValue("200") long maxDelayMs, // slowest normal answer
        @DefaultValue("0.2") double failureRate, // 0.0 = never, 0.2 = 20% of calls fail with 503
        @DefaultValue("5000") BigDecimal declineAbove // amounts above this are DECLINED (business rule)
) {
}
