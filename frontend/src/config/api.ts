// API Configuration
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// API Endpoints
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: `${API_URL}/auth/login`,
    REGISTER: `${API_URL}/auth/register`,
    ME: `${API_URL}/auth/me`,
    CHANGE_PASSWORD: `${API_URL}/auth/change-password`,
    LOGOUT: `${API_URL}/auth/logout`,
  },
  USERS: {
    BASE: `${API_URL}/users`,
    BY_ID: (id: string) => `${API_URL}/users/${id}`,
    STATISTICS: `${API_URL}/users/statistics`,
  },
  TRANSACTIONS: {
    BASE: `${API_URL}/transactions`,
    BY_ID: (id: string) => `${API_URL}/transactions/${id}`,
    SEARCH: `${API_URL}/transactions/search`,
  },
  FRAUD: {
    BASE: `${API_URL}/fraud`,
    PATTERNS: `${API_URL}/fraud/patterns`,
  },
};

// App Configuration
export const APP_CONFIG = {
  APP_NAME: import.meta.env.VITE_APP_NAME || 'Financial Fraud Detection',
  APP_VERSION: import.meta.env.VITE_APP_VERSION || '1.0.0',
};
