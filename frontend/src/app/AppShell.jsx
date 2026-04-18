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

        <nav className="flex-1 overflow-y-auto px-4 py-2 scrollbar-thin scrollbar-thumb-slate-200 scrollbar-track-transparent">
          <div className="space-y-0.5">
            <NavItem 
              to="/dashboard" 
              isActive={isDashboardActive}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" /></svg>}
            >
              Dashboard
            </NavItem>
            <NavItem 
              to="/notifications" 
              isActive={isActive("/notifications")}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>}
            >
              Notifications
            </NavItem>
          </div>

          <SectionHeading>My Workspace</SectionHeading>
          <div className="space-y-0.5">
            <NavItem 
              to="/my-bookings" 
              isActive={isActive("/my-bookings")}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>}
            >
              My Bookings
            </NavItem>
            <NavItem 
              to="/bookings" 
              isActive={isActive("/bookings") && location.pathname !== "/my-bookings"}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>}
            >
              Create Booking
            </NavItem>
            <NavItem 
              to="/tickets" 
              isActive={isActive("/tickets")}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" /></svg>}
            >
              Support Tickets
            </NavItem>
            <NavItem 
              to="/appeals" 
              isActive={isActive("/appeals")}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 21v-4m0 0V5a2 2 0 012-2h6.5l1 1H21l-3 6 3 6h-8.5l-1-1H5a2 2 0 00-2 2zm9-13.5V9" /></svg>}
            >
              My Appeals
            </NavItem>
          </div>

          <SectionHeading>Facilities</SectionHeading>
          <div className="space-y-0.5">
            <NavItem 
              to="/facilities" 
              isActive={location.pathname === "/facilities" || (isActive("/facilities") && !location.pathname.includes("suggestions") && !location.pathname.includes("underutilized"))}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>}
            >
              Facility Directory
            </NavItem>
            <NavItem 
              to="/facilities/suggestions" 
              isActive={isActive("/facilities/suggestions")}
              icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>}
            >
              Suggestions
            </NavItem>
            {isAdmin && (
              <NavItem 
                to="/facilities/underutilized" 
                isActive={isActive("/facilities/underutilized")}
                icon={<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6" /></svg>}
              >
                Underutilized
              </NavItem>
            )}
          </div>

          {(canApproveBookings || isAdmin || userRoles.includes("FACILITY_MANAGER")) && (
            <>
              <SectionHeading>Management</SectionHeading>
              <div className="space-y-0.5">
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
