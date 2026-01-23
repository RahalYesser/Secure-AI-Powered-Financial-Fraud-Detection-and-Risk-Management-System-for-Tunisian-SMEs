package com.tunisia.financial.controller;

import com.tunisia.financial.dto.ErrorResponse;
import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.dto.response.FraudPatternResponse;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.repository.TransactionRepository;
import com.tunisia.financial.service.FraudDetectionService;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for AI-powered fraud detection operations
 */
@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Detection", description = "APIs for AI-powered fraud detection and pattern analysis")
@SecurityRequirement(name = "bearer-jwt")
public class FraudController {
    
    private final FraudDetectionService fraudDetectionService;
    private final TransactionRepository transactionRepository;
    
    @PostMapping("/detect/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Detect fraud for a transaction", 
               description = "Run AI-powered fraud detection on a specific transaction using ensemble of models")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fraud detection completed successfully",
                    content = @Content(schema = @Schema(implementation = FraudDetectionResult.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN, AUDITOR, or FINANCIAL_ANALYST role"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FraudDetectionResult> detectFraud(@PathVariable Long transactionId) {
        log.info("Fraud detection requested for transaction {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        
        FraudDetectionResult result = fraudDetectionService.detectFraud(transaction);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/patterns")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get all fraud patterns", description = "Retrieve all detected fraud patterns with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fraud patterns retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<FraudPatternResponse>> getFraudPatterns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "detectedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.debug("Fetching fraud patterns - page: {}, size: {}", page, size);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<FraudPatternResponse> patterns = fraudDetectionService.getFraudPatterns(pageable);
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/patterns/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get fraud patterns for a transaction", 
               description = "Retrieve all fraud patterns detected for a specific transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fraud patterns retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<FraudPatternResponse>> getFraudPatternsByTransaction(
            @PathVariable Long transactionId) {
        
        log.debug("Fetching fraud patterns for transaction {}", transactionId);
        List<FraudPatternResponse> patterns = 
                fraudDetectionService.getFraudPatternsByTransactionId(transactionId);
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/patterns/unreviewed")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get unreviewed fraud patterns", 
               description = "Retrieve all fraud patterns that haven't been reviewed yet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unreviewed patterns retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<FraudPatternResponse>> getUnreviewedPatterns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching unreviewed fraud patterns");
        Pageable pageable = PageRequest.of(page, size, Sort.by("detectedAt").descending());
        Page<FraudPatternResponse> patterns = fraudDetectionService.getUnreviewedFraudPatterns(pageable);
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/patterns/high-confidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get high confidence fraud patterns", 
               description = "Retrieve fraud patterns with confidence above specified threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "High confidence patterns retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<FraudPatternResponse>> getHighConfidencePatterns(
            @RequestParam(defaultValue = "0.8") Double threshold) {
        
        log.debug("Fetching high confidence fraud patterns with threshold {}", threshold);
        List<FraudPatternResponse> patterns = 
                fraudDetectionService.getHighConfidenceFraudPatterns(threshold);
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/patterns/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get fraud patterns by date range", 
               description = "Retrieve fraud patterns detected within a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patterns retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<FraudPatternResponse>> getPatternsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        
        log.debug("Fetching fraud patterns between {} and {}", startDate, endDate);
        List<FraudPatternResponse> patterns = 
                fraudDetectionService.getFraudPatternsByDateRange(startDate, endDate);
        return ResponseEntity.ok(patterns);
    }
    
    @PutMapping("/patterns/{patternId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Mark fraud pattern as reviewed", 
               description = "Mark a fraud pattern as reviewed with optional notes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pattern marked as reviewed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or AUDITOR role"),
            @ApiResponse(responseCode = "404", description = "Pattern not found")
    })
    public ResponseEntity<Map<String, String>> reviewPattern(
            @PathVariable Long patternId,
            @RequestBody(required = false) Map<String, String> reviewData) {
        
        log.info("Reviewing fraud pattern {}", patternId);
        String reviewNotes = reviewData != null ? reviewData.get("reviewNotes") : null;
        fraudDetectionService.markPatternAsReviewed(patternId, reviewNotes);
        
        return ResponseEntity.ok(Map.of(
                "message", "Fraud pattern marked as reviewed successfully",
                "patternId", patternId.toString()
        ));
    }
    
    @PostMapping("/models/{modelType}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update fraud detection model", 
               description = "Reload or update a specific fraud detection model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Model updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid model type"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    public ResponseEntity<Map<String, String>> updateModel(@PathVariable String modelType) {
        log.info("Model update requested for type: {}", modelType);
        fraudDetectionService.updateModel(modelType);
        
        return ResponseEntity.ok(Map.of(
                "message", "Model updated successfully",
                "modelType", modelType
        ));
    }
}
