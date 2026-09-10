import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

interface ProtectedRouteProps {
  children: React.ReactElement;
  requiredRole?: 'ADMIN' | 'AGENT';
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requiredRole }) => {
  const { isAuthenticated, session } = useAuthStore();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && session?.role !== requiredRole && session?.role !== 'ADMIN') {
    // Admin has access everywhere, agent only has access to agent
    return <Navigate to="/track" replace />;
  }

  return children;
};
