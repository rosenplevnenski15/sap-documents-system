import { Navigate, createBrowserRouter } from 'react-router-dom';
import { AppLayout } from '../components/layout/app-layout';
import { AppShell } from '../components/layout/app-shell';
import { ProtectedRoute } from './protected-route';
import { RoleGuard } from './role-guard';
import { LoginPage } from '../pages/auth/login-page';
import { RegisterPage } from '../pages/auth/register-page';
import { DocumentsPage } from '../pages/documents/documents-page';
import { AuditPage } from '../pages/audit/audit-page';
import { AdminUsersPage } from '../pages/admin/admin-users-page';
import { NotFoundPage } from '../pages/not-found-page';

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      {
        path: '/auth/login',
        element: <LoginPage />,
      },
      {
        path: '/auth/register',
        element: <RegisterPage />,
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <AppShell />,
            children: [
              {
                path: '/',
                element: <Navigate to="/documents" replace />,
              },
              {
                path: '/documents',
                element: <DocumentsPage />,
              },
              {
                path: '/audit',
                element: <AuditPage />,
              },
              {
                path: '/admin/users',
                element: (
                  <RoleGuard allowedRoles={['ADMIN']}>
                    <AdminUsersPage />
                  </RoleGuard>
                ),
              },
            ],
          },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
