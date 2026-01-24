package com.tunisia.financial.seeder;

import com.tunisia.financial.entity.FraudPattern;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.repository.FraudPatternRepository;
import com.tunisia.financial.repository.TransactionRepository;
import com.tunisia.financial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Fraud Pattern seeder for creating sample fraud patterns
 * Runs after transaction seeder to link patterns to suspicious transactions
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class FraudPatternSeeder implements CommandLineRunner {

    private final FraudPatternRepository fraudPatternRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        try {
            // Check if fraud_patterns table is empty
            if (fraudPatternRepository.count() == 0) {
                log.info("Fraud patterns table is empty. Starting fraud pattern seeding...");
                seedFraudPatterns();
                log.info("Fraud pattern seeding completed successfully!");
            } else {
                log.info("Fraud patterns table already contains data. Skipping fraud pattern seeding.");
            }
        } catch (Exception e) {
            log.warn("Fraud pattern seeding skipped: {}", e.getMessage());
        }
    }

    private void seedFraudPatterns() {
        // Get transactions with high fraud scores
        List<Transaction> suspiciousTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getFraudScore() != null && t.getFraudScore() >= 0.5)
                .toList();

        if (suspiciousTransactions.isEmpty()) {
            log.warn("No suspicious transactions found. Skipping fraud pattern seeding.");
            return;
        }

        // Get auditors and admins for reviewing patterns
        List<User> reviewers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.AUDITOR)
                .toList();

        List<FraudPattern> patterns = new ArrayList<>();

        // Create fraud patterns for suspicious transactions
        for (Transaction transaction : suspiciousTransactions) {
            // Create 1-3 patterns per suspicious transaction
            int numPatterns = 1 + random.nextInt(3);
            
            for (int i = 0; i < numPatterns; i++) {
                FraudPattern pattern = createFraudPattern(transaction, reviewers);
                patterns.add(pattern);
            }
        }

        // Save all patterns
        fraudPatternRepository.saveAll(patterns);

        long reviewedCount = patterns.stream().filter(FraudPattern::getReviewed).count();
        long unreviewedCount = patterns.size() - reviewedCount;

        log.info("Created {} fraud patterns:", patterns.size());
        log.info("  - {} patterns reviewed", reviewedCount);
        log.info("  - {} patterns unreviewed (pending review)", unreviewedCount);
        log.info("  - Patterns linked to {} suspicious transactions", suspiciousTransactions.size());
    }

    private FraudPattern createFraudPattern(Transaction transaction, List<User> reviewers) {
        FraudPattern pattern = new FraudPattern();
        
        // Set transaction
        pattern.setTransaction(transaction);
        
        // Determine pattern type based on transaction characteristics
        String patternType = determinePatternType(transaction);
        pattern.setPatternType(patternType);
        
        // Generate description
        pattern.setDescription(generateDescription(patternType, transaction));
        
        // Set confidence based on fraud score
        pattern.setConfidence(transaction.getFraudScore());
        
        // Set detector model (ensemble)
        String[] models = {"ENSEMBLE", "DJL", "ONNX", "TENSORFLOW"};
        pattern.setDetectorModel(models[random.nextInt(models.length)]);
        
        // Generate metadata
        pattern.setMetadata(generateMetadata(transaction));
        
        // Set detection time (same as transaction or shortly after)
        Instant detectedAt = transaction.getCreatedAt().plus(
                random.nextInt(60), ChronoUnit.MINUTES
        );
        pattern.setDetectedAt(detectedAt);
        
        // Randomly review some patterns (60% reviewed)
        if (random.nextDouble() < 0.6 && !reviewers.isEmpty()) {
            pattern.setReviewed(true);
            User reviewer = reviewers.get(random.nextInt(reviewers.size()));
            pattern.setReviewedBy(reviewer.getId());
            pattern.setReviewNotes(generateReviewNotes(patternType, transaction));
            pattern.setReviewedAt(detectedAt.plus(1 + random.nextInt(24), ChronoUnit.HOURS));
        } else {
            pattern.setReviewed(false);
        }
        
        return pattern;
    }

    private String determinePatternType(Transaction transaction) {
        double fraudScore = transaction.getFraudScore();
        double amount = transaction.getAmount().doubleValue();
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        
        // Determine pattern based on characteristics
        if (fraudScore >= 0.85) {
            if (amount > 20000) {
                return "HIGH_AMOUNT_UNUSUAL";
            } else if (hour >= 22 || hour <= 6) {
                return "HIGH_AMOUNT_LATE_NIGHT";
            } else {
                return "SUSPICIOUS_ACTIVITY";
            }
        } else if (fraudScore >= 0.7) {
            if (amount > 15000) {
                return "MEDIUM_RISK_HIGH_AMOUNT";
            } else if (hour >= 22 || hour <= 6) {
                return "LATE_NIGHT_TRANSACTION";
            } else {
                return "UNUSUAL_PATTERN";
            }
        } else if (fraudScore >= 0.6) {
            if (amount >= 9900 && amount <= 10000) {
                return "STRUCTURING_PATTERN";
            } else {
                return "MEDIUM_RISK_UNUSUAL_PATTERN";
            }
        } else {
            return "BORDERLINE_SUSPICIOUS";
        }
    }

    private String generateDescription(String patternType, Transaction transaction) {
        double amount = transaction.getAmount().doubleValue();
        String type = transaction.getType().toString();
        
        return switch (patternType) {
            case "HIGH_AMOUNT_UNUSUAL" -> 
                String.format("High-value %s transaction of $%.2f detected. Amount significantly exceeds normal patterns for this user.", 
                    type.toLowerCase(), amount);
            case "HIGH_AMOUNT_LATE_NIGHT" -> 
                String.format("High-value %s of $%.2f during late night hours. Unusual for business operations.", 
                    type.toLowerCase(), amount);
            case "SUSPICIOUS_ACTIVITY" -> 
                String.format("%s transaction of $%.2f shows multiple suspicious indicators. Requires immediate review.", 
                    type, amount);
            case "MEDIUM_RISK_HIGH_AMOUNT" -> 
                String.format("Transaction of $%.2f exceeds typical amounts for this account. Monitor for related activity.", 
                    amount);
            case "LATE_NIGHT_TRANSACTION" -> 
                String.format("%s during non-business hours (late night/early morning). Atypical pattern detected.", 
                    type);
            case "UNUSUAL_PATTERN" -> 
                String.format("%s of $%.2f displays characteristics inconsistent with user's historical behavior.", 
                    type, amount);
            case "STRUCTURING_PATTERN" -> 
                String.format("Transaction amount of $%.2f appears designed to avoid reporting thresholds. Potential structuring.", 
                    amount);
            case "MEDIUM_RISK_UNUSUAL_PATTERN" -> 
                String.format("%s shows moderate risk indicators. Confidence: %.2f. Review recommended.", 
                    type, transaction.getFraudScore());
            case "BORDERLINE_SUSPICIOUS" -> 
                String.format("Transaction displays some suspicious characteristics but below high-confidence threshold. Monitor activity.", 
                    type);
            default -> 
                String.format("Fraud pattern detected in %s transaction of $%.2f.", type.toLowerCase(), amount);
        };
    }

    private String generateMetadata(Transaction transaction) {
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        int dayOfWeek = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                .getDayOfWeek().getValue();
        
        return String.format(
                "{\"avgConfidence\": %.3f, \"threshold\": 0.70, \"amount\": %.2f, " +
                "\"hour\": %d, \"dayOfWeek\": %d, \"type\": \"%s\", \"isWeekend\": %b, " +
                "\"isBusinessHours\": %b, \"detectionTimestamp\": \"%s\"}",
                transaction.getFraudScore(),
                transaction.getAmount(),
                hour,
                dayOfWeek,
                transaction.getType(),
                dayOfWeek >= 6,
                hour >= 9 && hour <= 17,
                Instant.now()
        );
    }

    private String generateReviewNotes(String patternType, Transaction transaction) {
        String[] reviewOutcomes = {
            "Confirmed as suspicious. Account flagged for monitoring.",
            "False positive - legitimate business transaction. User contacted for verification.",
            "Genuine fraud detected. Account frozen pending investigation.",
            "Requires additional documentation. Request sent to user.",
            "Pattern validated. No immediate action required but monitoring continues.",
            "Escalated to fraud investigation team for detailed analysis.",
            "User provided satisfactory explanation. Marked as reviewed.",
            "Transaction reversed. Funds returned to source account."
        };
        
        return reviewOutcomes[random.nextInt(reviewOutcomes.length)];
    }
}
