package com.tunisia.financial.service;

import com.tunisia.financial.dto.transaction.TransactionRequest;
import com.tunisia.financial.dto.transaction.TransactionResponse;
import com.tunisia.financial.dto.transaction.TransactionStatistics;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for transaction management
 */
public interface TransactionService {
    
    /**
     * Create a new transaction
     */
    TransactionResponse createTransaction(TransactionRequest request, User user);
    
    /**
     * Get transaction by ID
     */
    TransactionResponse getTransactionById(Long id, User user);
    
    /**
     * Get all transactions with pagination
     */
    Page<TransactionResponse> getAllTransactions(Pageable pageable);
    
    /**
     * Get transactions for a specific user
     */
    Page<TransactionResponse> getTransactionsByUserId(UUID userId, Pageable pageable);
    
    /**
     * Get transactions by status
     */
    Page<TransactionResponse> getTransactionsByStatus(TransactionStatus status, Pageable pageable);
    
    /**
     * Get transactions by type
     */
    Page<TransactionResponse> getTransactionsByType(TransactionType type, Pageable pageable);
    
    /**
     * Get transactions for user by status
     */
    Page<TransactionResponse> getUserTransactionsByStatus(UUID userId, TransactionStatus status, Pageable pageable);
    
    /**
     * Get transactions for user by type
     */
    Page<TransactionResponse> getUserTransactionsByType(UUID userId, TransactionType type, Pageable pageable);
    
    /**
     * Get transaction by reference number
     */
    TransactionResponse getTransactionByReferenceNumber(String referenceNumber);
    
    /**
     * Get transactions within date range
     */
    Page<TransactionResponse> getTransactionsByDateRange(Instant startDate, Instant endDate, Pageable pageable);
    
    /**
     * Get user transactions within date range
     */
    Page<TransactionResponse> getUserTransactionsByDateRange(UUID userId, Instant startDate, Instant endDate, Pageable pageable);
    
    /**
     * Update transaction status
     */
    TransactionResponse updateTransactionStatus(Long id, TransactionStatus status, User user);
    
    /**
     * Cancel a pending transaction
     */
    TransactionResponse cancelTransaction(Long id, User user);
    
    /**
     * Get transaction statistics
     */
    TransactionStatistics getTransactionStatistics();
    
    /**
     * Get user transaction statistics
     */
    TransactionStatistics getUserTransactionStatistics(UUID userId);
    
    /**
     * Convert Transaction entity to TransactionResponse DTO
     */
    TransactionResponse convertToResponse(Transaction transaction);
}
