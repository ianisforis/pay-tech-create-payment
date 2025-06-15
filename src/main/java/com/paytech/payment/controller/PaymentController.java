package com.paytech.payment.controller;

import com.paytech.payment.dto.PaymentRequest;
import com.paytech.payment.service.PaymentService;
import com.paytech.payment.validator.ValidationHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.paytech.payment.constant.Constants.PAYMENT_FORM;
import static com.paytech.payment.constant.Constants.REDIRECT;
import static com.paytech.payment.validator.ValidationHelper.handleValidationErrors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    
    @GetMapping
    public String showPaymentForm(Model model) {
        String idempotencyKey = paymentService.generateIdempotencyKey();
        model.addAttribute("paymentRequest", new PaymentRequest(null, idempotencyKey));
        return PAYMENT_FORM;
    }
    
    @PostMapping
    public String processPayment(@Valid @ModelAttribute PaymentRequest paymentRequest, BindingResult bindingResult) {
        return REDIRECT + paymentService.processPayment(paymentRequest, bindingResult);
    }
}