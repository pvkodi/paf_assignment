import React, { useMemo, useState } from "react";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";

function todayDateString() {
  return new Date().toISOString().slice(0, 10);
}

function pastDateString(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}

function formatPercent(value) {
  const numeric = Number(value);
  if (Number.isNaN(numeric)) {
    return "0.00%";
  }
  return `${numeric.toFixed(2)}%`;
}

function formatDayOfWeek(dayOfWeek) {
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  if (dayOfWeek < 0 || dayOfWeek > 6) {
    return "-";
  }
  return days[dayOfWeek];
}

export function UtilizationDashboard() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [fromDate, setFromDate] = useState(pastDateString(30));
  const [toDate, setToDate] = useState(todayDateString());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [analytics, setAnalytics] = useState({
    heatmap: [],
    underutilizedFacilities: [],
    recommendations: [],
  });

  const summary = useMemo(() => {
    return {
      heatmapCount: analytics.heatmap.length,
      underutilizedCount: analytics.underutilizedFacilities.length,
      recommendationCount: analytics.recommendations.length,
    };
  }, [analytics]);

  const fetchAnalytics = async (event) => {
    if (event) {
      event.preventDefault();
    }

    if (!fromDate || !toDate || fromDate > toDate) {
      setError("Select a valid date range.");
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.get("/v1/analytics/utilization", {
        params: {
          from: fromDate,
          to: toDate,
        },
      });

      const payload = response?.data || {};
      setAnalytics({
        heatmap: Array.isArray(payload.heatmap) ? payload.heatmap : [],
        underutilizedFacilities: Array.isArray(payload.underutilizedFacilities)
          ? payload.underutilizedFacilities
          : [],
        recommendations: Array.isArray(payload.recommendations) ? payload.recommendations : [],
      });
    } catch (requestError) {
      console.error("Failed to load analytics", requestError);
      setError("Failed to load analytics. Ensure you are signed in as ADMIN.");
    } finally {
      setLoading(false);
    }
  };

  if (!isAdmin) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-slate-900">Utilization Dashboard</h2>
        <p className="mt-2 text-sm text-slate-600">
          This dashboard is restricted to administrators.
        </p>
      </section>
    );
  }

  return (
    <section className="space-y-5">
      <header className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="text-xl font-semibold text-slate-900">Utilization Dashboard</h2>
        <p className="mt-1 text-sm text-slate-600">
          Analyze facility utilization trends and optimization opportunities.
        </p>
      </header>

      <form onSubmit={fetchAnalytics} className="rounded-xl border border-slate-200 bg-white p-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <label className="text-sm text-slate-700">
            <span className="mb-1 block">From</span>
            <input
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
            />
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-1 block">To</span>
            <input
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
            />
          </label>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? "Loading..." : "Run Analysis"}
            </button>
          </div>
        </div>
      </form>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="grid gap-4 sm:grid-cols-3">
        <article className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">Heatmap Entries</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{summary.heatmapCount}</p>
        </article>
        <article className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">Underutilized Facilities</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{summary.underutilizedCount}</p>
        </article>
        <article className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">Recommendations</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{summary.recommendationCount}</p>
        </article>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Underutilized Facilities</h3>
        {analytics.underutilizedFacilities.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">No underutilized facilities found for this range.</p>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-2 py-2">Facility</th>
                  <th className="px-2 py-2">Utilization</th>
                  <th className="px-2 py-2">Consecutive Days</th>
                  <th className="px-2 py-2">Recommendation</th>
                </tr>
              </thead>
              <tbody>
                {analytics.underutilizedFacilities.map((facility) => (
                  <tr key={facility.facilityId} className="border-b border-slate-100 align-top">
                    <td className="px-2 py-2 text-slate-700">{facility.facilityName}</td>
                    <td className="px-2 py-2 text-slate-700">{formatPercent(facility.utilizationPercent)}</td>
                    <td className="px-2 py-2 text-slate-700">{facility.consecutiveUnderutilizedDays || 0}</td>
                    <td className="px-2 py-2 text-slate-600">{facility.recommendation}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Heatmap Sample</h3>
        {analytics.heatmap.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">No heatmap data for this range.</p>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-2 py-2">Facility</th>
                  <th className="px-2 py-2">Day</th>
                  <th className="px-2 py-2">Hour</th>
                  <th className="px-2 py-2">Utilization</th>
                </tr>
              </thead>
              <tbody>
                {analytics.heatmap.slice(0, 30).map((entry, index) => (
                  <tr key={`${entry.facilityId}-${entry.dayOfWeek}-${entry.hourOfDay}-${index}`} className="border-b border-slate-100">
                    <td className="px-2 py-2 text-slate-700">{entry.facilityName}</td>
                    <td className="px-2 py-2 text-slate-700">{formatDayOfWeek(entry.dayOfWeek)}</td>
                    <td className="px-2 py-2 text-slate-700">{entry.hourOfDay}</td>
                    <td className="px-2 py-2 text-slate-700">{formatPercent(entry.utilizationPercent)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}

export default UtilizationDashboard;
