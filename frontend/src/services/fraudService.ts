import api from './api';
import {
  FraudDetectionResult,
  FraudPatternResponse,
  FraudStatistics
} from '../types/fraud.types';
import { PaginatedResponse, SearchParams } from '../types/common.types';

const API_BASE = '/fraud';

export const fraudService = {
  // Detect fraud for transaction
  detectFraud: async (transactionId: number): Promise<FraudDetectionResult> => {
    const response = await api.post(`${API_BASE}/detect/${transactionId}`);
    return response.data;
  },

  // Get all fraud patterns
  getAllPatterns: async (params?: SearchParams): Promise<PaginatedResponse<FraudPatternResponse>> => {
    const response = await api.get(`${API_BASE}/patterns`, { params });
    return response.data;
  },

  // Get fraud patterns by transaction ID
  getPatternsByTransactionId: async (transactionId: number): Promise<FraudPatternResponse[]> => {
    const response = await api.get(`${API_BASE}/patterns/transaction/${transactionId}`);
    return response.data;
  },

  // Get fraud patterns by user ID
  getPatternsByUserId: async (userId: string, params?: SearchParams): Promise<PaginatedResponse<FraudPatternResponse>> => {
    // Note: Backend doesn't have this endpoint yet, using transactions to filter
    const response = await api.get(`${API_BASE}/patterns`, { params: { userId, ...params } });
    return response.data;
  },

  // Batch detect fraud
  batchDetectFraud: async (transactionIds: number[]): Promise<FraudDetectionResult[]> => {
    // Note: Backend doesn't have batch endpoint, detect individually
    const results = await Promise.all(
      transactionIds.map(id => api.post(`${API_BASE}/detect/${id}`).then(res => res.data))
    );
    return results;
  },

  // Get fraud statistics
  getFraudStatistics: async (): Promise<FraudStatistics> => {
    const response = await api.get(`${API_BASE}/statistics`);
    return response.data;
  },

  // Review fraud pattern (Admin/Auditor only) - changed from resolve to review
  reviewPattern: async (patternId: number, reviewNotes?: string): Promise<FraudPatternResponse> => {
    const response = await api.put(`${API_BASE}/patterns/${patternId}/review`, { reviewNotes });
    return response.data;
  }
};
