import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import { LoginPage, OAuthCallback } from "./features/auth";
import AppShell from "./app/AppShell";
import {
  BookingsPage,
  FacilitiesAndBookingsPage,
  ApprovalsPage,
  BookingApprovalsPage,
  AdminBookingsPage,
  DashboardPage,
  FacilitiesPage,
  FacilityDetailRoutePage,
  FacilitySuggestionsPage,
  NotFoundPage,
  NotificationsPage,
  TicketDetailPage,
  TicketsPage,
  UnderutilizedPage,
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

            {/* Facilities & Bookings */}
            <Route path="bookings" element={<FacilitiesAndBookingsPage />} />
            <Route
              path="bookings/recommendations"
              element={<BookingRecommendationsPage />}
            />
            <Route path="my-bookings" element={<BookingsPage />} />
            <Route path="facilities" element={<FacilitiesPage />} />
            <Route
              path="facilities/:id"
              element={<FacilityDetailRoutePage />}
            />
            <Route
              path="facilities/underutilized"
              element={<UnderutilizedPage />}
            />
            <Route
              path="facilities/suggestions"
              element={<FacilitySuggestionsPage />}
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
