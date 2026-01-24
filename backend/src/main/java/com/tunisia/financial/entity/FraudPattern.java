package com.tunisia.financial.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a detected fraud pattern in the system
 */
@Entity
@Table(name = "fraud_patterns", indexes = {
        @Index(name = "idx_fraud_pattern_type", columnList = "pattern_type"),
        @Index(name = "idx_fraud_pattern_detected", columnList = "detected_at"),
        @Index(name = "idx_fraud_pattern_transaction", columnList = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudPattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Type of fraud pattern detected
     */
    @Column(name = "pattern_type", nullable = false, length = 100)
    @NotBlank(message = "Pattern type is required")
    private String patternType;
    
    /**
     * Description of the detected pattern
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Confidence score of the detection (0.0 to 1.0)
     */
    @Column(nullable = false)
    @NotNull(message = "Confidence score is required")
    private Double confidence;
    
    /**
     * Transaction associated with this pattern
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    /**
     * Model that detected this pattern
     */
    @Column(name = "detector_model", length = 50)
    private String detectorModel;
    
    /**
     * Additional metadata as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Timestamp when the pattern was detected
     */
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;
    
    /**
     * Whether this pattern has been reviewed by a human
     */
    @Column(name = "reviewed")
    private Boolean reviewed = false;
    
    /**
     * Reviewer's notes if reviewed
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    /**
     * User ID who reviewed this pattern
     */
    @Column(name = "reviewed_by")
    private java.util.UUID reviewedBy;
    
    /**
     * Timestamp when the pattern was reviewed
     */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    
    @PrePersist
    protected void onCreate() {
        detectedAt = Instant.now();
        if (reviewed == null) {
            reviewed = false;
        }
    }
}
