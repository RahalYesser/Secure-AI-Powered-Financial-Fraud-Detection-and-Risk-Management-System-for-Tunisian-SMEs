import { ReactNode } from 'react';
import { usePermission } from '../../hooks/usePermission';
import { PermissionKey } from '../../utils/permissions';
import { UserRole } from '../../types/user.types';

interface ProtectedComponentProps {
  children: ReactNode;
  permission?: PermissionKey;
  roles?: UserRole[];
  fallback?: ReactNode;
}

/**
 * Component wrapper that shows content only if user has permission
 */
export const ProtectedComponent = ({ 
  children, 
  permission, 
  roles, 
  fallback = null 
}: ProtectedComponentProps) => {
  const { hasPermission, hasAnyRole } = usePermission();

  const hasAccess = permission 
    ? hasPermission(permission)
    : roles 
    ? hasAnyRole(roles)
    : true;

  if (!hasAccess) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};

interface RoleBasedComponentProps {
  children: ReactNode;
  allowedRoles: UserRole[];
  fallback?: ReactNode;
}

/**
 * Component wrapper that shows content only for specific roles
 */
export const RoleBasedComponent = ({ 
  children, 
  allowedRoles, 
  fallback = null 
}: RoleBasedComponentProps) => {
  const { hasAnyRole } = usePermission();

  if (!hasAnyRole(allowedRoles)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};
