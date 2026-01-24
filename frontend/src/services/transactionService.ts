import api from './api';
import {
  TransactionRequest,
  TransactionResponse,
  TransactionStatistics,
  TransactionStatus
} from '../types/transaction.types';
import { PaginatedResponse, SearchParams } from '../types/common.types';

const API_BASE = '/transactions';

export const transactionService = {
  // Create transaction
  createTransaction: async (data: TransactionRequest): Promise<TransactionResponse> => {
    const response = await api.post(API_BASE, data);
    return response.data;
  },

  // Get transaction by ID
  getTransactionById: async (id: number): Promise<TransactionResponse> => {
    const response = await api.get(`${API_BASE}/${id}`);
    return response.data;
  },

  // Get all transactions (Admin/Auditor/Analyst)
  getAllTransactions: async (params?: SearchParams): Promise<PaginatedResponse<TransactionResponse>> => {
    const response = await api.get(API_BASE, { params });
    return response.data;
  },

  // Get transactions by user ID
  getTransactionsByUserId: async (userId: string, params?: SearchParams): Promise<PaginatedResponse<TransactionResponse>> => {
    const response = await api.get(`${API_BASE}/user/${userId}`, { params });
    return response.data;
  },

  // Search transactions - Note: Backend doesn't have dedicated search endpoint, using base with params
  searchTransactions: async (query: string, params?: SearchParams): Promise<PaginatedResponse<TransactionResponse>> => {
    const response = await api.get(API_BASE, {
      params: { search: query, ...params }
    });
    return response.data;
  },

  // Get transactions by status
  getTransactionsByStatus: async (status: TransactionStatus, params?: SearchParams): Promise<PaginatedResponse<TransactionResponse>> => {
    const response = await api.get(`${API_BASE}/status/${status}`, { params });
    return response.data;
  },

  // Create batch transactions - Note: Backend doesn't have batch endpoint yet
  createBatchTransactions: async (data: TransactionRequest[]): Promise<TransactionResponse[]> => {
    const results = await Promise.all(
      data.map(transaction => api.post(API_BASE, transaction).then(res => res.data))
    );
    return results;
  },

  // Get transaction statistics (Admin/Analyst)
  getTransactionStatistics: async (): Promise<TransactionStatistics> => {
    const response = await api.get(`${API_BASE}/statistics`);
    return response.data;
  },

  // Update transaction status (Admin only) - Backend uses PUT with query param, not PATCH with body
  updateTransactionStatus: async (id: number, status: TransactionStatus): Promise<TransactionResponse> => {
    const response = await api.put(`${API_BASE}/${id}/status`, null, { params: { status } });
    return response.data;
  }
};
