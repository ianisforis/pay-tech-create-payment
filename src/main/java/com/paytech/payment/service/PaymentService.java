package com.paytech.payment.service;

import com.paytech.payment.dto.PaymentRequest;
import org.springframework.validation.BindingResult;

public interface PaymentService {

    String processPayment(PaymentRequest paymentRequest, BindingResult bindingResult);
    
    String generateIdempotencyKey();
}