import React, { useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * QuotaPolicySummary Component
 * Displays the user's current role-based quota limits and usage.
 */
export default function QuotaPolicySummary({ userRole, compact = false }) {
  const [quotaStatus, setQuotaStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchQuotaStatus();
  }, [userRole]);

  const fetchQuotaStatus = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get("/v1/bookings/quota-status");
      setQuotaStatus(response.data);
    } catch (error) {
      console.error("Failed to fetch quota status:", error);
      // Set default quota based on role
      setQuotaStatus(getDefaultQuota(userRole));
    } finally {
      setLoading(false);
    }
  };

  const getDefaultQuota = (role) => {
    const quotas = {
      USER: {
        maxConcurrentBookings: 3,
        maxPeakHoursPerWeek: 2,
        maxAdvanceDays: 14,
        currentConcurrent: 1,
        currentPeakHours: 0,
      },
      LECTURER: {
        maxConcurrentBookings: 5,
        maxPeakHoursPerWeek: 3,
        maxAdvanceDays: 30,
        currentConcurrent: 2,
        currentPeakHours: 1,
      },
      ADMIN: {
        maxConcurrentBookings: null, // Unlimited
        maxPeakHoursPerWeek: null,
        maxAdvanceDays: null,
        currentConcurrent: 0,
        currentPeakHours: 0,
      },
    };
    return quotas[role] || quotas.USER;
  };

  const getProgressColor = (current, max) => {
    if (!max) return "bg-[#10b981]"; // Unlimited
    const percentage = (current / max) * 100;
    if (percentage >= 90) return "bg-[#ef4444]";
    if (percentage >= 70) return "bg-[#f59e0b]";
    return "bg-[#10b981]";
  };

  const getProgressPercentage = (current, max) => {
    if (!max) return 0;
    return Math.min((current / max) * 100, 100);
  };

  if (loading) {
    return (
      <div className={`bg-[#f8fafc] rounded-2xl p-6 border border-[#e2e8f0] flex items-center justify-center ${compact ? "p-4" : ""}`}>
        <div className="w-5 h-5 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mr-3"></div>
        <p className="text-sm font-medium text-[#64748b]">Loading quota status...</p>
      </div>
    );
  }

  const quota = quotaStatus || getDefaultQuota(userRole);
  const isAdmin = quota.effectiveRole === "ADMIN" || userRole === "ADMIN";

  if (compact) {
    return (
      <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-4 shadow-sm">
        <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-2">
          Quota Status <span className="text-[#64748b]">({quota.effectiveRole || userRole})</span>
        </p>
        <div className="space-y-2 text-xs text-[#0f172a] font-medium">
          <div className="flex justify-between">
            <span className="text-[#475569]">Weekly Bookings:</span>
            <span className="font-mono bg-white border border-[#e2e8f0] px-1.5 rounded">
              {quota.weeklyBookings || 0}/{quota.weeklyQuota || "∞"}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#475569]">Monthly Bookings:</span>
            <span className="font-mono bg-white border border-[#e2e8f0] px-1.5 rounded">
              {quota.monthlyBookings || 0}/{quota.monthlyQuota || "∞"}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#475569]">Advance Window:</span>
            <span className="font-mono bg-white border border-[#e2e8f0] px-1.5 rounded">
              {quota.advanceBookingDays || "∞"} days
            </span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl shadow-sm p-6 border border-[#e2e8f0]">
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h3 className="text-lg font-bold text-[#0f172a] tracking-tight mb-1">Quota Policy</h3>
          <p className="text-sm text-[#64748b] font-medium flex items-center gap-2">
            Active Role: <span className="bg-[#f1f5f9] px-2 py-0.5 rounded-md text-[#0f172a] text-xs uppercase tracking-wider">{quota.effectiveRole || userRole}</span>
          </p>
        </div>
        <div className="w-10 h-10 bg-[#f8fafc] rounded-full border border-[#e2e8f0] flex items-center justify-center">
          <svg className="w-5 h-5 text-[#64748b]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
        </div>
      </div>

      {isAdmin ? (
        <div className="bg-[#f0fdf4] border border-[#dcfce3] rounded-xl p-5">
          <p className="text-sm text-[#166534] font-bold flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            Unlimited Admin Quota
          </p>
          <p className="text-sm text-[#15803d] mt-2">
            As an admin, you have no restrictions on concurrent bookings, advance scheduling, or peak hour usage.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Weekly Bookings */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-semibold text-[#0f172a]">Weekly Limit</span>
              <span className="text-sm font-bold font-mono bg-[#f8fafc] px-2 py-0.5 rounded border border-[#e2e8f0]">
                {quota.weeklyBookings || 0} / {quota.weeklyQuota || "∞"}
              </span>
            </div>
            <div className="w-full bg-[#f1f5f9] rounded-full h-2 overflow-hidden border border-[#e2e8f0]/50">
              <div
                className={`h-full rounded-full transition-all duration-500 ease-out ${getProgressColor(
                  quota.weeklyBookings || 0,
                  quota.weeklyQuota,
                )}`}
                style={{
                  width: `${getProgressPercentage(
                    quota.weeklyBookings || 0,
                    quota.weeklyQuota,
                  )}%`,
                }}
              />
            </div>
            <p className="text-xs text-[#64748b] mt-1.5 font-medium">
              Maximum of {quota.weeklyQuota || "∞"} bookings per week (Monday-Sunday).
            </p>
          </div>

          {/* Monthly Bookings */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-semibold text-[#0f172a]">Monthly Limit</span>
              <span className="text-sm font-bold font-mono bg-[#f8fafc] px-2 py-0.5 rounded border border-[#e2e8f0]">
                {quota.monthlyBookings || 0} / {quota.monthlyQuota || "∞"}
              </span>
            </div>
            <div className="w-full bg-[#f1f5f9] rounded-full h-2 overflow-hidden border border-[#e2e8f0]/50">
              <div
                className={`h-full rounded-full transition-all duration-500 ease-out ${getProgressColor(
                  quota.monthlyBookings || 0,
                  quota.monthlyQuota,
                )}`}
                style={{
                  width: `${getProgressPercentage(quota.monthlyBookings || 0, quota.monthlyQuota)}%`,
                }}
              />
            </div>
            <p className="text-xs text-[#64748b] mt-1.5 font-medium">
              Maximum of {quota.monthlyQuota || "∞"} bookings per calendar month.
            </p>
          </div>

          {/* Restrictions Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Peak Hours */}
            <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-4">
              <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-2">Peak Hours</p>
              <div className="flex items-center gap-2 mb-1">
                {quota.canBookDuringPeakHours ? (
                  <svg className="w-4 h-4 text-[#10b981]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
                ) : (
                  <svg className="w-4 h-4 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                )}
                <span className="text-sm font-bold text-[#0f172a]">
                  {quota.canBookDuringPeakHours ? "Allowed" : "Restricted"}
                </span>
              </div>
              <p className="text-xs text-[#64748b] font-medium">
                Window: {quota.peakHourWindow || "08:00-10:00"}
              </p>
            </div>

            {/* Advance Booking Window */}
            <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-4">
              <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-2">Advance Booking</p>
              <div className="flex items-center gap-2 mb-1">
                <svg className="w-4 h-4 text-[#6366f1]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                <span className="text-sm font-bold text-[#0f172a]">
                  ≤ {quota.advanceBookingDays || "∞"} Days
                </span>
              </div>
              <p className="text-xs text-[#64748b] font-medium truncate">
                {quota.advanceBookingUntil ? `Until ${quota.advanceBookingUntil}` : "Rolling window"}
              </p>
            </div>
          </div>

          {/* Usage Warnings */}
          <div className="space-y-3">
            {quota.weeklyBookings && quota.weeklyQuota && quota.weeklyBookings >= quota.weeklyQuota * 0.8 && (
              <div className="bg-[#fffbeb] border border-[#fde68a] rounded-xl p-4 flex gap-3">
                <svg className="w-5 h-5 text-[#b45309] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                <div>
                  <p className="text-sm font-bold text-[#92400e]">
                    Approaching weekly limit
                  </p>
                  <p className="text-xs text-[#b45309] mt-0.5 font-medium">
                    You have <span className="font-bold">{quota.weeklyRemaining}</span> booking{quota.weeklyRemaining === 1 ? "" : "s"} remaining this week.
                  </p>
                </div>
              </div>
            )}

            {quota.monthlyBookings && quota.monthlyQuota && quota.monthlyBookings >= quota.monthlyQuota * 0.8 && (
              <div className="bg-[#fffbeb] border border-[#fde68a] rounded-xl p-4 flex gap-3">
                <svg className="w-5 h-5 text-[#b45309] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                <div>
                  <p className="text-sm font-bold text-[#92400e]">
                    Approaching monthly limit
                  </p>
                  <p className="text-xs text-[#b45309] mt-0.5 font-medium">
                    You have <span className="font-bold">{quota.monthlyRemaining}</span> booking{quota.monthlyRemaining === 1 ? "" : "s"} remaining this month.
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
