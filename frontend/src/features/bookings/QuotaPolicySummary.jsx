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
    if (!max) return "bg-green-500"; // Unlimited
    const percentage = (current / max) * 100;
    if (percentage >= 90) return "bg-red-500";
    if (percentage >= 70) return "bg-yellow-500";
    return "bg-green-500";
  };

  const getProgressPercentage = (current, max) => {
    if (!max) return 0;
    return Math.min((current / max) * 100, 100);
  };

  if (loading) {
    return (
      <div className={`bg-slate-50 rounded-lg p-4 border border-slate-200 ${compact ? "" : ""}`}>
        <p className="text-sm text-slate-600">Loading quota information...</p>
      </div>
    );
  }

  const quota = quotaStatus || getDefaultQuota(userRole);
  const isAdmin = quota.effectiveRole === "ADMIN" || userRole === "ADMIN";

  if (compact) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <p className="text-sm font-semibold text-blue-900 mb-2">
          Your Quota Status ({quota.effectiveRole || userRole})
        </p>
        <div className="space-y-1 text-xs text-blue-800">
          <p>
            Weekly Bookings:{" "}
            <span className="font-mono font-bold">
              {quota.weeklyBookings || 0}/{quota.weeklyQuota || "∞"}
            </span>
          </p>
          <p>
            Monthly Bookings:{" "}
            <span className="font-mono font-bold">
              {quota.monthlyBookings || 0}/{quota.monthlyQuota || "∞"}
            </span>
          </p>
          <p>
            Advance Window:{" "}
            <span className="font-mono font-bold">{quota.advanceBookingDays || "∞"} days</span>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6 border-l-4 border-blue-500">
      <div className="mb-4">
        <h3 className="text-lg font-bold text-slate-900 mb-1">Quota Policy Summary</h3>
        <p className="text-sm text-slate-600">Role: <span className="font-semibold">{quota.effectiveRole || userRole}</span></p>
      </div>

      {isAdmin ? (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-sm text-green-800 font-semibold">
            ✅ Admin users have unlimited booking quotas
          </p>
          <p className="text-xs text-green-700 mt-1">
            You can create unlimited bookings and book any time in advance.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Weekly Bookings */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium text-slate-700">Weekly Bookings</span>
              <span className="text-sm font-bold text-slate-900">
                {quota.weeklyBookings || 0}/{quota.weeklyQuota || "∞"}
              </span>
            </div>
            <div className="w-full bg-slate-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${getProgressColor(
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
            <p className="text-xs text-slate-600 mt-1">
              You can book up to {quota.weeklyQuota || "∞"} times per week (Monday-Sunday).
            </p>
          </div>

          {/* Monthly Bookings */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium text-slate-700">Monthly Bookings</span>
              <span className="text-sm font-bold text-slate-900">
                {quota.monthlyBookings || 0}/{quota.monthlyQuota || "∞"}
              </span>
            </div>
            <div className="w-full bg-slate-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${getProgressColor(
                  quota.monthlyBookings || 0,
                  quota.monthlyQuota,
                )}`}
                style={{
                  width: `${getProgressPercentage(quota.monthlyBookings || 0, quota.monthlyQuota)}%`,
                }}
              />
            </div>
            <p className="text-xs text-slate-600 mt-1">
              You can book up to {quota.monthlyQuota || "∞"} times per calendar month.
            </p>
          </div>

          {/* Peak Hours */}
          <div className="bg-blue-50 border border-blue-100 rounded-lg p-3">
            <div className="flex justify-between items-center mb-1">
              <span className="text-sm font-medium text-blue-900">Peak Hour Access</span>
              <span className="text-sm font-bold text-blue-900">
                {quota.canBookDuringPeakHours ? "✓ Allowed" : "✗ Restricted"}
              </span>
            </div>
            <p className="text-xs text-blue-700">
              Peak hours: {quota.peakHourWindow || "08:00-10:00"}
            </p>
          </div>

          {/* Advance Booking Window */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium text-slate-700">Advance Booking Window</span>
              <span className="text-sm font-bold text-slate-900">
                ≤ {quota.advanceBookingDays || "∞"} days
              </span>
            </div>
            <p className="text-xs text-slate-600">
              You can book facilities up to {quota.advanceBookingDays || "∞"} days in advance
              {quota.advanceBookingUntil ? ` (until ${quota.advanceBookingUntil})` : ""}.
            </p>
          </div>

          {/* Usage Warnings */}
          {quota.weeklyBookings && quota.weeklyQuota && quota.weeklyBookings >= quota.weeklyQuota * 0.8 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
              <p className="text-xs font-semibold text-yellow-800">
                ⚠️ Approaching weekly booking limit
              </p>
              <p className="text-xs text-yellow-700 mt-1">
                You have {quota.weeklyRemaining} booking{quota.weeklyRemaining === 1 ? "" : "s"} remaining
                this week.
              </p>
            </div>
          )}

          {quota.monthlyBookings && quota.monthlyQuota && quota.monthlyBookings >= quota.monthlyQuota * 0.8 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
              <p className="text-xs font-semibold text-yellow-800">
                ⚠️ Approaching monthly booking limit
              </p>
              <p className="text-xs text-yellow-700 mt-1">
                You have {quota.monthlyRemaining} booking{quota.monthlyRemaining === 1 ? "" : "s"} remaining
                this month.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
