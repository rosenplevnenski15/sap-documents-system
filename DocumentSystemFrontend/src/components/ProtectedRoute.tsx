import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { Role } from "../types";

interface Props {
  children: JSX.Element;
  roles?: Role[];
}

export default function ProtectedRoute({ children, roles }: Props) {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(session.user.role)) return <Navigate to="/" replace />;
  return children;
}
