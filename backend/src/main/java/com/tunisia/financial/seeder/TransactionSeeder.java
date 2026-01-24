package com.tunisia.financial.seeder;

import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.repository.TransactionRepository;
import com.tunisia.financial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Transaction seeder for creating sample transactions
 * Runs after user seeder to create realistic transaction data
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class TransactionSeeder implements CommandLineRunner {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        try {
            // Check if transactions table is empty
            if (transactionRepository.count() == 0) {
                log.info("Transactions table is empty. Starting transaction seeding...");
                seedTransactions();
                log.info("Transaction seeding completed successfully!");
            } else {
                log.info("Transactions table already contains data. Skipping transaction seeding.");
            }
        } catch (Exception e) {
            log.warn("Transaction seeding skipped: {}", e.getMessage());
        }
    }

    private void seedTransactions() {
        // Get users by role
        List<User> smeUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.SME_USER)
                .toList();
        
        List<User> analysts = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.FINANCIAL_ANALYST)
                .toList();
        
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .toList();

        if (smeUsers.isEmpty()) {
            log.warn("No SME users found. Skipping transaction seeding.");
            return;
        }

        List<Transaction> transactions = new ArrayList<>();

        // Create 50 legitimate transactions
        for (int i = 0; i < 50; i++) {
            User user = smeUsers.get(random.nextInt(smeUsers.size()));
            Transaction transaction = createLegitimateTransaction(user, i);
            transactions.add(transaction);
        }

        // Create 10 suspicious/fraudulent transactions (high amounts, odd hours, etc.)
        for (int i = 0; i < 10; i++) {
            User user = smeUsers.get(random.nextInt(smeUsers.size()));
            Transaction transaction = createSuspiciousTransaction(user, i);
            transactions.add(transaction);
        }

        // Create some transactions for analysts and admins too
        for (int i = 0; i < 10; i++) {
            User user = i % 2 == 0 && !analysts.isEmpty() 
                    ? analysts.get(random.nextInt(analysts.size()))
                    : admins.get(random.nextInt(admins.size()));
            Transaction transaction = createLegitimateTransaction(user, 50 + i);
            transactions.add(transaction);
        }

        // Save all transactions
        transactionRepository.saveAll(transactions);

        log.info("Created {} transactions:", transactions.size());
        log.info("  - 50 legitimate transactions (normal amounts, business hours)");
        log.info("  - 10 suspicious transactions (high amounts, odd hours, patterns)");
        log.info("  - 10 mixed transactions for analysts/admins");
        log.info("");
        log.info("Transaction ID ranges:");
        log.info("  - Legitimate: IDs 1-50");
        log.info("  - Suspicious: IDs 51-60");
        log.info("  - Mixed: IDs 61-70");
    }

    private Transaction createLegitimateTransaction(User user, int index) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(getRandomTransactionType());
        transaction.setStatus(TransactionStatus.COMPLETED);
        
        // Normal amounts between $10 and $2000
        BigDecimal amount = BigDecimal.valueOf(10 + random.nextDouble() * 1990);
        transaction.setAmount(amount);
        
        transaction.setDescription(generateDescription(transaction.getType(), false));
        transaction.setReferenceNumber("REF-" + System.currentTimeMillis() + "-" + index);
        transaction.setReceipt("RCP-" + index);
        
        // Set fraud score to low (0.1 - 0.3)
        transaction.setFraudScore(0.1 + random.nextDouble() * 0.2);
        
        // Created during business hours (9 AM - 5 PM), recent dates
        Instant createdAt = generateBusinessHoursTimestamp();
        transaction.setCreatedAt(createdAt);
        transaction.setUpdatedAt(createdAt);
        
        return transaction;
    }

    private Transaction createSuspiciousTransaction(User user, int index) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(getSuspiciousTransactionType());
        transaction.setStatus(index % 3 == 0 ? TransactionStatus.PENDING : TransactionStatus.COMPLETED);
        
        // High amounts that trigger fraud alerts
        BigDecimal amount;
        int pattern = index % 4;
        switch (pattern) {
            case 0:
                // Very high amount
                amount = BigDecimal.valueOf(15000 + random.nextDouble() * 35000);
                break;
            case 1:
                // Round suspicious amount
                amount = BigDecimal.valueOf(9999.99);
                break;
            case 2:
                // Just below reporting threshold
                amount = BigDecimal.valueOf(9950 + random.nextDouble() * 49);
                break;
            default:
                // Multiple rapid transactions pattern
                amount = BigDecimal.valueOf(4500 + random.nextDouble() * 500);
        }
        transaction.setAmount(amount);
        
        transaction.setDescription(generateDescription(transaction.getType(), true));
        transaction.setReferenceNumber("SUSP-" + System.currentTimeMillis() + "-" + index);
        transaction.setReceipt("SUS-RCP-" + index);
        
        // Set high fraud score (0.6 - 0.95)
        transaction.setFraudScore(0.6 + random.nextDouble() * 0.35);
        
        // Created during suspicious hours (late night or early morning)
        Instant createdAt = generateSuspiciousTimestamp();
        transaction.setCreatedAt(createdAt);
        transaction.setUpdatedAt(createdAt);
        
        return transaction;
    }

    private TransactionType getRandomTransactionType() {
        TransactionType[] types = TransactionType.values();
        return types[random.nextInt(types.length)];
    }

    private TransactionType getSuspiciousTransactionType() {
        // Withdrawals and transfers are more suspicious
        return random.nextBoolean() ? TransactionType.WITHDRAWAL : TransactionType.TRANSFER;
    }

    private String generateDescription(TransactionType type, boolean suspicious) {
        if (suspicious) {
            return switch (type) {
                case PAYMENT -> "Large payment to overseas account";
                case TRANSFER -> "High-value wire transfer - urgent";
                case WITHDRAWAL -> "Large ATM withdrawal - unusual pattern";
                case DEPOSIT -> "Large cash deposit - source verification required";
            };
        } else {
            return switch (type) {
                case PAYMENT -> "Payment for invoice #" + random.nextInt(1000);
                case TRANSFER -> "Monthly transfer to supplier account";
                case WITHDRAWAL -> "Business operational withdrawal";
                case DEPOSIT -> "Customer payment received";
            };
        }
    }

    private Instant generateBusinessHoursTimestamp() {
        // Random date within last 30 days
        long daysAgo = random.nextInt(30);
        Instant date = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        
        // Business hours: 9 AM - 5 PM
        int hour = 9 + random.nextInt(8); // 9-16 (5 PM is 17:00)
        int minute = random.nextInt(60);
        
        return date.truncatedTo(ChronoUnit.DAYS)
                .plus(hour, ChronoUnit.HOURS)
                .plus(minute, ChronoUnit.MINUTES);
    }

    private Instant generateSuspiciousTimestamp() {
        // Random date within last 30 days
        long daysAgo = random.nextInt(30);
        Instant date = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        
        // Suspicious hours: 11 PM - 5 AM
        int hour;
        if (random.nextBoolean()) {
            hour = 23 + random.nextInt(2); // 23-00
        } else {
            hour = random.nextInt(6); // 0-5
        }
        int minute = random.nextInt(60);
        
        return date.truncatedTo(ChronoUnit.DAYS)
                .plus(hour, ChronoUnit.HOURS)
                .plus(minute, ChronoUnit.MINUTES);
    }
}
