import {
  BrowserRouter as Router,
  Routes,
  Route,
} from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import { LoginPage, OAuthCallback } from "./features/auth";
import AppShell from "./app/AppShell";
import {
  AnalyticsPage,
  AppealsPage,
  ApprovalsPage,
  DashboardPage,
  NotFoundPage,
  NotificationsPage,
  TicketDetailPage,
  TicketsPage,
} from "./routes/pages";

/**
 * Main App Component
 * Routes application with auth context and protected routes
 * Implements FR-001 (OAuth), FR-002 (roles), FR-003 (suspension)
 */

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/callback" element={<OAuthCallback />} />

          {/* Protected Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppShell />
              </ProtectedRoute>
            }
          >
            {/* Dashboard */}
            <Route index element={<DashboardPage />} />
            <Route path="dashboard" element={<DashboardPage />} />

            {/* Tickets */}
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="tickets/:id" element={<TicketDetailPage />} />

            {/* Notifications */}
            <Route path="notifications" element={<NotificationsPage />} />

            {/* Admin-only features */}
            <Route
              path="approvals"
              element={
                <ProtectedRoute requiredRoles={["ADMIN"]}>
                  <ApprovalsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="analytics"
              element={
                <ProtectedRoute requiredRoles={["ADMIN"]}>
                  <AnalyticsPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Suspended users are allowed to submit appeals */}
          <Route
            path="/appeals"
            element={
              <ProtectedRoute allowSuspended>
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route index element={<AppealsPage />} />
          </Route>

          {/* 404 Not Found */}
          <Route
            path="*"
            element={
              <div className="flex items-center justify-center min-h-screen">
                <div className="text-center">
                  <h1 className="text-4xl font-bold text-gray-900 mb-2">404</h1>
                  <p className="text-gray-600 mb-4">Page not found</p>
                  <a
                    href="/"
                    className="text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    Go back home
                  </a>
                </div>
              </div>
            }
          />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
