import { Outlet, Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { ToastContainer } from "react-toastify";
import { useState } from "react";
import "react-toastify/dist/ReactToastify.css";

function NavItem({ to, children, isActive, icon }) {
  return (
    <Link
      to={to}
      className={`flex items-center gap-4 px-4 py-3 rounded-xl transition-all text-sm font-semibold mb-1 ${
        isActive
          ? "bg-[#49BBBB]/10 text-[#49BBBB]"
          : "text-slate-400 hover:bg-slate-50 hover:text-slate-700"
      }`}
    >
      <span className="shrink-0">{icon}</span>
      <span className="truncate">{children}</span>
    </Link>
  );
}

function SectionHeading({ children }) {
  return (
    <div className="px-4 py-2 mt-4 mb-2 text-[10px] font-bold uppercase tracking-widest text-slate-300">
      {children}
    </div>
  );
}

function AppShell() {
  const { user, isAuthenticated, logout } = useAuth();
  const location = useLocation();
  const userRoles = user?.roles || [];
  const isAdmin = userRoles.includes("ADMIN");
  const canApproveBookings = userRoles.some((r) =>
    ["ADMIN", "LECTURER", "FACILITY_MANAGER"].includes(r)
  );

  const isActive = (path) =>
    location.pathname === path || location.pathname.startsWith(path + "/");
  const isDashboardActive = location.pathname === "/" || isActive("/dashboard");

  const [facilitiesOpen, setFacilitiesOpen] = useState(isActive("/facilities"));

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-[#f8f9fa] text-slate-900">
        <main className="mx-auto max-w-7xl px-4 py-8">
          <Outlet />
        </main>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-[#f4f7fe] overflow-hidden font-sans">
      {/* Sidebar */}
      <aside className="w-[260px] bg-white flex flex-col shrink-0 border-r border-slate-100/50 z-20">
        <div className="h-24 flex items-center px-8 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-[#49BBBB] flex items-center justify-center shadow-[0_4px_12px_rgba(73,187,187,0.3)]">
              <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h1 className="text-xl font-extrabold text-slate-900 tracking-tight">Smart<span className="text-[#49BBBB]">Campus</span></h1>
          </div>
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
                  <NavItem 
                    to="/approvals/bookings" 
                    isActive={isActive("/approvals/bookings")}
                    icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
                  >
                    Booking Approvals
                  </NavItem>
                )}
                {(isAdmin || userRoles.includes("FACILITY_MANAGER")) && (
                  <NavItem 
                    to="/admin/bookings" 
                    isActive={isActive("/admin/bookings")}
                    icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>}
                  >
                    All Bookings
                  </NavItem>
                )}
                {isAdmin && (
                  <NavItem 
                    to="/approvals" 
                    isActive={isActive("/approvals") && location.pathname === "/approvals"}
                    icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg>}
                  >
                    System Approvals
                  </NavItem>
                )}
                {isAdmin && (
                  <NavItem 
                    to="/analytics" 
                    isActive={isActive("/analytics")}
                    icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z" /></svg>}
                  >
                    Analytics
                  </NavItem>
                )}
                {isAdmin && (
                  <NavItem 
                    to="/admin/user-management" 
                    isActive={isActive("/admin/user-management")}
                    icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>}
                  >
                    User Management
                  </NavItem>
                )}
              </div>
            </>
          )}
        </nav>

        <div className="p-6 border-t border-slate-100 shrink-0">
          <button
            onClick={logout}
            className="flex items-center justify-center gap-2 w-full px-4 py-3 rounded-xl transition-all text-sm font-semibold text-slate-500 hover:bg-slate-50 hover:text-red-500"
          >
            <svg className="w-5 h-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Container */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden relative">
        <header className="h-24 bg-[#f4f7fe] flex items-center justify-between px-8 shrink-0 z-10 sticky top-0">
          <div>
             <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none mb-1">Pages</p>
             <h2 className="text-2xl font-bold text-slate-800 capitalize leading-none tracking-tight">
                {location.pathname === "/" ? "Dashboard" : location.pathname.substring(1).replace(/-/g, ' ').split('/')[0]}
             </h2>
          </div>
          <div className="flex items-center gap-4 bg-white px-3 py-1.5 rounded-full shadow-[0_4px_20px_rgb(0,0,0,0.03)]">
            <div className="text-right pl-2">
               <p className="text-sm font-bold text-slate-700 leading-tight">
                 {user?.displayName || "User"}
               </p>
               <p className="text-[10px] font-bold text-[#49BBBB] uppercase tracking-wider leading-tight">
                 {user?.roles?.[0]?.replace('_', ' ') || "Guest"}
               </p>
            </div>
            <div className="w-10 h-10 rounded-full bg-[#49BBBB] flex items-center justify-center text-white font-bold shadow-[0_2px_8px_rgba(73,187,187,0.3)]">
              {(user?.displayName || user?.email || "U")?.[0]?.toUpperCase()}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto px-4 pb-8 md:px-8 scrollbar-thin scrollbar-thumb-slate-200 scrollbar-track-transparent">
          <div className="mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>

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
