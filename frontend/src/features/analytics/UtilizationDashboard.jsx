import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import {
  fetchFacilities,
  fetchFacilityUtilization,
  fetchUnderutilizedFacilities,
} from "../facilities/api";

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

function toDateTimeRange(fromDate, toDate) {
  return {
    start: `${fromDate}T00:00:00`,
    end: `${toDate}T23:59:59`,
  };
}

function shiftDate(dateText, daysToShift) {
  const date = new Date(`${dateText}T00:00:00`);
  date.setDate(date.getDate() + daysToShift);
  return date.toISOString().slice(0, 10);
}

function LineChart({ points }) {
  if (!points.length) {
    return <p className="text-sm text-gray-500">No chart data available.</p>;
  }

  const width = 600;
  const height = 220;
  const padding = 24;
  const maxValue = Math.max(...points.map((point) => Number(point.value)), 1);

  const coordinates = points.map((point, index) => {
    const x = padding + (index * (width - padding * 2)) / Math.max(points.length - 1, 1);
    const y = height - padding - (Number(point.value) / maxValue) * (height - padding * 2);
    return { x, y };
  });

  const linePath = coordinates
    .map((coordinate, index) => `${index === 0 ? "M" : "L"} ${coordinate.x} ${coordinate.y}`)
    .join(" ");

  return (
    <div className="space-y-2">
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full rounded-lg border border-gray-200 bg-gray-50">
        <path d={linePath} fill="none" stroke="#4f46e5" strokeWidth="3" />
        {coordinates.map((coordinate, index) => (
          <circle key={points[index].label} cx={coordinate.x} cy={coordinate.y} r="4" fill="#4f46e5" />
        ))}
      </svg>
      <div className="grid grid-cols-4 gap-2 text-xs text-gray-600 md:grid-cols-7">
        {points.map((point) => (
          <div key={point.label} className="text-center">
            <p>{point.label}</p>
            <p className="font-medium text-gray-800">{formatPercent(point.value)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

export function UtilizationDashboard() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [fromDate, setFromDate] = useState(pastDateString(30));
  const [toDate, setToDate] = useState(todayDateString());
  const [facilityId, setFacilityId] = useState("");
  const [facilityOptions, setFacilityOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [metrics, setMetrics] = useState({
    totalAvailableHours: 0,
    totalBookedHours: 0,
    utilizationPercentage: 0,
  });
  const [underutilizedFacilities, setUnderutilizedFacilities] = useState([]);
  const [chartPoints, setChartPoints] = useState([]);

  useEffect(() => {
    const loadFacilities = async () => {
      try {
        const payload = await fetchFacilities({ page: 0, size: 100 });
        const content = Array.isArray(payload?.content) ? payload.content : [];
        setFacilityOptions(content);
        if (content.length > 0) {
          setFacilityId((current) => current || content[0].id);
        }
      } catch {
        setFacilityOptions([]);
      }
    };

    loadFacilities();
  }, []);

  const selectedFacilityName = useMemo(() => {
    const selected = facilityOptions.find((facility) => facility.id === facilityId);
    return selected?.name || "Selected Facility";
  }, [facilityOptions, facilityId]);

  const summaryCards = useMemo(() => {
    return {
      utilizationPercentage: Number(metrics.utilizationPercentage || 0),
      totalAvailableHours: Number(metrics.totalAvailableHours || 0),
      totalBookedHours: Number(metrics.totalBookedHours || 0),
    };
  }, [metrics]);

  const loadChartSeries = async (selectedFacilityId) => {
    const dayLabels = [6, 5, 4, 3, 2, 1, 0];
    const pointRequests = dayLabels.map(async (offset) => {
      const day = shiftDate(toDate, -offset);
      const { start, end } = toDateTimeRange(day, day);
      const data = await fetchFacilityUtilization(selectedFacilityId, start, end);
      return {
        label: day.slice(5),
        value: Number(data?.utilizationPercentage || 0),
      };
    });

    const points = await Promise.all(pointRequests);
    setChartPoints(points);
  };

  const fetchAnalytics = async (event) => {
    if (event) {
      event.preventDefault();
    }

    if (!facilityId || !fromDate || !toDate || fromDate > toDate) {
      setError("Select a valid date range.");
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const { start, end } = toDateTimeRange(fromDate, toDate);
      const [utilizationData, underutilizedData] = await Promise.all([
        fetchFacilityUtilization(facilityId, start, end),
        fetchUnderutilizedFacilities(`${toDate}T23:59:59`),
      ]);

      setMetrics({
        totalAvailableHours: Number(utilizationData?.totalAvailableHours || 0),
        totalBookedHours: Number(utilizationData?.totalBookedHours || 0),
        utilizationPercentage: Number(utilizationData?.utilizationPercentage || 0),
      });
      setUnderutilizedFacilities(Array.isArray(underutilizedData) ? underutilizedData : []);

      await loadChartSeries(facilityId);
    } catch {
      setError("Failed to load utilization analytics.");
    } finally {
      setLoading(false);
    }
  };

  if (!isAdmin) {
    return (
      <section className="rounded-lg border border-gray-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-gray-900">Access Restricted</h2>
        <p className="mt-2 text-sm text-gray-600">
          The Utilization Dashboard is only available to administrators.
        </p>
        <p className="mt-1 text-xs text-gray-500">Contact your system administrator to request access.</p>
      </section>
    );
  }

  return (
    <section className="space-y-5">
      <header className="rounded-lg border border-gray-200 bg-white p-5">
        <h2 className="text-xl font-semibold text-gray-900">Facility Utilization Dashboard</h2>
        <p className="mt-1 text-sm text-gray-600">
          Analyze facility usage patterns, identify optimization opportunities, and track underutilized facilities.
        </p>
      </header>

      <form onSubmit={fetchAnalytics} className="rounded-lg border border-gray-200 bg-white p-5">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">Select Facility and Date Range</h3>
        <div className="grid gap-4 sm:grid-cols-4">
          <label className="text-sm text-gray-700">
            <span className="mb-2 block font-medium">Facility</span>
            <select
              value={facilityId}
              onChange={(event) => setFacilityId(event.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {facilityOptions.map((facility) => (
                <option key={facility.id} value={facility.id}>
                  {facility.name}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-2 block font-medium">From Date</span>
            <input
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-2 block font-medium">To Date</span>
            <input
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </label>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? "Analyzing..." : "Analyze"}
            </button>
          </div>
        </div>
      </form>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3">
          <div>
            <h4 className="font-semibold text-red-800">Analysis Failed</h4>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      <section className="grid gap-4 sm:grid-cols-3">
        <article className="rounded-lg border border-gray-200 bg-white p-4">
          <p className="text-xs uppercase tracking-wide text-gray-500">Utilization</p>
          <p className="mt-2 text-2xl font-semibold text-gray-900">{formatPercent(summaryCards.utilizationPercentage)}</p>
          <p className="mt-1 text-xs text-gray-600">{selectedFacilityName}</p>
        </article>
        <article className="rounded-lg border border-gray-200 bg-white p-4">
          <p className="text-xs uppercase tracking-wide text-gray-500">Available Hours</p>
          <p className="mt-2 text-2xl font-semibold text-gray-900">{summaryCards.totalAvailableHours.toFixed(2)}</p>
          <p className="mt-1 text-xs text-gray-600">Within selected range</p>
        </article>
        <article className="rounded-lg border border-gray-200 bg-white p-4">
          <p className="text-xs uppercase tracking-wide text-gray-500">Booked Hours</p>
          <p className="mt-2 text-2xl font-semibold text-gray-900">{summaryCards.totalBookedHours.toFixed(2)}</p>
          <p className="mt-1 text-xs text-gray-600">From booking integration boundary</p>
        </article>
      </section>

      <section className="rounded-lg border border-gray-200 bg-white p-5">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-4">Utilization Over Time</h3>
        <LineChart points={chartPoints} />
      </section>

      <section className="rounded-lg border border-gray-200 bg-white p-5">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-4">Underutilized Facilities</h3>
        {underutilizedFacilities.length === 0 ? (
          <p className="mt-3 text-sm text-gray-600">No underutilized facilities found for this range.</p>
        ) : (
          <div className="space-y-3">
            {underutilizedFacilities.map((facility) => {
              const critical = Number(facility.utilizationPercentage) < 20;
              return (
                <div
                  key={facility.facilityId}
                  className={`border rounded-lg p-4 ${critical ? "border-red-200 bg-red-50" : "border-gray-200 bg-white"}`}
                >
                  <div className="flex justify-between items-start mb-2">
                    <div className="flex-1">
                      <h4 className="font-semibold text-gray-900">{facility.facilityName}</h4>
                      <p className="text-xs text-gray-500 mt-1">
                        Consecutive low-utilization days: {facility.consecutiveUnderutilizedDays || 0}
                      </p>
                    </div>
                    <div className={`px-3 py-1 rounded-full text-sm font-semibold ${critical ? "bg-red-100 text-red-700" : "bg-gray-100 text-gray-700"}`}>
                      {formatPercent(facility.utilizationPercentage)}
                    </div>
                  </div>
                  <div className="text-xs text-gray-600 flex justify-between">
                    <span>Status: {facility.status}</span>
                    <span>{facility.persistentForSevenDays ? "Persistent (>=7 days)" : "Observed"}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </section>
  );
}

export default UtilizationDashboard;
