package com.paytech.payment.service;

import com.paytech.payment.dto.PaymentApiRequest;
import com.paytech.payment.dto.PaymentApiResponse;
import com.paytech.payment.dto.PaymentRequest;
import com.paytech.payment.entity.IdempotencyKey;
import com.paytech.payment.exception.PaymentException;
import com.paytech.payment.repository.IdempotencyKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.paytech.payment.validator.ValidationHelper.handleValidationErrors;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final WebClient webClient;
    private final String baseUrl;
    private final String bearerToken;
    private final Duration timeout;

    public PaymentServiceImpl(IdempotencyKeyRepository idempotencyKeyRepository,
                             WebClient webClient,
                             @Value("${paytech.api.base-url}") String baseUrl,
                             @Value("${paytech.api.bearer-token}") String bearerToken,
                             @Value("${paytech.api.timeout}") Duration timeout) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.bearerToken = bearerToken;
        this.timeout = timeout;
    }

    @Override
    public String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String processPayment(PaymentRequest paymentRequest, BindingResult bindingResult) {
        String validationResult = handleValidationErrors(bindingResult, "payment-form");
        if (validationResult != null) {
            return validationResult;
        }

        String idempotencyKeyValue = paymentRequest.idempotencyKey();
        log.info("Processing payment for amount: {} with key: {}", paymentRequest.amount(), idempotencyKeyValue);
        
        // Check if payment was already processed
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(idempotencyKeyValue);
        if (existingKey.isPresent()) {
            log.info("Payment already processed for key: {}, skipping", idempotencyKeyValue);
            throw new PaymentException("Payment with this key has already been processed");
        }
        
        try {
            // Save idempotency key before processing
            IdempotencyKey idempotencyKey = new IdempotencyKey(idempotencyKeyValue);
            idempotencyKeyRepository.save(idempotencyKey);
            
            log.info("Created idempotency key: {}", idempotencyKeyValue);
            PaymentApiRequest apiRequest = new PaymentApiRequest(paymentRequest.amount());
            
            PaymentApiResponse response = webClient
                    .post()
                    .uri(baseUrl + "/payments")
                    .header(AUTHORIZATION, "Bearer " + bearerToken)
                    .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .header("Idempotency-Key", idempotencyKeyValue)
                    .bodyValue(apiRequest)
                    .retrieve()
                    .bodyToMono(PaymentApiResponse.class)
                    .timeout(timeout)
                    .block();
            
            if (response == null || response.result() == null) {
                throw new PaymentException("Invalid response from payment API");
            }
            
            // Update idempotency key with payment ID
            idempotencyKey.setPaymentId(response.result().id());
            idempotencyKeyRepository.save(idempotencyKey);
            
            log.info("Payment successful - ID: {}, Status: {}", 
                       response.result().id(), response.result().status());
            
            String redirectUrl = response.result().redirectUrl();
            if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
                throw new PaymentException("No redirect URL provided in API response");
            }
            
            log.info("Payment processed successfully, redirecting to: {}", redirectUrl);
            return redirectUrl;
        } catch (WebClientResponseException e) {
            log.error("API call failed with status: {} and body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("Payment API call failed: " + e.getMessage(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        }
    }
}