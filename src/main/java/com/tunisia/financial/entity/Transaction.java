package com.tunisia.financial.entity;

import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a financial transaction in the system
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_user", columnList = "user_id"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Type of the transaction (PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Transaction type is required")
    private TransactionType type;
    
    /**
     * Current status of the transaction
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Transaction status is required")
    private TransactionStatus status;
    
    /**
     * Transaction amount (must be greater than 0)
     */
    @Column(nullable = false, precision = 19, scale = 2)
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    /**
     * User who initiated the transaction
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * Description of the transaction
     */
    @Column(length = 500)
    private String description;
    
    /**
     * Fraud detection score (0.0 - 1.0, higher means more suspicious)
     */
    @Column(name = "fraud_score")
    private Double fraudScore;
    
    /**
     * Reference number for the transaction (unique identifier)
     */
    @Column(name = "reference_number", unique = true, length = 50)
    private String referenceNumber;
    
    /**
     * Receipt or confirmation code
     */
    @Column(length = 100)
    private String receipt;
    
    /**
     * Timestamp when the transaction was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Timestamp when the transaction was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Set timestamps before persisting
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        
        // Generate reference number if not provided
        if (referenceNumber == null) {
            referenceNumber = generateReferenceNumber();
        }
    }
    
    /**
     * Update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    /**
     * Generate a unique reference number for the transaction
     */
    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}
