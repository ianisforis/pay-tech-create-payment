package com.paytech.payment.validator;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ValidationHelper {

    public static String handleValidationErrors(BindingResult bindingResult, String view) {
        if (bindingResult.hasErrors()) {
            log.warn("Payment form validation failed: {}", bindingResult.getAllErrors());
            return view;
        }
        return null;
    }
}