package com.paytech.payment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    @Mock
    private Model model;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handle_PaymentException_returns_payment_error_view() {
        PaymentException exception = new PaymentException("API error", "response body");
        String viewName = handler.handlePaymentException(exception, model);
        assertEquals("payment-error", viewName);
    }

    @Test
    void handle_WebClientException_ReturnsPaymentErrorView() {
        WebClientResponseException exception = WebClientResponseException.create(
            BAD_REQUEST.value(),
            "Bad Request",
            null,
            "response body".getBytes(),
            null
        );
        String viewName = handler.handleWebClientException(exception, model);

        assertEquals("payment-error", viewName);
    }

    @Test
    void handle_GenericException_returns_payment_error_view() {
        Exception exception = new RuntimeException("Unexpected error");
        String viewName = handler.handleGenericException(exception, model);

        assertEquals("payment-error", viewName);
    }
}