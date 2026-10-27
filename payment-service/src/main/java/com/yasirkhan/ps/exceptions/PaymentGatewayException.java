package com.yasirkhan.ps.exceptions;

/**
 * A technical failure (gateway down, timeout). Returned to the caller as HTTP
 * 503.
 */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }
}
