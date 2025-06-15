package com.paytech.payment.service;

import com.paytech.payment.dto.PaymentApiResponse;
import com.paytech.payment.dto.PaymentRequest;
import com.paytech.payment.entity.IdempotencyKey;
import com.paytech.payment.exception.PaymentException;
import com.paytech.payment.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private BindingResult bindingResult;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
            idempotencyKeyRepository,
            webClient,
            "https://test-api.com",
            "test-token",
            ofSeconds(30)
        );
    }
    
    private void setupWebClientMocks() {
        // Setup WebClient chain mocks only when needed
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void generateIdempotencyKey_generates_unique_keys() {
        // When
        String key1 = paymentService.generateIdempotencyKey();
        String key2 = paymentService.generateIdempotencyKey();
        
        // Then
        assertNotEquals(key1, key2);
    }

    @Test
    void processPayment_new_payment_success() {
        // Given
        setupWebClientMocks();
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "new-key");
        IdempotencyKey newKey = new IdempotencyKey("new-key");
        
        PaymentApiResponse.PaymentResult result = new PaymentApiResponse.PaymentResult(
            "payment-123",
            "https://redirect.example.com",
            "success"
        );
        PaymentApiResponse response = new PaymentApiResponse("2023-01-01T00:00:00", 200, result);
        
        when(idempotencyKeyRepository.findByKey("new-key")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenReturn(newKey);
        when(responseSpec.bodyToMono(PaymentApiResponse.class)).thenReturn(Mono.just(response));
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When
        String redirectUrl = paymentService.processPayment(request, bindingResult);
        
        // Then
        assertEquals("https://redirect.example.com", redirectUrl);
        verify(idempotencyKeyRepository, times(2)).save(any(IdempotencyKey.class));
        verify(idempotencyKeyRepository, atLeastOnce()).save(argThat(key -> 
            key.getKey().equals("new-key") &&
            key.getPaymentId().equals("payment-123")
        ));
    }

    @Test
    void processPayment_already_processed_payment_throws_exception() {
        // Given
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "existing-key");
        IdempotencyKey existingKey = new IdempotencyKey("existing-key");
        existingKey.setPaymentId("payment-123");
        
        when(idempotencyKeyRepository.findByKey("existing-key"))
                .thenReturn(Optional.of(existingKey));
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(request, bindingResult);
        });
        
        assertTrue(exception.getMessage().contains("Payment with this key has already been processed"));
        verify(idempotencyKeyRepository, never()).save(any(IdempotencyKey.class));
        verifyNoInteractions(webClient);
    }


    @Test
    void processPayment_empty_api_response_throws_PaymentException() {
        // Given
        setupWebClientMocks();
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "test-key");
        IdempotencyKey savedKey = new IdempotencyKey("test-key");
        
        when(idempotencyKeyRepository.findByKey("test-key"))
                .thenReturn(Optional.empty())  // First call
                .thenReturn(Optional.of(savedKey));  // Second call from updateIdempotencyKeyStatus
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenReturn(savedKey);
        when(responseSpec.bodyToMono(PaymentApiResponse.class)).thenReturn(Mono.empty());
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(request, bindingResult);
        });
        
        assertTrue(exception.getMessage().contains("Invalid response from payment API"));
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }

    @Test
    void processPayment_null_result_throws_PaymentException() {
        // Given
        setupWebClientMocks();
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "test-key");
        PaymentApiResponse response = new PaymentApiResponse("2023-01-01T00:00:00", 200, null);
        IdempotencyKey savedKey = new IdempotencyKey("test-key");
        
        when(idempotencyKeyRepository.findByKey("test-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedKey));
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenReturn(savedKey);
        when(responseSpec.bodyToMono(PaymentApiResponse.class)).thenReturn(Mono.just(response));
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(request, bindingResult);
        });
        
        assertTrue(exception.getMessage().contains("Invalid response from payment API"));
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }

    @Test
    void processPayment_empty_redirect_url_throws_PaymentException() {
        // Given
        setupWebClientMocks();
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "test-key");
        PaymentApiResponse.PaymentResult result = new PaymentApiResponse.PaymentResult(
            "payment-123",
            "",
            "success"
        );
        PaymentApiResponse response = new PaymentApiResponse("2023-01-01T00:00:00", 200, result);
        IdempotencyKey savedKey = new IdempotencyKey("test-key");
        
        when(idempotencyKeyRepository.findByKey("test-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedKey));
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenReturn(savedKey);
        when(responseSpec.bodyToMono(PaymentApiResponse.class)).thenReturn(Mono.just(response));
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(request, bindingResult);
        });
        
        assertTrue(exception.getMessage().contains("No redirect URL provided in API response"));
        verify(idempotencyKeyRepository, times(2)).save(any(IdempotencyKey.class));
    }

    @Test
    void processPayment_WebClientResponseException_throws_payment_exception() {
        // Given
        setupWebClientMocks();
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "test-key");
        WebClientResponseException webClientException = WebClientResponseException.create(
            BAD_REQUEST.value(),
            "Bad Request",
            null,
            "{\"error\":\"Invalid amount\"}".getBytes(),
            null
        );
        IdempotencyKey savedKey = new IdempotencyKey("test-key");
        
        when(idempotencyKeyRepository.findByKey("test-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedKey));
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenReturn(savedKey);
        when(responseSpec.bodyToMono(PaymentApiResponse.class)).thenReturn(Mono.error(webClientException));
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(request, bindingResult);
        });
        
        assertTrue(exception.getMessage().contains("Payment API call failed"));
        assertEquals("{\"error\":\"Invalid amount\"}", exception.getResponseBody());
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }

    @Test
    void processPayment_RuntimeException_throws_PaymentException() {
        // Given
        setupWebClientMocks();
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "test-key");
        RuntimeException runtimeException = new RuntimeException("Network error");
        IdempotencyKey savedKey = new IdempotencyKey("test-key");
        
        when(idempotencyKeyRepository.findByKey("test-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedKey));
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenReturn(savedKey);
        when(responseSpec.bodyToMono(PaymentApiResponse.class)).thenReturn(Mono.error(runtimeException));
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(request, bindingResult);
        });
        
        assertTrue(exception.getMessage().contains("Payment processing failed"));
        assertTrue(exception.getMessage().contains("Network error"));
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }

    @Test
    void processPayment_validation_errors_returns_error_view() {
        // Given
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), "test-key");
        when(bindingResult.hasErrors()).thenReturn(true);
        
        // When
        String result = paymentService.processPayment(request, bindingResult);
        
        // Then
        assertEquals("payment-form", result);
        verifyNoInteractions(idempotencyKeyRepository);
        verifyNoInteractions(webClient);
    }
}