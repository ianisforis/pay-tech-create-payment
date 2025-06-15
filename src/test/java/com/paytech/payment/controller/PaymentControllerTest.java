package com.paytech.payment.controller;

import com.paytech.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void show_payment_form() throws Exception {
        when(paymentService.generateIdempotencyKey()).thenReturn("generated-key");
        
        mockMvc.perform(get("/payment"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-form"))
                .andExpect(model().attributeExists("paymentRequest"));
    }

    @Test
    void process_payment_validation_error_zero_amount() throws Exception {
        when(paymentService.processPayment(any(), any())).thenReturn("payment-form");
        
        mockMvc.perform(post("/payment")
                        .param("amount", "0")
                        .param("idempotencyKey", "test-key"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("payment-form"));
    }

    @Test
    void process_payment_validation_error_negative_amount() throws Exception {
        when(paymentService.processPayment(any(), any())).thenReturn("payment-form");
        
        mockMvc.perform(post("/payment")
                        .param("amount", "-10.50")
                        .param("idempotencyKey", "test-key"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("payment-form"));
    }

    @Test
    void process_payment_validation_error_missing_amount() throws Exception {
        when(paymentService.processPayment(any(), any())).thenReturn("payment-form");
        
        mockMvc.perform(post("/payment")
                        .param("idempotencyKey", "test-key"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("payment-form"));
    }

    @Test
    void process_payment_success() throws Exception {
        when(paymentService.processPayment(any(), any())).thenReturn("https://redirect.url");

        mockMvc.perform(post("/payment")
                        .param("amount", "100.00")
                        .param("idempotencyKey", "test-key"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://redirect.url"));
    }
}