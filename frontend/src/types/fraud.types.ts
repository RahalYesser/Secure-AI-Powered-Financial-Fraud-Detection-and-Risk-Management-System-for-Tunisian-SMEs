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
  patternType: string;
  description: string;
  confidence: number;
  transactionId: number;
  detectorModel: string;
  detectedAt: string;
  reviewed: boolean;
}

export interface FraudStatistics {
  totalPatterns: number;
  resolvedPatterns: number;
  unresolvedPatterns: number;
  patternsBySeverity: Record<string, number>;
  patternsByType: Record<string, number>;
  patternsOverTime: Array<{
    date: string;
    count: number;
  }>;
}
