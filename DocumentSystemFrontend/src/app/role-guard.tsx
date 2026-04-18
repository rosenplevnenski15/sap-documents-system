import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth.store';
import type { Role } from '../types/api';

export function RoleGuard({ allowedRoles, children }: { allowedRoles: Role[]; children: JSX.Element }) {
  const hasRole = useAuthStore((state) => state.hasRole);

  if (!hasRole(allowedRoles)) {
    return <Navigate to="/documents" replace />;
  }

  return children;
}
