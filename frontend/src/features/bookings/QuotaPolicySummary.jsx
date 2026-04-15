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
  const isAdmin = userRole === "ADMIN";

  if (compact) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <p className="text-sm font-semibold text-blue-900 mb-2">
          📊 Your Quota Status ({userRole})
        </p>
        <div className="space-y-1 text-xs text-blue-800">
          <p>
            Concurrent Bookings:{" "}
            <span className="font-mono font-bold">
              {quota.currentConcurrent}/{quota.maxConcurrentBookings || "∞"}
            </span>
          </p>
          <p>
            Peak Hour Slots This Week:{" "}
            <span className="font-mono font-bold">
              {quota.currentPeakHours}/{quota.maxPeakHoursPerWeek || "∞"}
            </span>
          </p>
          <p>
            Max Advance Booking:{" "}
            <span className="font-mono font-bold">{quota.maxAdvanceDays || "∞"} days</span>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6 border-l-4 border-blue-500">
      <div className="mb-4">
        <h3 className="text-lg font-bold text-slate-900 mb-1">📊 Quota Policy Summary</h3>
        <p className="text-sm text-slate-600">Role: <span className="font-semibold">{userRole}</span></p>
      </div>

      {isAdmin ? (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-sm text-green-800 font-semibold">
            ✅ Admin users have unlimited booking quotas
          </p>
          <p className="text-xs text-green-700 mt-1">
            You can create unlimited concurrent bookings and book any time in advance.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Concurrent Bookings */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium text-slate-700">Concurrent Bookings</span>
              <span className="text-sm font-bold text-slate-900">
                {quota.currentConcurrent}/{quota.maxConcurrentBookings}
              </span>
            </div>
            <div className="w-full bg-slate-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${getProgressColor(
                  quota.currentConcurrent,
                  quota.maxConcurrentBookings,
                )}`}
                style={{
                  width: `${getProgressPercentage(
                    quota.currentConcurrent,
                    quota.maxConcurrentBookings,
                  )}%`,
                }}
              />
            </div>
            <p className="text-xs text-slate-600 mt-1">
              You can have up to {quota.maxConcurrentBookings} active bookings at once.
            </p>
          </div>

          {/* Peak Hour Slots */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium text-slate-700">Peak Hour Slots (This Week)</span>
              <span className="text-sm font-bold text-slate-900">
                {quota.currentPeakHours}/{quota.maxPeakHoursPerWeek}
              </span>
            </div>
            <div className="w-full bg-slate-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${getProgressColor(
                  quota.currentPeakHours,
                  quota.maxPeakHoursPerWeek,
                )}`}
                style={{
                  width: `${getProgressPercentage(quota.currentPeakHours, quota.maxPeakHoursPerWeek)}%`,
                }}
              />
            </div>
            <p className="text-xs text-slate-600 mt-1">
              Peak hours: 10:00-12:00, 14:00-16:00. Limited to {quota.maxPeakHoursPerWeek} slots
              per week.
            </p>
          </div>

          {/* Advance Booking Window */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium text-slate-700">Advance Booking Window</span>
              <span className="text-sm font-bold text-slate-900">
                ≤ {quota.maxAdvanceDays} days
              </span>
            </div>
            <p className="text-xs text-slate-600">
              You can book facilities up to {quota.maxAdvanceDays} days in advance. Bookings
              beyond this date will be rejected.
            </p>
          </div>

          {/* Usage Warnings */}
          {quota.currentConcurrent >= quota.maxConcurrentBookings * 0.8 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
              <p className="text-xs font-semibold text-yellow-800">
                ⚠️ Approaching concurrent booking limit
              </p>
              <p className="text-xs text-yellow-700 mt-1">
                You have {quota.maxConcurrentBookings - quota.currentConcurrent} booking
                {quota.maxConcurrentBookings - quota.currentConcurrent === 1 ? "" : "s"} remaining
                this cycle.
              </p>
            </div>
          )}

          {quota.currentPeakHours >= quota.maxPeakHoursPerWeek * 0.8 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
              <p className="text-xs font-semibold text-yellow-800">
                ⚠️ Approaching peak hour limit
              </p>
              <p className="text-xs text-yellow-700 mt-1">
                You have {quota.maxPeakHoursPerWeek - quota.currentPeakHours} peak hour
                {quota.maxPeakHoursPerWeek - quota.currentPeakHours === 1 ? "" : "s"} remaining
                this week.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
