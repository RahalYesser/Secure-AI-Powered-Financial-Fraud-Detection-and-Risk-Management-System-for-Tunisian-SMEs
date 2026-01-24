export enum UserRole {
  ADMIN = 'ADMIN',
  FINANCIAL_ANALYST = 'FINANCIAL_ANALYST',
  SME_USER = 'SME_USER',
  AUDITOR = 'AUDITOR'
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  accountLocked: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface UserRegistrationRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

export interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: UserRole;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UserStatistics {
  totalUsers: number;
  lockedUsers: number;
  adminCount: number;
  analystCount: number;
  smeUserCount: number;
  auditorCount: number;
}
