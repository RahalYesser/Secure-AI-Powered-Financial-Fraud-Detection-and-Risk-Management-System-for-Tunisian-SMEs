import api from './api';
import {
  UserResponse,
  UserRegistrationRequest,
  UserUpdateRequest,
  PasswordChangeRequest,
  UserStatistics,
  UserRole
} from '../types/user.types';
import { PaginatedResponse, SearchParams } from '../types/common.types';

const API_BASE = '/users';

export const userService = {
  // Get user by ID
  getUserById: async (userId: string): Promise<UserResponse> => {
    const response = await api.get(`${API_BASE}/${userId}`);
    return response.data;
  },

  // Update user
  updateUser: async (userId: string, data: UserUpdateRequest): Promise<UserResponse> => {
    const response = await api.put(`${API_BASE}/${userId}`, data);
    return response.data;
  },

  // Lock user account (Admin only)
  lockUser: async (userId: string): Promise<void> => {
    await api.post(`${API_BASE}/${userId}/lock`);
  },

  // Unlock user account (Admin only)
  unlockUser: async (userId: string): Promise<void> => {
    await api.post(`${API_BASE}/${userId}/unlock`);
  },

  // Delete user (Admin only)
  deleteUser: async (userId: string): Promise<void> => {
    await api.delete(`${API_BASE}/${userId}`);
  },

  // Get all users (Admin/Auditor)
  getAllUsers: async (params?: SearchParams): Promise<PaginatedResponse<UserResponse>> => {
    const response = await api.get(`${API_BASE}`, { params });
    return response.data;
  },

  // Search users
  searchUsers: async (query: string, params?: SearchParams): Promise<PaginatedResponse<UserResponse>> => {
    const response = await api.get(`${API_BASE}/search`, {
      params: { query, ...params }
    });
    return response.data;
  },

  // Get users by role
  getUsersByRole: async (role: UserRole, params?: SearchParams): Promise<PaginatedResponse<UserResponse>> => {
    const response = await api.get(`${API_BASE}/by-role/${role}`, { params });
    return response.data;
  },

  // Get user statistics (Admin only)
  getUserStatistics: async (): Promise<UserStatistics> => {
    const response = await api.get(`${API_BASE}/statistics`);
    return response.data;
  },

  // Register new user (Admin only)
  registerUser: async (data: UserRegistrationRequest): Promise<UserResponse> => {
    const response = await api.post('/api/v1/auth/register', data);
    return response.data;
  },

  // Change password
  changePassword: async (data: PasswordChangeRequest): Promise<void> => {
    await api.post('/api/v1/auth/change-password', data);
  }
};
