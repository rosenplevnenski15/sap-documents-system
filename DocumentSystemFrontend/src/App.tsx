import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import OperationsPage from "./pages/OperationsPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import DocumentRegistryPage from "./pages/DocumentRegistryPage";

export default function App() {
  const { session } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<OperationsPage />} />
        <Route path="documents" element={<DocumentRegistryPage />} />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to={session ? "/" : "/login"} replace />} />
    </Routes>
  );
}
