package com.yasirkhan.ps.handlers;

import com.yasirkhan.ps.exceptions.PaymentGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Extends ResponseEntityExceptionHandler so Spring's own errors (validation = 400,
 * wrong method = 405, unknown path = 404) keep their correct status codes.
 * We only add our own exception on top.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PaymentGatewayException.class)
    ProblemDetail gatewayDown(PaymentGatewayException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());   // 503
    }
}
