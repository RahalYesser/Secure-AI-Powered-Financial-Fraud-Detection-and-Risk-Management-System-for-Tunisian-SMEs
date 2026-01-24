export interface ModelPrediction {
  modelName: string;
  confidence: number;
  isFraud: boolean;
  reason: string;
}

export interface FraudDetectionResult {
  isFraud: boolean;
  confidence: number;
  primaryReason: string;
  modelPredictions: ModelPrediction[];
  fraudScore: number;
}

export interface FraudPatternResponse {
  id: number;
  transactionId: number;
  userId: string;
  patternType: string;
  severity: string;
  description: string;
  detectedAt: string;
  resolved: boolean;
}

export interface FraudStatistics {
  totalPatterns: number;
  resolvedPatterns: number;
  unresolvedPatterns: number;
  patternsBySeverity: Record<string, number>;
  patternsOverTime: Array<{
    date: string;
    count: number;
  }>;
}
