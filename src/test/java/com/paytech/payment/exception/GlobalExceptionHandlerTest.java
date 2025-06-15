package com.paytech.payment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private Model model;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        model = mock(Model.class);
    }

    @Test
    void handlePaymentException_ReturnsPaymentErrorView() {
        // Given
        PaymentException exception = new PaymentException("API error", "response body");

        // When
        String viewName = handler.handlePaymentException(exception, model);

        // Then
        assertEquals("payment-error", viewName);
    }

    @Test
    void handleWebClientException_ReturnsPaymentErrorView() {
        // Given
        WebClientResponseException exception = WebClientResponseException.create(
            BAD_REQUEST.value(),
            "Bad Request",
            null,
            "response body".getBytes(),
            null
        );

        // When
        String viewName = handler.handleWebClientException(exception, model);

        // Then
        assertEquals("payment-error", viewName);
    }

    @Test
    void handleGenericException_ReturnsPaymentErrorView() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        String viewName = handler.handleGenericException(exception, model);

        // Then
        assertEquals("payment-error", viewName);
    }
}