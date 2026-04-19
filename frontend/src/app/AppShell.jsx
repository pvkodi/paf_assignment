import { Outlet, Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

function AppShell() {
  const { user, isAuthenticated, logout } = useAuth();
  const location = useLocation();
  const userRoles = user?.roles || [];
  const isAdmin = userRoles.includes("ADMIN");
  const isFacilityManager = userRoles.includes("FACILITY_MANAGER");
  const isTechnician = userRoles.includes("TECHNICIAN");
  const canApproveBookings = userRoles.some((r) =>
    ["ADMIN", "LECTURER", "FACILITY_MANAGER"].includes(r),
  );

  const isActive = (path) =>
    location.pathname === path || location.pathname.startsWith(path + "/");
  const isDashboardActive = location.pathname === "/" || isActive("/dashboard");

  return (
    <div className="min-h-screen flex bg-slate-50 text-slate-900">
      {/* Sidebar */}
      {isAuthenticated && (
        <aside className="w-64 h-screen sticky top-0 bg-white border-r border-slate-200 flex flex-col">
          {/* Brand/Logo */}
          <div className="p-6 border-b border-slate-200">
            <h1 className="text-lg font-semibold text-indigo-600">VenueLink</h1>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-hidden px-4 py-6 space-y-1">
            {/* Technician: Show only Tickets and Notifications */}
            {isTechnician ? (
              <>
                <Link
                  to="/tickets"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/tickets")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Tickets
                </Link>
                <Link
                  to="/notifications"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/notifications")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Notifications
                </Link>
              </>
            ) : (
              <>
                {/* Dashboard - Admin and Facility Managers only */}
                {(isAdmin || isFacilityManager) && (
                  <Link
                    to="/dashboard"
                    className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                      isDashboardActive
                        ? "bg-indigo-50 text-indigo-600"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    Dashboard
                  </Link>
                )}
                <Link
                  to="/tickets"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/tickets")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Tickets
                </Link>
                <Link
                  to="/bookings"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/bookings") && !isActive("/admin/bookings")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Create Booking
                </Link>
                <Link
                  to="/my-bookings"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/my-bookings")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  My Bookings
                </Link>
                <Link
                  to="/facilities"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/facilities")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Facilities
                </Link>
                <Link
                  to="/appeals"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/appeals")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Appeals
                </Link>
                <Link
                  to="/notifications"
                  className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                    isActive("/notifications")
                      ? "bg-indigo-50 text-indigo-600"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Notifications
                </Link>
                {canApproveBookings && (
                  <Link
                    to="/approvals/bookings"
                    className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                      isActive("/approvals/bookings")
                        ? "bg-indigo-50 text-indigo-600"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    Booking Approvals
                  </Link>
                )}
                {(isAdmin || userRoles.includes("FACILITY_MANAGER")) && (
                  <Link
                    to="/admin/bookings"
                    className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                      isActive("/admin/bookings")
                        ? "bg-indigo-50 text-indigo-600"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    All Bookings
                  </Link>
                )}
                {isAdmin && (
                  <Link
                    to="/approvals"
                    className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                      isActive("/approvals") && !isActive("/approvals/bookings")
                        ? "bg-indigo-50 text-indigo-600"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    Approval Queue
                  </Link>
                )}
                {isAdmin && (
                  <Link
                    to="/admin/user-management"
                    className={`block px-4 py-2.5 text-sm font-medium rounded-lg transition ${
                      isActive("/admin/user-management")
                        ? "bg-indigo-50 text-indigo-600"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    User Management
                  </Link>
                )}
              </>
            )}
          </nav>

          {/* User Info & Logout */}
          <div className="border-t border-slate-200 p-4 space-y-4">
            {isAuthenticated && (
              <div className="text-xs text-slate-600 space-y-1">
                <p className="font-medium text-slate-900">
                  {user?.displayName || user?.email}
                </p>
                <p className="text-slate-500">{user?.roles?.join(", ") || "No roles"}</p>
              </div>
            )}
            <button
              onClick={logout}
              className="w-full px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
            >
              Sign Out
            </button>
          </div>
        </aside>
      )}

      {/* Main Content */}
      <main className="flex-1 flex flex-col">
        {isAuthenticated && (
          <header className="bg-white border-b border-slate-200 px-8 py-4">
            <h2 className="text-xl font-semibold text-slate-900">
              VenueLink Operations Hub
            </h2>
          </header>
        )}
        <div className="flex-1 overflow-y-auto px-8 py-8">
          <Outlet />
        </div>
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
