import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

/**
 * Protected Route Component
 * Wraps routes that require authentication and optionally role-based access control
 * Enforces FR-002 (role enforcement) and FR-003 (suspension blocking)
 */

export function ProtectedRoute({
  children,
  requiredRoles = null,
  allowSuspended = false,
  loadingComponent = null,
}) {
  const { isAuthenticated, user, loading, isSuspended } = useAuth();
  const location = useLocation();

  // Show loading state while checking authentication
  if (loading) {
    return (
      loadingComponent || (
        <div className="flex items-center justify-center min-h-screen">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
        </div>
      )
    );
  }

  // Not authenticated - redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check suspension status
  if (isSuspended() && !allowSuspended) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 px-4">
        <div className="max-w-md text-center rounded-lg border border-red-200 bg-white p-6">
          <h1 className="text-2xl font-bold text-red-900 mb-2">
            Account Suspended
          </h1>
          <p className="text-red-700 mb-6">
            Your account has been suspended. Please contact support or submit an
            appeal.
          </p>
          <a
            href="/appeals"
            className="inline-block px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
          >
            Submit Appeal
          </a>
        </div>
      </div>
    );
  }

  // Check role-based access
  if (requiredRoles) {
    const hasRequiredRole = Array.isArray(requiredRoles)
      ? user?.roles?.some((role) => requiredRoles.includes(role))
      : user?.roles?.includes(requiredRoles);

    if (!hasRequiredRole) {
      return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50 px-4">
          <div className="max-w-md text-center rounded-lg border border-yellow-200 bg-white p-6">
            <h1 className="text-2xl font-bold text-yellow-900 mb-2">
              Access Denied
            </h1>
            <p className="text-yellow-700 mb-6">
              You don't have permission to access this page. Your role(s) are:{" "}
              {user?.roles?.join(", ")}
            </p>
            <a
              href="/"
              className="inline-block px-6 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 transition"
            >
              Go Home
            </a>
          </div>
        </div>
      );
    }
  }

  // All checks passed
  return children;
}

export default ProtectedRoute;
