package com.tunisia.financial.controller;

import com.tunisia.financial.dto.ErrorResponse;
import com.tunisia.financial.dto.FinancialData;
import com.tunisia.financial.dto.response.RiskAssessment;
import com.tunisia.financial.dto.response.RiskReport;
import com.tunisia.financial.entity.CreditRiskAssessment;
import com.tunisia.financial.enumerations.RiskCategory;
import com.tunisia.financial.service.CreditRiskService;
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
import java.util.UUID;

/**
 * REST Controller for AI-powered credit risk assessment operations
 */
@RestController
@RequestMapping("/api/v1/credit-risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Credit Risk Assessment", description = "APIs for AI-powered credit risk assessment and reporting")
@SecurityRequirement(name = "bearer-jwt")
public class CreditRiskController {
    
    private final CreditRiskService creditRiskService;
    
    @PostMapping("/assess")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Assess credit risk", 
               description = "Perform AI-powered credit risk assessment using ensemble of ML models")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit risk assessment completed successfully",
                    content = @Content(schema = @Schema(implementation = RiskAssessment.class))),
            @ApiResponse(responseCode = "400", description = "Invalid financial data provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FINANCIAL_ANALYST role"),
            @ApiResponse(responseCode = "404", description = "SME user not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RiskAssessment> assessCreditRisk(@Valid @RequestBody FinancialData financialData) {
        log.info("Credit risk assessment requested for SME user {}", financialData.smeUserId());
        
        RiskAssessment assessment = creditRiskService.assessCreditRisk(financialData);
        return ResponseEntity.ok(assessment);
    }
    
    @GetMapping("/report/{assessmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'SME_USER', 'AUDITOR')")
    @Operation(summary = "Generate risk report", 
               description = "Generate comprehensive risk report with detailed analysis and recommendations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk report generated successfully",
                    content = @Content(schema = @Schema(implementation = RiskReport.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Assessment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RiskReport> generateRiskReport(@PathVariable Long assessmentId) {
        log.info("Risk report generation requested for assessment {}", assessmentId);
        
        RiskReport report = creditRiskService.generateRiskReport(assessmentId);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/{assessmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'SME_USER', 'AUDITOR')")
    @Operation(summary = "Get assessment by ID", description = "Retrieve a specific credit risk assessment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<CreditRiskAssessment> getAssessmentById(@PathVariable Long assessmentId) {
        log.debug("Fetching assessment {}", assessmentId);
        
        CreditRiskAssessment assessment = creditRiskService.getAssessmentById(assessmentId);
        return ResponseEntity.ok(assessment);
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get assessments by user", 
               description = "Retrieve all credit risk assessments for a specific SME user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<CreditRiskAssessment>> getAssessmentsByUser(@PathVariable UUID userId) {
        log.debug("Fetching assessments for user {}", userId);
        
        List<CreditRiskAssessment> assessments = creditRiskService.getAssessmentsByUserId(userId);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/user/{userId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'SME_USER', 'AUDITOR')")
    @Operation(summary = "Get latest assessment for user", 
               description = "Retrieve the most recent credit risk assessment for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest assessment retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "No assessments found for user")
    })
    public ResponseEntity<CreditRiskAssessment> getLatestAssessment(@PathVariable UUID userId) {
        log.debug("Fetching latest assessment for user {}", userId);
        
        CreditRiskAssessment assessment = creditRiskService.getMostRecentAssessment(userId);
        if (assessment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(assessment);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get all assessments", description = "Retrieve all credit risk assessments with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<CreditRiskAssessment>> getAllAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assessedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.debug("Fetching all assessments - page: {}, size: {}", page, size);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CreditRiskAssessment> assessments = creditRiskService.getAllAssessments(pageable);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get assessments by risk category", 
               description = "Retrieve assessments filtered by risk category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<CreditRiskAssessment>> getAssessmentsByCategory(
            @PathVariable RiskCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching assessments for category {}", category);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assessedAt"));
        Page<CreditRiskAssessment> assessments = creditRiskService.getAssessmentsByCategory(category, pageable);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/high-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get high-risk assessments", 
               description = "Retrieve assessments with HIGH or CRITICAL risk category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "High-risk assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<CreditRiskAssessment>> getHighRiskAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching high-risk assessments");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CreditRiskAssessment> assessments = creditRiskService.getHighRiskAssessments(pageable);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/threshold/{threshold}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get assessments above threshold", 
               description = "Retrieve assessments with risk score above specified threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<CreditRiskAssessment>> getAssessmentsAboveThreshold(
            @PathVariable Double threshold) {
        
        log.debug("Fetching assessments above threshold {}", threshold);
        
        List<CreditRiskAssessment> assessments = creditRiskService.getAssessmentsAboveThreshold(threshold);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/unreviewed")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get unreviewed assessments", 
               description = "Retrieve assessments that have not been reviewed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unreviewed assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<CreditRiskAssessment>> getUnreviewedAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching unreviewed assessments");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assessedAt"));
        Page<CreditRiskAssessment> assessments = creditRiskService.getUnreviewedAssessments(pageable);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/unreviewed/high-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Get unreviewed high-risk assessments", 
               description = "Retrieve high-risk assessments that have not been reviewed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unreviewed high-risk assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<CreditRiskAssessment>> getUnreviewedHighRiskAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching unreviewed high-risk assessments");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CreditRiskAssessment> assessments = creditRiskService.getUnreviewedHighRiskAssessments(pageable);
        return ResponseEntity.ok(assessments);
    }
    
    @PutMapping("/{assessmentId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST')")
    @Operation(summary = "Mark assessment as reviewed", 
               description = "Mark a credit risk assessment as reviewed with optional notes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment marked as reviewed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<Void> markAsReviewed(
            @PathVariable Long assessmentId,
            @RequestBody(required = false) Map<String, String> body) {
        
        String reviewNotes = body != null ? body.get("reviewNotes") : null;
        log.info("Marking assessment {} as reviewed", assessmentId);
        
        creditRiskService.markAssessmentAsReviewed(assessmentId, reviewNotes);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get risk statistics", 
               description = "Get statistical breakdown of assessments by risk category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<RiskCategory, Long>> getRiskStatistics() {
        log.debug("Fetching risk statistics");
        
        Map<RiskCategory, Long> statistics = creditRiskService.getRiskStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/user/{userId}/average-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'SME_USER', 'AUDITOR')")
    @Operation(summary = "Get average risk score for user", 
               description = "Calculate average risk score across all assessments for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Average score calculated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Double>> getAverageRiskScore(@PathVariable UUID userId) {
        log.debug("Calculating average risk score for user {}", userId);
        
        Double averageScore = creditRiskService.calculateAverageRiskScore(userId);
        return ResponseEntity.ok(Map.of("averageRiskScore", averageScore));
    }
    
    @GetMapping("/sector/{sector}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get assessments by sector", 
               description = "Retrieve assessments filtered by industry sector")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<CreditRiskAssessment>> getAssessmentsBySector(
            @PathVariable String sector,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching assessments for sector {}", sector);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assessedAt"));
        Page<CreditRiskAssessment> assessments = creditRiskService.getAssessmentsBySector(sector, pageable);
        return ResponseEntity.ok(assessments);
    }
    
    @GetMapping("/sector/{sector}/average-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL_ANALYST', 'AUDITOR')")
    @Operation(summary = "Get average risk score by sector", 
               description = "Calculate average risk score for all assessments in a sector")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Average score calculated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Double>> getAverageRiskScoreBySector(@PathVariable String sector) {
        log.debug("Calculating average risk score for sector {}", sector);
        
        Double averageScore = creditRiskService.calculateAverageRiskScoreBySector(sector);
        return ResponseEntity.ok(Map.of("averageRiskScore", averageScore));
    }
    
    @PostMapping("/models/{modelType}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update risk model", 
               description = "Reload/update a specific risk assessment model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Model updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    public ResponseEntity<Map<String, String>> updateModel(@PathVariable String modelType) {
        log.info("Model update requested for {}", modelType);
        
        creditRiskService.updateModel(modelType);
        return ResponseEntity.ok(Map.of("message", "Model " + modelType + " updated successfully"));
    }
}
