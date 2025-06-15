package com.paytech.payment.dto;

public record PaymentApiResponse(
    String timestamp,
    Integer status,
    PaymentResult result
) {
    public record PaymentResult(
        String id,
        String redirectUrl,
        String status
    ) {}
}