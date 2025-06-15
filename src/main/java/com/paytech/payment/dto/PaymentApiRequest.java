package com.paytech.payment.dto;

import java.math.BigDecimal;
import com.paytech.payment.enums.PaymentType;
import com.paytech.payment.enums.Currency;

import static com.paytech.payment.enums.PaymentType.DEPOSIT;
import static com.paytech.payment.enums.Currency.EUR;
import static java.lang.System.currentTimeMillis;

public record PaymentApiRequest(
    PaymentType paymentType,
    BigDecimal amount,
    Currency currency,
    CustomerInfo customer
) {
    public PaymentApiRequest(BigDecimal amount) {
        this(DEPOSIT, amount, EUR, new CustomerInfo());
    }
    
    public record CustomerInfo(
        String email,
        String firstName,
        String lastName,
        String referenceId
    ) {
        public CustomerInfo() {
            this("customer@example.com", "John", "Sweet-Escott",
                    "CUST-" + currentTimeMillis());
        }
    }
}