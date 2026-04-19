import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import { LoginPage, OAuthCallback } from "./features/auth";
import { RegistrationPendingPage } from "./pages/RegistrationPendingPage";
import AppShell from "./app/AppShell";
import AdminUserManagementPanel from "./features/admin/AdminUserManagementPanel";
import {
  BookingsPage,
  FacilitiesAndBookingsPage,
  ApprovalsPage,
  BookingApprovalsPage,
  AdminBookingsPage,
  DashboardPage,
  FacilitiesPage,
  FacilityDetailRoutePage,
  TimetableImportPreviewPage,
  NotFoundPage,
  NotificationsPage,
  TicketDetailPage,
  TicketsPage,
  BookingRecommendationsPage,
  AnalyticsPage,
  AppealsPage,
  QuickCheckInPageWrapper,
  QRCodeGeneratorPage,
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
          <Route
            path="/registration-pending"
            element={<RegistrationPendingPage />}
          />

          {/* Protected Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppShell />
              </ProtectedRoute>
            }
          >
            {/* Dashboard - Not accessible to TECHNICIAN */}
            <Route
              index
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="dashboard"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />

            {/* Facilities & Bookings - Not accessible to TECHNICIAN */}
            <Route
              path="bookings"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <FacilitiesAndBookingsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="bookings/recommendations"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <BookingRecommendationsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="my-bookings"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <BookingsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="facilities"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <FacilitiesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="facilities/:id"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <FacilityDetailRoutePage />
                </ProtectedRoute>
              }
            />

            <Route
              path="facilities/timetable-preview"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER"]}>
                  <TimetableImportPreviewPage />
                </ProtectedRoute>
              }
            />

            {/* Booking Approvals - for LECTURER, FACILITY_MANAGER, ADMIN */}
            <Route
              path="approvals/bookings"
              element={
                <ProtectedRoute
                  requiredRoles={["LECTURER", "FACILITY_MANAGER", "ADMIN"]}
                >
                  <BookingApprovalsPage />
                </ProtectedRoute>
              }
            />

            {/* Admin Bookings - All scheduled bookings for ADMIN/FACILITY_MANAGER */}
            <Route
              path="admin/bookings"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER"]}>
                  <AdminBookingsPage />
                </ProtectedRoute>
              }
            />

            {/* QR Code Generator - for ADMIN/FACILITY_MANAGER to generate check-in QR codes */}
            <Route
              path="qr-code-generator"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER"]}>
                  <QRCodeGeneratorPage />
                </ProtectedRoute>
              }
            />

            {/* Tickets - Accessible to all authenticated users including TECHNICIAN */}
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="tickets/:id" element={<TicketDetailPage />} />

            {/* Notifications - Accessible to all authenticated users including TECHNICIAN */}
            <Route path="notifications" element={<NotificationsPage />} />

            {/* Appeals - Not accessible to TECHNICIAN */}
            <Route
              path="appeals"
              element={
                <ProtectedRoute requiredRoles={["ADMIN", "FACILITY_MANAGER", "USER", "LECTURER"]}>
                  <AppealsPage />
                </ProtectedRoute>
              }
            />

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
            <Route
              path="admin/user-management"
              element={
                <ProtectedRoute requiredRoles={["ADMIN"]}>
                  <AdminUserManagementPanel />
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

          {/* QR Code Quick Check-In Route */}
          <Route
            path="/check-in/booking/:bookingId"
            element={
              <ProtectedRoute>
                <QuickCheckInPageWrapper />
              </ProtectedRoute>
            }
          />

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
