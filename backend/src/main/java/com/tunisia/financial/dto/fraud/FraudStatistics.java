package com.tunisia.financial.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for fraud detection statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudStatistics {
    private Long totalPatterns;
    private Long resolvedPatterns;
    private Long unresolvedPatterns;
    private Map<String, Long> patternsBySeverity;
    private Map<String, Long> patternsByType;
    private List<PatternTimeSeries> patternsOverTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternTimeSeries {
        private String date;
        private Long count;
    }
}
