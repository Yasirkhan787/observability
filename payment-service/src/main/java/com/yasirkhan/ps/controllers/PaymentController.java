package com.yasirkhan.ps.controllers;

import com.yasirkhan.ps.dtos.PaymentRequest;
import com.yasirkhan.ps.dtos.PaymentResponse;
import com.yasirkhan.ps.services.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    // order-service calls this. The incoming "traceparent" header is read automatically,
    // so this request's span joins the SAME trace as the caller's span.
    @PostMapping
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        return service.process(request);
    }
}
