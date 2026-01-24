import { UserRole } from '../types/user.types';

// Permission definitions based on backend @PreAuthorize annotations
export const Permissions = {
  // User Management
  USER_VIEW_ALL: [UserRole.ADMIN, UserRole.AUDITOR] as UserRole[],
  USER_MANAGE: [UserRole.ADMIN] as UserRole[],
  USER_LOCK_UNLOCK: [UserRole.ADMIN] as UserRole[],
  USER_DELETE: [UserRole.ADMIN] as UserRole[],
  
  // Transaction Management
  TRANSACTION_CREATE: [UserRole.ADMIN, UserRole.FINANCIAL_ANALYST, UserRole.SME_USER, UserRole.AUDITOR] as UserRole[],
  TRANSACTION_VIEW_ALL: [UserRole.ADMIN, UserRole.AUDITOR, UserRole.FINANCIAL_ANALYST] as UserRole[],
  TRANSACTION_VIEW_OWN: [UserRole.SME_USER] as UserRole[],
  TRANSACTION_UPDATE_STATUS: [UserRole.ADMIN] as UserRole[],
  TRANSACTION_CANCEL: [UserRole.ADMIN] as UserRole[],
  
  // Fraud Detection
  FRAUD_DETECT: [UserRole.ADMIN, UserRole.AUDITOR, UserRole.FINANCIAL_ANALYST] as UserRole[],
  FRAUD_VIEW_PATTERNS: [UserRole.ADMIN, UserRole.AUDITOR, UserRole.FINANCIAL_ANALYST] as UserRole[],
  FRAUD_RESOLVE: [UserRole.ADMIN] as UserRole[],
  FRAUD_REVIEW: [UserRole.ADMIN, UserRole.AUDITOR, UserRole.FINANCIAL_ANALYST] as UserRole[],
  
  // Credit Risk Assessment
  CREDIT_RISK_ASSESS: [UserRole.ADMIN, UserRole.FINANCIAL_ANALYST] as UserRole[],
  CREDIT_RISK_VIEW: [UserRole.ADMIN, UserRole.FINANCIAL_ANALYST, UserRole.SME_USER, UserRole.AUDITOR] as UserRole[],
  CREDIT_RISK_REVIEW: [UserRole.ADMIN, UserRole.FINANCIAL_ANALYST] as UserRole[],
  CREDIT_RISK_GENERATE_REPORT: [UserRole.ADMIN, UserRole.FINANCIAL_ANALYST, UserRole.SME_USER, UserRole.AUDITOR] as UserRole[],
} as const;

export type PermissionKey = keyof typeof Permissions;

/**
 * Check if user has permission
 */
export const hasPermission = (userRole: UserRole | undefined, permission: PermissionKey): boolean => {
  if (!userRole) return false;
  return Permissions[permission].includes(userRole);
};

/**
 * Check if user has any of the specified roles
 */
export const hasAnyRole = (userRole: UserRole | undefined, roles: UserRole[]): boolean => {
  if (!userRole) return false;
  return roles.includes(userRole);
};

/**
 * Check if user has specific role
 */
export const hasRole = (userRole: UserRole | undefined, role: UserRole): boolean => {
  return userRole === role;
};

/**
 * Check if user can view all users
 */
export const canViewAllUsers = (userRole: UserRole | undefined): boolean => {
  return hasPermission(userRole, 'USER_VIEW_ALL');
};

/**
 * Check if user can manage users (CRUD)
 */
export const canManageUsers = (userRole: UserRole | undefined): boolean => {
  return hasPermission(userRole, 'USER_MANAGE');
};

/**
 * Check if user can view all transactions
 */
export const canViewAllTransactions = (userRole: UserRole | undefined): boolean => {
  return hasPermission(userRole, 'TRANSACTION_VIEW_ALL');
};

/**
 * Check if user can only view own transactions
 */
export const canOnlyViewOwnTransactions = (userRole: UserRole | undefined): boolean => {
  return hasRole(userRole, UserRole.SME_USER);
};

/**
 * Check if user can access fraud detection features
 */
export const canAccessFraudDetection = (userRole: UserRole | undefined): boolean => {
  return hasPermission(userRole, 'FRAUD_DETECT');
};

/**
 * Check if user can modify resource (own or admin)
 */
export const canModifyResource = (
  userRole: UserRole | undefined,
  resourceUserId: string,
  currentUserId: string
): boolean => {
  if (!userRole) return false;
  return userRole === UserRole.ADMIN || resourceUserId === currentUserId;
};
