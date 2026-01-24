import api from './api';
import {
  FinancialData,
  RiskAssessment,
  RiskReport,
  CreditRiskAssessment,
  RiskStatistics,
  RiskCategory,
  ReviewRequest
} from '../types/credit-risk.types';
import { PaginatedResponse, SearchParams } from '../types/common.types';

const API_BASE = '/credit-risk';

export const creditRiskService = {
  // Assess credit risk
  assessCreditRisk: async (financialData: FinancialData): Promise<RiskAssessment> => {
    const response = await api.post(`${API_BASE}/assess`, financialData);
    return response.data;
  },

  // Generate risk report
  generateRiskReport: async (assessmentId: number): Promise<RiskReport> => {
    const response = await api.get(`${API_BASE}/report/${assessmentId}`);
    return response.data;
  },

  // Get assessment by ID
  getAssessmentById: async (assessmentId: number): Promise<CreditRiskAssessment> => {
    const response = await api.get(`${API_BASE}/${assessmentId}`);
    return response.data;
  },

  // Get assessments by user
  getAssessmentsByUser: async (userId: string): Promise<CreditRiskAssessment[]> => {
    const response = await api.get(`${API_BASE}/user/${userId}`);
    return response.data;
  },

  // Get latest assessment for user
  getLatestAssessment: async (userId: string): Promise<CreditRiskAssessment | null> => {
    const response = await api.get(`${API_BASE}/user/${userId}/latest`);
    return response.data;
  },

  // Get all assessments (paginated)
  getAllAssessments: async (params?: SearchParams): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(API_BASE, { params });
    return response.data;
  },

  // Get assessments by category
  getAssessmentsByCategory: async (
    category: RiskCategory,
    params?: SearchParams
  ): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(`${API_BASE}/category/${category}`, { params });
    return response.data;
  },

  // Get high-risk assessments
  getHighRiskAssessments: async (params?: SearchParams): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(`${API_BASE}/high-risk`, { params });
    return response.data;
  },

  // Get assessments above threshold
  getAssessmentsAboveThreshold: async (
    threshold: number,
    params?: SearchParams
  ): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(`${API_BASE}/threshold/${threshold}`, { params });
    return response.data;
  },

  // Get unreviewed assessments
  getUnreviewedAssessments: async (params?: SearchParams): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(`${API_BASE}/unreviewed`, { params });
    return response.data;
  },

  // Get unreviewed high-risk assessments
  getUnreviewedHighRiskAssessments: async (
    params?: SearchParams
  ): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(`${API_BASE}/unreviewed/high-risk`, { params });
    return response.data;
  },

  // Review assessment
  reviewAssessment: async (
    assessmentId: number,
    reviewData: ReviewRequest
  ): Promise<CreditRiskAssessment> => {
    const response = await api.put(`${API_BASE}/${assessmentId}/review`, reviewData);
    return response.data;
  },

  // Get credit risk statistics
  getRiskStatistics: async (): Promise<RiskStatistics> => {
    const response = await api.get(`${API_BASE}/statistics`);
    return response.data;
  },

  // Get average score by user
  getAverageScoreByUser: async (userId: string): Promise<number> => {
    const response = await api.get(`${API_BASE}/user/${userId}/average-score`);
    return response.data;
  },

  // Get assessments by sector
  getAssessmentsBySector: async (
    sector: string,
    params?: SearchParams
  ): Promise<PaginatedResponse<CreditRiskAssessment>> => {
    const response = await api.get(`${API_BASE}/sector/${sector}`, { params });
    return response.data;
  },

  // Get average score by sector
  getAverageScoreBySector: async (sector: string): Promise<number> => {
    const response = await api.get(`${API_BASE}/sector/${sector}/average-score`);
    return response.data;
  },

  // Update model (Admin only)
  updateModel: async (modelType: string): Promise<{ message: string }> => {
    const response = await api.post(`${API_BASE}/models/${modelType}/update`);
    return response.data;
  },
};
