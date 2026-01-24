export enum RiskCategory {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export interface FinancialData {
  smeUserId: string;
  totalRevenue: number;
  totalExpenses: number;
  totalAssets: number;
  totalLiabilities: number;
  cashFlow: number;
  outstandingDebt: number;
  creditHistory: number; // Score 0-100
  industryRisk: number; // Score 0-100
  marketConditions: number; // Score 0-100
  sector?: string;
  additionalMetrics?: Record<string, number>;
}

export interface ModelPrediction {
  modelName: string;
  riskScore: number;
  predictedCategory: RiskCategory;
  rationale: string;
}

export interface RiskAssessment {
  assessmentId: number;
  smeUserId: string;
  riskScore: number;
  riskCategory: RiskCategory;
  assessmentSummary: string;
  modelPredictions: ModelPrediction[];
  assessedAt: string;
}

export interface RiskReport {
  reportId: number;
  assessmentId: number;
  smeUserId: string;
  businessName: string;
  riskCategory: RiskCategory;
  overallRiskScore: number;
  financialMetrics: {
    debtRatio: number;
    currentRatio: number;
    profitMargin: number;
    cashFlowScore: number;
    liquidityStatus: string;
  };
  historicalAnalysis: {
    yearsAnalyzed: number;
    revenueTrend: string;
    profitabilityTrend: string;
    numberOfDefaults: number;
    averagePaymentDelay: number;
  };
  marketConditions: {
    industrySector: string;
    marketOutlook: string;
    sectorRiskScore: number;
    economicIndicators: string;
  };
  keyRiskFactors: Array<{
    factorName: string;
    severity: RiskCategory;
    impactScore: number;
    description: string;
  }>;
  recommendations: string[];
  executiveSummary: string;
  generatedAt: string;
}

export interface CreditRiskAssessment {
  id: number;
  riskScore: number;
  riskCategory: RiskCategory;
  assessmentSummary?: string;
  financialData?: string;
  annualRevenue?: number;
  totalAssets?: number;
  totalLiabilities?: number;
  debtRatio?: number;
  industrySector?: string;
  yearsInBusiness?: number;
  creditHistoryScore?: number;
  modelPredictions?: string;
  ensembleMethod?: string;
  marketConditions?: string;
  assessedAt: string;
  reviewed: boolean;
  reviewNotes?: string;
  updatedAt?: string;
}

export type RiskStatistics = Record<RiskCategory | string, number>;

export interface AssessmentRequest {
  financialData: FinancialData;
}

export interface ReviewRequest {
  reviewNotes?: string;
}
