package com.tunisia.financial.service.impl;

import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.dto.transaction.TransactionRequest;
import com.tunisia.financial.dto.transaction.TransactionResponse;
import com.tunisia.financial.dto.transaction.TransactionStatistics;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.exception.transaction.InsufficientFundsException;
import com.tunisia.financial.exception.transaction.InvalidTransactionException;
import com.tunisia.financial.exception.transaction.TransactionNotFoundException;
import com.tunisia.financial.repository.TransactionRepository;
import com.tunisia.financial.service.FraudDetectionService;
import com.tunisia.financial.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of TransactionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionServiceImpl implements TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;
    
    private static final double FRAUD_THRESHOLD = 0.7;
    
    @Override
    public TransactionResponse createTransaction(TransactionRequest request, User user) {
        log.info("Creating transaction for user: {}", user.getEmail());
        
        // Validate transaction
        validateTransaction(request, user);
        
        // Create transaction entity with PENDING status
        Transaction transaction = new Transaction();
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setUser(user);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setReceipt(generateReceipt());
        
        // Save transaction immediately (PENDING state)
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction created with ID: {} - Running AI fraud detection...", savedTransaction.getId());
        
        // REAL-TIME AI FRAUD DETECTION
        try {
            FraudDetectionResult fraudResult = fraudDetectionService.detectFraud(savedTransaction);
            
            // Update fraud score
            savedTransaction.setFraudScore(fraudResult.fraudScore());
            
            // Make decision based on AI analysis
            if (fraudResult.isFraud() && fraudResult.confidence() >= FRAUD_THRESHOLD) {
                // FRAUD DETECTED - Block transaction
                savedTransaction.setStatus(TransactionStatus.FRAUD_DETECTED);
                log.warn("FRAUD DETECTED for transaction {}. Confidence: {}. Reason: {}", 
                        savedTransaction.getId(), fraudResult.confidence(), fraudResult.primaryReason());
            } else {
                // LEGITIMATE - Approve transaction
                savedTransaction.setStatus(TransactionStatus.COMPLETED);
                log.info("Transaction {} approved by AI. Confidence: {}", 
                        savedTransaction.getId(), fraudResult.confidence());
            }
            
            // Save updated status
            savedTransaction = transactionRepository.save(savedTransaction);
            
        } catch (Exception e) {
            // If AI detection fails, keep as PENDING for manual review
            log.error("AI fraud detection failed for transaction {}, keeping PENDING for manual review", 
                    savedTransaction.getId(), e);
        }
        
        return convertToResponse(savedTransaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id, User user) {
        log.debug("Fetching transaction with ID: {}", id);
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        // Check if user has permission to view this transaction
        if (!canAccessTransaction(transaction, user)) {
            throw new AccessDeniedException("You don't have permission to access this transaction");
        }
        
        return convertToResponse(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        log.debug("Fetching all transactions");
        return transactionRepository.findAll(pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByUserId(UUID userId, Pageable pageable) {
        log.debug("Fetching transactions for user ID: {}", userId);
        return transactionRepository.findByUserId(userId, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByStatus(TransactionStatus status, Pageable pageable) {
        log.debug("Fetching transactions with status: {}", status);
        return transactionRepository.findByStatus(status, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByType(TransactionType type, Pageable pageable) {
        log.debug("Fetching transactions with type: {}", type);
        return transactionRepository.findByType(type, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactionsByStatus(UUID userId, TransactionStatus status, Pageable pageable) {
        log.debug("Fetching transactions for user {} with status: {}", userId, status);
        return transactionRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactionsByType(UUID userId, TransactionType type, Pageable pageable) {
        log.debug("Fetching transactions for user {} with type: {}", userId, type);
        return transactionRepository.findByUserIdAndType(userId, type, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReferenceNumber(String referenceNumber) {
        log.debug("Fetching transaction with reference number: {}", referenceNumber);
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with reference number: " + referenceNumber));
        return convertToResponse(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        log.debug("Fetching transactions between {} and {}", startDate, endDate);
        return transactionRepository.findByDateRange(startDate, endDate, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactionsByDateRange(UUID userId, Instant startDate, Instant endDate, Pageable pageable) {
        log.debug("Fetching transactions for user {} between {} and {}", userId, startDate, endDate);
        return transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    public TransactionResponse updateTransactionStatus(Long id, TransactionStatus status, User user) {
        log.info("Updating transaction {} status to {}", id, status);
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        // Only ADMIN and FINANCIAL_ANALYST can update transaction status
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.FINANCIAL_ANALYST) {
            throw new AccessDeniedException("You don't have permission to update transaction status");
        }
        
        // Validate status transition
        validateStatusTransition(transaction.getStatus(), status);
        
        transaction.setStatus(status);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        log.info("Transaction status updated successfully");
        return convertToResponse(updatedTransaction);
    }
    
    @Override
    public TransactionResponse cancelTransaction(Long id, User user) {
        log.info("Cancelling transaction {}", id);
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        // Check if user can cancel this transaction
        if (!transaction.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You don't have permission to cancel this transaction");
        }
        
        // Can only cancel pending transactions
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("Only pending transactions can be cancelled");
        }
        
        transaction.setStatus(TransactionStatus.FAILED);
        Transaction cancelledTransaction = transactionRepository.save(transaction);
        
        log.info("Transaction cancelled successfully");
        return convertToResponse(cancelledTransaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionStatistics getTransactionStatistics() {
        log.debug("Calculating transaction statistics");
        
        long totalTransactions = transactionRepository.count();
        long pendingTransactions = transactionRepository.countByStatus(TransactionStatus.PENDING);
        long completedTransactions = transactionRepository.countByStatus(TransactionStatus.COMPLETED);
        long failedTransactions = transactionRepository.countByStatus(TransactionStatus.FAILED);
        long fraudDetectedTransactions = transactionRepository.countByStatus(TransactionStatus.FRAUD_DETECTED);
        
        BigDecimal totalAmount = transactionRepository.sumAmountByStatus(TransactionStatus.COMPLETED);
        BigDecimal averageAmount = transactionRepository.getAverageCompletedAmount();
        
        long paymentCount = transactionRepository.countByType(TransactionType.PAYMENT);
        long transferCount = transactionRepository.countByType(TransactionType.TRANSFER);
        long withdrawalCount = transactionRepository.countByType(TransactionType.WITHDRAWAL);
        long depositCount = transactionRepository.countByType(TransactionType.DEPOSIT);
        
        return new TransactionStatistics(
                totalTransactions,
                pendingTransactions,
                completedTransactions,
                failedTransactions,
                fraudDetectedTransactions,
                totalAmount,
                averageAmount,
                paymentCount,
                transferCount,
                withdrawalCount,
                depositCount
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionStatistics getUserTransactionStatistics(UUID userId) {
        log.debug("Calculating transaction statistics for user {}", userId);
        
        long totalTransactions = transactionRepository.countByUserId(userId);
        
        // For user stats, we'll get counts by querying with filters
        // This is a simplified version - you could optimize with custom queries
        long pendingTransactions = transactionRepository.findByUserIdAndStatus(userId, TransactionStatus.PENDING, Pageable.unpaged()).getTotalElements();
        long completedTransactions = transactionRepository.findByUserIdAndStatus(userId, TransactionStatus.COMPLETED, Pageable.unpaged()).getTotalElements();
        long failedTransactions = transactionRepository.findByUserIdAndStatus(userId, TransactionStatus.FAILED, Pageable.unpaged()).getTotalElements();
        long fraudDetectedTransactions = transactionRepository.findByUserIdAndStatus(userId, TransactionStatus.FRAUD_DETECTED, Pageable.unpaged()).getTotalElements();
        
        BigDecimal totalAmount = transactionRepository.sumAmountByUserIdAndStatus(userId, TransactionStatus.COMPLETED);
        BigDecimal averageAmount = completedTransactions > 0 ? totalAmount.divide(BigDecimal.valueOf(completedTransactions), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        
        long paymentCount = transactionRepository.findByUserIdAndType(userId, TransactionType.PAYMENT, Pageable.unpaged()).getTotalElements();
        long transferCount = transactionRepository.findByUserIdAndType(userId, TransactionType.TRANSFER, Pageable.unpaged()).getTotalElements();
        long withdrawalCount = transactionRepository.findByUserIdAndType(userId, TransactionType.WITHDRAWAL, Pageable.unpaged()).getTotalElements();
        long depositCount = transactionRepository.findByUserIdAndType(userId, TransactionType.DEPOSIT, Pageable.unpaged()).getTotalElements();
        
        return new TransactionStatistics(
                totalTransactions,
                pendingTransactions,
                completedTransactions,
                failedTransactions,
                fraudDetectedTransactions,
                totalAmount,
                averageAmount,
                paymentCount,
                transferCount,
                withdrawalCount,
                depositCount
        );
    }
    
    @Override
    public TransactionResponse convertToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getUser().getId(),
                transaction.getUser().getEmail(),
                transaction.getDescription(),
                transaction.getFraudScore(),
                transaction.getReferenceNumber(),
                transaction.getReceipt(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
    
    // Private helper methods
    
    private void validateTransaction(TransactionRequest request, User user) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than zero");
        }
        
        // For WITHDRAWAL, check if user has sufficient funds (simplified - in real app check account balance)
        if (request.type() == TransactionType.WITHDRAWAL) {
            BigDecimal userBalance = getUserBalance(user);
            if (userBalance.compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds for withdrawal. Available: " + userBalance);
            }
        }
        
        // Validate transaction limits based on user role
        validateTransactionLimits(request, user);
    }
    
    private BigDecimal getUserBalance(User user) {
        // Simplified balance calculation - sum of completed deposits minus withdrawals
        BigDecimal deposits = transactionRepository.sumAmountByUserIdAndStatus(user.getId(), TransactionStatus.COMPLETED);
        return deposits != null ? deposits : BigDecimal.ZERO;
    }
    
    private void validateTransactionLimits(TransactionRequest request, User user) {
        // Example: SME_USER has transaction limit
        if (user.getRole() == UserRole.SME_USER) {
            BigDecimal smeLimit = new BigDecimal("5000");
            if (request.amount().compareTo(smeLimit) > 0) {
                throw new InvalidTransactionException("Transaction amount exceeds limit for SME users: " + smeLimit);
            }
        }
    }
    
    private String generateReceipt() {
        return "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private boolean canAccessTransaction(Transaction transaction, User user) {
        // ADMIN and AUDITOR can access all transactions
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.AUDITOR) {
            return true;
        }
        
        // FINANCIAL_ANALYST can access all transactions
        if (user.getRole() == UserRole.FINANCIAL_ANALYST) {
            return true;
        }
        
        // Users can only access their own transactions
        return transaction.getUser().getId().equals(user.getId());
    }
    
    private void validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        // COMPLETED and FRAUD_DETECTED are final states
        if (currentStatus == TransactionStatus.COMPLETED || currentStatus == TransactionStatus.FRAUD_DETECTED) {
            throw new InvalidTransactionException("Cannot change status of " + currentStatus + " transaction");
        }
        
        // PENDING can transition to any status
        // FAILED can transition back to PENDING for retry
        if (currentStatus == TransactionStatus.FAILED && newStatus != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("Failed transactions can only be set back to PENDING for retry");
        }
    }
}
