package com.tunisia.financial.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for transaction trends over time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTrends {
    private List<TrendDataPoint> trends;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String date;
        private Long count;
        private BigDecimal amount;
    }
}
