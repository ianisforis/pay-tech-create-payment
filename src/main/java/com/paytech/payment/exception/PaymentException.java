package com.paytech.payment.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    
    private final String responseBody;

    public PaymentException(String message, String responseBody) {
        super(message);
        this.responseBody = responseBody;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.responseBody = null;
    }

    public PaymentException(String message) {
        super(message);
        this.responseBody = null;
    }
}