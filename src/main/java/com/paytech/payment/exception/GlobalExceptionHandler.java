package com.paytech.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public String handlePaymentException(PaymentException ex, Model model) {
        log.error("Payment exception occurred: {}", ex.getMessage());
        return "payment-error";
    }

    @ExceptionHandler(WebClientResponseException.class)
    public String handleWebClientException(WebClientResponseException ex, Model model) {
        log.error("API call failed with status: {} and body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return "payment-error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        return "payment-error";
    }

}