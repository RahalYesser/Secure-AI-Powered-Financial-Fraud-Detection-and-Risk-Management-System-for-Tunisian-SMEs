// User Roles
export enum UserRole {
  ADMIN = 'ADMIN',
  FINANCIAL_ANALYST = 'FINANCIAL_ANALYST',
  SME_USER = 'SME_USER',
  AUDITOR = 'AUDITOR',
}

// User Types
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  accountLocked: boolean;
  lastLoginAt?: string;
  createdAt: string;
}

// Auth Request/Response Types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  userId: string;
  email: string;
  role: UserRole;
  expiresIn: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role?: UserRole;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  accountLocked: boolean;
  lastLoginAt?: string;
  createdAt: string;
}

// Error Types
export interface ApiError {
  message: string;
  status: number;
  timestamp: string;
  path?: string;
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface ValidationErrorResponse {
  message: string;
  status: number;
  timestamp: string;
  errors: ValidationError[];
}
