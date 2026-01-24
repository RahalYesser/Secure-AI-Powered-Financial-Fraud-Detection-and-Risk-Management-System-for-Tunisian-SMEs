import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { UserRole } from '../types/user.types';
import { PermissionKey, hasPermission, hasRole, hasAnyRole } from '../utils/permissions';

/**
 * Hook to check user permissions
 */
export const usePermission = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('usePermission must be used within AuthProvider');
  }
  const { user } = context;

  const checkPermission = (permission: PermissionKey): boolean => {
    return hasPermission(user?.role, permission);
  };

  const checkAnyRole = (roles: UserRole[]): boolean => {
    return hasAnyRole(user?.role, roles);
  };

  const checkRole = (role: UserRole): boolean => {
    return hasRole(user?.role, role);
  };

  return {
    hasPermission: checkPermission,
    hasAnyRole: checkAnyRole,
    hasRole: checkRole,
    user,
    userRole: user?.role,
  };
};

/**
 * Hook to get user role
 */
export const useRole = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useRole must be used within AuthProvider');
  }
  const { user } = context;
  return user?.role;
};

/**
 * Hook to check if user is admin
 */
export const useIsAdmin = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useIsAdmin must be used within AuthProvider');
  }
  const { user } = context;
  return user?.role === UserRole.ADMIN;
};

/**
 * Hook to check if user is financial analyst
 */
export const useIsAnalyst = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useIsAnalyst must be used within AuthProvider');
  }
  const { user } = context;
  return user?.role === UserRole.FINANCIAL_ANALYST;
};

/**
 * Hook to check if user is auditor
 */
export const useIsAuditor = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useIsAuditor must be used within AuthProvider');
  }
  const { user } = context;
  return user?.role === UserRole.AUDITOR;
};

/**
 * Hook to check if user is SME user
 */
export const useIsSMEUser = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useIsSMEUser must be used within AuthProvider');
  }
  const { user } = context;
  return user?.role === UserRole.SME_USER;
};
