package com.yasirkhan.ps.services;

import com.yasirkhan.ps.configs.SimulationProperties;
import com.yasirkhan.ps.dtos.PaymentRequest;
import com.yasirkhan.ps.dtos.PaymentResponse;
import com.yasirkhan.ps.exceptions.PaymentGatewayException;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    private final SimulationProperties sim;
    private final ObservationRegistry observationRegistry;

    public PaymentService(SimulationProperties sim, ObservationRegistry observationRegistry) {
        this.sim = sim;
        this.observationRegistry = observationRegistry;
    }

    public PaymentResponse process(PaymentRequest req) {
        // One Observation = one span in Tempo (child of the HTTP span) + one timer
        // metric in Prometheus.
        return Observation.createNotStarted("payment.process", observationRegistry)
                .lowCardinalityKeyValue("gateway", "simulated") // few values: safe as a metric label AND span attribute
                .highCardinalityKeyValue("order.id", String.valueOf(req.orderId())) // many values: span attribute only,
                                                                                    // never a metric label
                .observe(() -> callGateway(req));
    }

    // Pretends to talk to an external bank. This is the work that shows up as the
    // "slow part" in a trace.
    private PaymentResponse callGateway(PaymentRequest req) {
        sleep(ThreadLocalRandom.current().nextLong(sim.minDelayMs(), sim.maxDelayMs() + 1));

        // Technical failure: becomes an ERROR span and an HTTP 503
        if (ThreadLocalRandom.current().nextDouble() < sim.failureRate()) {
            log.warn("Gateway failure for order {}", req.orderId());
            throw new PaymentGatewayException("Payment gateway unavailable (simulated)");
        }

        String paymentId = UUID.randomUUID().toString();

        // Business decision: a normal answer, NOT an error
        if (req.amount().compareTo(sim.declineAbove()) > 0) {
            log.info("Payment {} DECLINED for order {} (amount {})", paymentId, req.orderId(), req.amount());
            return new PaymentResponse(paymentId, req.orderId(), "DECLINED", "Amount over limit", Instant.now());
        }

        log.info("Payment {} APPROVED for order {} (amount {})", paymentId, req.orderId(), req.amount());
        return new PaymentResponse(paymentId, req.orderId(), "APPROVED", null, Instant.now());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
