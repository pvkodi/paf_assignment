import { Outlet, Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

function AppShell() {
  const { user, isAuthenticated, logout } = useAuth();
  const location = useLocation();
  const userRoles = user?.roles || [];
  const isAdmin = userRoles.includes("ADMIN");
  const canApproveBookings = userRoles.some((r) =>
    ["ADMIN", "LECTURER", "FACILITY_MANAGER"].includes(r),
  );

  const isActive = (path) =>
    location.pathname === path || location.pathname.startsWith(path + "/");
  const isDashboardActive = location.pathname === "/" || isActive("/dashboard");

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-4">
          <div className="py-4 mb-4">
            <h1 className="text-xl font-semibold mb-2">
              Smart Campus Operations Hub
            </h1>
            {isAuthenticated && (
              <p className="text-xs text-slate-600">
                Logged in as {user?.displayName || user?.email} • Roles:{" "}
                {user?.roles?.join(", ") || "No roles"}
              </p>
            )}
          </div>

          {/* Navigation */}
          {isAuthenticated && (
            <nav className="flex items-center justify-between border-t border-slate-200 pt-4">
              <div className="flex items-center gap-6">
                <Link
                  to="/dashboard"
                  className={`pb-2 text-sm font-medium transition ${
                    isDashboardActive
                      ? "border-b-2 border-indigo-600 text-indigo-600"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Dashboard
                </Link>
                <Link
                  to="/tickets"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive("/tickets")
                      ? "border-b-2 border-indigo-600 text-indigo-600"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Tickets
                </Link>
                <Link
                  to="/bookings"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive("/bookings")
                      ? "border-b-2 border-indigo-600 text-indigo-600"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Create Booking
                </Link>
                <Link
                  to="/my-bookings"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive("/my-bookings")
                      ? "border-b-2 border-indigo-600 text-indigo-600"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  My Bookings
                </Link>
                <div className="relative group inline-flex items-center">
                  <Link
                    to="/facilities"
                    className={`pb-2 text-sm font-medium transition ${
                      isActive("/facilities")
                        ? "border-b-2 border-indigo-600 text-indigo-600"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    Facilities
                  </Link>

                  <div className="absolute left-0 mt-2 w-48 rounded bg-white border border-gray-200 shadow-md hidden group-hover:block z-10">
                    <div className="py-1">
                      <Link
                        to="/facilities"
                        className="block px-4 py-2 text-sm text-slate-700 hover:bg-gray-50"
                      >
                        Overview
                      </Link>
                      <Link
                        to="/facilities/suggestions"
                        className="block px-4 py-2 text-sm text-slate-700 hover:bg-gray-50"
                      >
                        Suggestions
                      </Link>
                      {isAdmin && (
                        <Link
                          to="/facilities/underutilized"
                          className="block px-4 py-2 text-sm text-slate-700 hover:bg-gray-50"
                        >
                          Underutilized
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
                <Link
                  to="/appeals"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive("/appeals")
                      ? "border-b-2 border-indigo-600 text-indigo-600"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Appeals
                </Link>
                <Link
                  to="/notifications"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive("/notifications")
                      ? "border-b-2 border-indigo-600 text-indigo-600"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Notifications
                </Link>
                {canApproveBookings && (
                  <Link
                    to="/approvals/bookings"
                    className={`pb-2 text-sm font-medium transition ${
                      isActive("/approvals/bookings")
                        ? "border-b-2 border-indigo-600 text-indigo-600"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    📋 Booking Approvals
                  </Link>
                )}
                {(isAdmin || userRoles.includes("FACILITY_MANAGER")) && (
                  <Link
                    to="/admin/bookings"
                    className={`pb-2 text-sm font-medium transition ${
                      isActive("/admin/bookings")
                        ? "border-b-2 border-indigo-600 text-indigo-600"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    📅 Scheduled Bookings
                  </Link>
                )}
                {isAdmin && (
                  <Link
                    to="/admin/bookings"
                    className={`pb-2 text-sm font-medium transition ${
                      isActive("/admin/bookings")
                        ? "border-b-2 border-indigo-600 text-indigo-600"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    📅 All Bookings
                  </Link>
                )}
                {isAdmin && (
                  <Link
                    to="/approvals"
                    className={`pb-2 text-sm font-medium transition ${
                      isActive("/approvals")
                        ? "border-b-2 border-indigo-600 text-indigo-600"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    Approval Queue
                  </Link>
                )}
                {/* Underutilized is available under the Facilities menu for admins */}
                {isAdmin && (
                  <Link
                    to="/analytics"
                    className={`pb-2 text-sm font-medium transition ${
                      isActive("/analytics")
                        ? "border-b-2 border-indigo-600 text-indigo-600"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    Utilization
                  </Link>
                )}
              </div>
              <button
                onClick={logout}
                className="text-xs px-3 py-1 text-slate-600 hover:text-slate-900 transition"
              >
                Sign Out
              </button>
            </nav>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-8">
        <Outlet />
      </main>
      <ToastContainer
        position="bottom-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop={true}
        closeOnClick={true}
        rtl={false}
        pauseOnFocusLoss={true}
        draggable={true}
        pauseOnHover={true}
      />
    </div>
  );
}

export default AppShell;
