package com.tunisia.financial.controller;

import com.tunisia.financial.dto.ErrorResponse;
import com.tunisia.financial.dto.transaction.TransactionRequest;
import com.tunisia.financial.dto.transaction.TransactionResponse;
import com.tunisia.financial.dto.transaction.TransactionStatistics;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;
import com.tunisia.financial.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST Controller for transaction management operations
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "APIs for managing financial transactions")
@SecurityRequirement(name = "bearer-jwt")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new transaction", description = "Create a new financial transaction for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data or insufficient funds",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have permission")
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating transaction for user: {}", user.getEmail());
        TransactionResponse response = transactionService.createTransaction(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction by ID", description = "Retrieve a specific transaction by its ID. Users can only view their own transactions unless they are ADMIN, AUDITOR, or FINANCIAL_ANALYST")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access this transaction"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        log.info("Fetching transaction {} for user: {}", id, user.getEmail());
        TransactionResponse response = transactionService.getTransactionById(id, user);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get all transactions", description = "Retrieve all transactions with pagination (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN, AUDITOR, or FINANCIAL_ANALYST role")
    })
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        log.info("Fetching all transactions - page: {}, size: {}", page, size);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<TransactionResponse> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/my-transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my transactions", description = "Retrieve all transactions for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal User user) {
        log.info("Fetching transactions for user: {}", user.getEmail());
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<TransactionResponse> transactions = transactionService.getTransactionsByUserId(user.getId(), pageable);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get transactions by user ID", description = "Retrieve all transactions for a specific user (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        log.info("Fetching transactions for user ID: {}", userId);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<TransactionResponse> transactions = transactionService.getTransactionsByUserId(userId, pageable);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get transactions by status", description = "Filter transactions by status (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByStatus(
            @PathVariable TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching transactions with status: {}", status);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionResponse> transactions = transactionService.getTransactionsByStatus(status, pageable);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get transactions by type", description = "Filter transactions by type (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByType(
            @PathVariable TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching transactions with type: {}", type);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionResponse> transactions = transactionService.getTransactionsByType(type, pageable);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/reference/{referenceNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction by reference number", description = "Retrieve a transaction by its reference number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> getTransactionByReferenceNumber(
            @PathVariable String referenceNumber) {
        log.info("Fetching transaction with reference number: {}", referenceNumber);
        TransactionResponse response = transactionService.getTransactionByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get transactions by date range", description = "Filter transactions within a date range (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching transactions between {} and {}", startDate, endDate);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionResponse> transactions = transactionService.getTransactionsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(transactions);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Update transaction status", description = "Update the status of a transaction (Admin and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status,
            @AuthenticationPrincipal User user) {
        log.info("Updating transaction {} status to {}", id, status);
        TransactionResponse response = transactionService.updateTransactionStatus(id, status, user);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a transaction", description = "Cancel a pending transaction. Users can cancel their own transactions, ADMIN can cancel any transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel this transaction"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> cancelTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        log.info("Cancelling transaction {} for user: {}", id, user.getEmail());
        TransactionResponse response = transactionService.cancelTransaction(id, user);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get transaction statistics", description = "Get overall transaction statistics (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TransactionStatistics> getTransactionStatistics() {
        log.info("Fetching transaction statistics");
        TransactionStatistics statistics = transactionService.getTransactionStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/my-statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my transaction statistics", description = "Get transaction statistics for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TransactionStatistics> getMyTransactionStatistics(
            @AuthenticationPrincipal User user) {
        log.info("Fetching transaction statistics for user: {}", user.getEmail());
        TransactionStatistics statistics = transactionService.getUserTransactionStatistics(user.getId());
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/user/{userId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get user transaction statistics", description = "Get transaction statistics for a specific user (Admin, Auditor, and Financial Analyst only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TransactionStatistics> getUserTransactionStatistics(
            @PathVariable UUID userId) {
        log.info("Fetching transaction statistics for user ID: {}", userId);
        TransactionStatistics statistics = transactionService.getUserTransactionStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
}
