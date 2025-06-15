package com.paytech.payment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.LocalDateTime.now;

@Entity
@Table(name = "idempotency_keys")
@Data
@NoArgsConstructor
public class IdempotencyKey {
    
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @Column(name = "key_value", unique = true, nullable = false)
    private String key;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private String paymentId;
    
    public IdempotencyKey(String key) {
        this.key = key;
        this.createdAt = now();
    }
}