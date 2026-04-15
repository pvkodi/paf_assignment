import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import {
  fetchFacilities,
  fetchFacilityUtilization,
  fetchUnderutilizedFacilities,
} from "../facilities/api";
import { fetchCampusUtilization } from "./api";

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

// Normalize backend (snake_case) -> frontend (camelCase) analytics payloads
function normalizeAnalytics(raw) {
  if (!raw) return raw;
  const normalized = { ...raw };

  if (Array.isArray(raw.heatmap)) {
    normalized.heatmap = raw.heatmap.map((e) => ({
      facilityId: e.facility_id ?? e.facilityId,
      facilityName: e.facility_name ?? e.facilityName,
      dayOfWeek: e.day_of_week ?? e.dayOfWeek,
      hourOfDay: e.hour_of_day ?? e.hourOfDay,
      utilizationPercent: e.utilization_percent ?? e.utilizationPercent,
    }));
  }

  const underutilized = raw.underutilizedFacilities ?? raw.underutilized_facilities ?? [];
  if (Array.isArray(underutilized)) {
    normalized.underutilizedFacilities = underutilized.map((f) => ({
      facilityId: f.facility_id ?? f.facilityId,
      facilityName: f.facility_name ?? f.facilityName,
      utilizationPercent: f.utilization_percent ?? f.utilizationPercent,
      consecutiveUnderutilizedDays: f.consecutive_underutilized_days ?? f.consecutiveUnderutilizedDays,
      recommendation: f.recommendation ?? f.recommendation,
    }));
  } else {
    normalized.underutilizedFacilities = raw.underutilizedFacilities || [];
  }

  return normalized;
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
        {/* horizontal grid lines */}
        {[0.25, 0.5, 0.75].map((g, i) => (
          <line
            key={`grid-${i}`}
            x1={padding}
            x2={width - padding}
            y1={height - padding - g * (height - padding * 2)}
            y2={height - padding - g * (height - padding * 2)}
            stroke="#e6e7eb"
            strokeWidth="1"
          />
        ))}
        <path d={linePath} fill="none" stroke="#4f46e5" strokeWidth="3" />
        {coordinates.map((coordinate, index) => (
          <circle key={`dot-${index}-${points[index]?.label ?? index}`} cx={coordinate.x} cy={coordinate.y} r="4" fill="#4f46e5" />
        ))}
      </svg>
      <div className="grid grid-cols-4 gap-2 text-xs text-gray-600 md:grid-cols-7">
        {points.map((point, idx) => (
          <div key={`label-${idx}-${point.label}`} className="text-center">
            <p>{point.label}</p>
            <p className="font-medium text-gray-800">{formatPercent(point.value)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function HeatmapLegend() {
  return (
    <div className="flex items-center justify-center gap-3 text-xs mt-3 flex-wrap">
      <div className="flex items-center gap-1"><div className="w-3 h-3 bg-red-600 rounded" /><span>0-20%</span></div>
      <div className="flex items-center gap-1"><div className="w-3 h-3 bg-orange-500 rounded" /><span>20-40%</span></div>
      <div className="flex items-center gap-1"><div className="w-3 h-3 bg-yellow-400 rounded" /><span>40-60%</span></div>
      <div className="flex items-center gap-1"><div className="w-3 h-3 bg-lime-400 rounded" /><span>60-80%</span></div>
      <div className="flex items-center gap-1"><div className="w-3 h-3 bg-green-600 rounded" /><span>80%+</span></div>
    </div>
  );
}

function DayHourHeatmap({ heatmapData = [] }) {
  if (!heatmapData.length) return <p className="text-sm text-gray-500">No heatmap data available.</p>;
  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const grid = Array(7).fill(null).map(() => Array(24).fill(0));
  const cellCounts = Array(7).fill(null).map(() => Array(24).fill(0));
  heatmapData.forEach((e) => {
    const d = Number(e.dayOfWeek ?? 0), h = Number(e.hourOfDay ?? 0);
    if (d >= 0 && d < 7 && h >= 0 && h < 24) {
      grid[d][h] += Number(e.utilizationPercent ?? 0);
      cellCounts[d][h] += 1;
    }
  });
  for (let d = 0; d < 7; d++) for (let h = 0; h < 24; h++) if (cellCounts[d][h] > 0) grid[d][h] /= cellCounts[d][h];
  const getColor = (u) => u < 20 ? "bg-red-600" : u < 40 ? "bg-orange-500" : u < 60 ? "bg-yellow-400" : u < 80 ? "bg-lime-400" : "bg-green-600";
  return (
    <div className="overflow-x-auto">
      <div className="inline-block bg-gray-50 p-2 rounded-lg">
        <div className="flex mb-1">{Array(24).fill(null).map((_, h) => (<div key={`h-${h}`} style={{ width: 18 }} className="text-center text-[10px] text-gray-600">{h % 6 === 0 ? `${h}h` : ""}</div>))}</div>
        {grid.map((row, d) => (
          <div key={`r-${d}`} className="flex mb-0.5">
            {[
              <div key={`d-${d}`} style={{ width: 40 }} className="text-[11px] font-bold text-gray-700 flex items-center">{days[d]}</div>,
              ...row.map((u, h) => (
                <div key={`c-${d}-${h}`} style={{ width: 18, height: 18 }} className={`${getColor(u)} border border-gray-300 rounded-sm cursor-pointer hover:ring-2`} title={`${days[d]} ${h}h: ${u.toFixed(0)}%`} />
              ))
            ]}
          </div>
        ))}
      </div>
      <HeatmapLegend />
    </div>
  );
}

function HorizontalBarChart({ items = [], labelKey = "label", valueKey = "value", maxItems = 10 }) {
  if (!items || items.length === 0) {
    return <p className="text-sm text-gray-500">No bar chart data available.</p>;
  }

  const top = items.slice(0, maxItems);
  const scores = top.map((it) => 100 - Number(it[valueKey] || 0));
  const maxScore = Math.max(...scores, 1);

  return (
    <div className="space-y-3">
      {top.map((it, idx) => {
        const value = Number(it[valueKey] || 0);
        const score = 100 - value;
        const widthPct = Math.round((score / maxScore) * 100);
        const color = value < 20 ? "bg-red-500" : value < 30 ? "bg-amber-400" : "bg-green-400";
        return (
          <div key={`bar-${it[labelKey] ?? it.facilityId ?? idx}`}>
            <div className="flex justify-between items-center">
              <div className="text-sm text-gray-700 truncate" style={{ maxWidth: 240 }}>{it[labelKey]}</div>
              <div className="text-sm font-semibold text-gray-800">{formatPercent(value)}</div>
            </div>
            <div className="mt-1 h-3 bg-gray-100 rounded overflow-hidden">
              <div className={`${color} h-3 rounded`} style={{ width: `${widthPct}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

/* ========== ADVANCED ANALYTICS COMPONENTS ========== */

function PeakHoursChart({ heatmapData = [] }) {
  if (!heatmapData.length) return <p className="text-sm text-slate-500">No data</p>;
  
  const hourUtilization = Array(24).fill(0).map((_, h) => {
    const entries = heatmapData.filter((d) => Number(d.hourOfDay) === h);
    return entries.length ? entries.reduce((s, e) => s + Number(e.utilizationPercent || 0), 0) / entries.length : 0;
  });
  
  const maxUtil = Math.max(...hourUtilization, 1);
  const peakHour = hourUtilization.findIndex((u) => u === Math.max(...hourUtilization));
  const avgUtil = hourUtilization.reduce((a, b) => a + b, 0) / 24;
  
  return (
    <div className="space-y-3">
      <div className="flex justify-between text-xs font-medium text-slate-600 mb-2">
        <span>Peak: {peakHour}:00 ({hourUtilization[peakHour].toFixed(1)}%)</span>
        <span>Average: {avgUtil.toFixed(1)}%</span>
      </div>
      <div className="flex gap-1 h-24">
        {hourUtilization.map((u, h) => {
          const color = u < 30 ? "bg-red-500" : u < 60 ? "bg-yellow-500" : u < 80 ? "bg-blue-500" : "bg-green-600";
          return (
            <div key={`ph-${h}`} className="flex-1 flex flex-col justify-end gap-1" title={`${h}h: ${u.toFixed(0)}%`}>
              <div className={`${color} rounded-t transition hover:opacity-75`} style={{ height: `${(u / maxUtil) * 100}%`, minHeight: 2 }} />
              <span className="text-[10px] text-center text-slate-600">{h % 6 === 0 ? h : ""}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function UtilizationDistribution({ heatmapData = [] }) {
  if (!heatmapData.length) return <p className="text-sm text-slate-500">No data</p>;
  
  const ranges = {
    critical: { min: 0, max: 20, count: 0, label: "Critical", color: "bg-red-600" },
    low: { min: 20, max: 40, count: 0, label: "Low", color: "bg-orange-500" },
    moderate: { min: 40, max: 60, count: 0, label: "Moderate", color: "bg-yellow-500" },
    high: { min: 60, max: 80, count: 0, label: "High", color: "bg-blue-500" },
    optimal: { min: 80, max: 100, count: 0, label: "Optimal", color: "bg-green-600" },
  };
  
  heatmapData.forEach((d) => {
    const u = Number(d.utilizationPercent || 0);
    Object.keys(ranges).forEach((k) => {
      if (u >= ranges[k].min && u < ranges[k].max) ranges[k].count++;
    });
    if (u === 100) ranges.optimal.count++;
  });
  
  const total = heatmapData.length;
  
  return (
    <div className="flex gap-3">
      {Object.entries(ranges).map(([key, r]) => {
        const pct = ((r.count / total) * 100).toFixed(1);
        return (
          <div key={key} className="flex-1 text-center">
            <div className={`${r.color} h-20 rounded flex items-center justify-center text-white font-bold text-sm mb-2`}>
              {pct}%
            </div>
            <p className="text-xs font-medium text-slate-700">{r.label}</p>
            <p className="text-[10px] text-slate-500">{r.count} slots</p>
          </div>
        );
      })}
    </div>
  );
}

function RiskAssessment({ underutilizedFacilities = [], heatmapData = [] }) {
  const critical = underutilizedFacilities.filter((f) => Number(f.utilizationPercentage || 0) < 20).length;
  const warning = underutilizedFacilities.filter((f) => Number(f.utilizationPercentage || 0) >= 20 && Number(f.utilizationPercentage || 0) < 30).length;
  const overcrowded = heatmapData.filter((d) => Number(d.utilizationPercent || 0) > 90).length;
  
  return (
    <div className="grid grid-cols-3 gap-3">
      <div className="rounded-lg border border-red-200 bg-red-50 p-4">
        <p className="text-xs font-semibold text-red-700">Critical</p>
        <p className="text-2xl font-bold text-red-900 mt-2">{critical}</p>
        <p className="text-xs text-red-600 mt-1">Under 20%</p>
      </div>
      <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4">
        <p className="text-xs font-semibold text-yellow-700">Warning</p>
        <p className="text-2xl font-bold text-yellow-900 mt-2">{warning}</p>
        <p className="text-xs text-yellow-600 mt-1">20-30%</p>
      </div>
      <div className="rounded-lg border border-green-200 bg-green-50 p-4">
        <p className="text-xs font-semibold text-green-700">Optimal</p>
        <p className="text-2xl font-bold text-green-900 mt-2">{heatmapData.length - overcrowded}</p>
        <p className="text-xs text-green-600 mt-1">Below 90%</p>
      </div>
    </div>
  );
}

function DailyPatterns({ heatmapData = [] }) {
  if (!heatmapData.length) return <p className="text-sm text-slate-500">No data</p>;
  
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const dayUtilization = Array(7).fill(0).map((_, d) => {
    const entries = heatmapData.filter((h) => Number(h.dayOfWeek) === d);
    return entries.length ? entries.reduce((s, e) => s + Number(e.utilizationPercent || 0), 0) / entries.length : 0;
  });
  
  const maxDay = Math.max(...dayUtilization, 1);
  const peakDay = dayUtilization.indexOf(Math.max(...dayUtilization));
  
  return (
    <div className="space-y-3">
      <p className="text-xs font-medium text-slate-600">Busiest: {days[peakDay]} ({dayUtilization[peakDay].toFixed(1)}%)</p>
      <div className="flex gap-2 h-24">
        {dayUtilization.map((u, d) => {
          const color = u < 40 ? "bg-red-500" : u < 60 ? "bg-yellow-500" : u < 80 ? "bg-blue-500" : "bg-green-600";
          return (
            <div key={`dp-${d}`} className="flex-1 flex flex-col justify-end gap-1">
              <div className={`${color} rounded-t transition hover:opacity-75`} style={{ height: `${(u / maxDay) * 100}%`, minHeight: 2 }} title={`${days[d]}: ${u.toFixed(0)}%`} />
              <span className="text-[10px] text-center text-slate-600 font-medium">{days[d]}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function InsightCards({ heatmapData = [], underutilizedFacilities = [] }) {
  const avgUtilization = heatmapData.length ? (heatmapData.reduce((s, e) => s + Number(e.utilizationPercent || 0), 0) / heatmapData.length).toFixed(1) : 0;
  const peakSlots = heatmapData.filter((d) => Number(d.utilizationPercent) > 85).length;
  const lowSlots = heatmapData.filter((d) => Number(d.utilizationPercent) < 25).length;
  const variability = Math.max(...heatmapData.map((d) => Number(d.utilizationPercent || 0))) - Math.min(...heatmapData.map((d) => Number(d.utilizationPercent || 0)));
  
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      <div className="rounded-lg bg-blue-50 border border-blue-200 p-4">
        <p className="text-xs text-blue-700 font-semibold">Average Usage</p>
        <p className="text-2xl font-bold text-blue-900 mt-2">{avgUtilization}%</p>
      </div>
      <div className="rounded-lg bg-green-50 border border-green-200 p-4">
        <p className="text-xs text-green-700 font-semibold">Peak Slots</p>
        <p className="text-2xl font-bold text-green-900 mt-2">{peakSlots}</p>
        <p className="text-xs text-green-600 mt-1">&gt;85%</p>
      </div>
      <div className="rounded-lg bg-orange-50 border border-orange-200 p-4">
        <p className="text-xs text-orange-700 font-semibold">Low Slots</p>
        <p className="text-2xl font-bold text-orange-900 mt-2">{lowSlots}</p>
        <p className="text-xs text-orange-600 mt-1">&lt;25%</p>
      </div>
      <div className="rounded-lg bg-slate-50 border border-slate-200 p-4">
        <p className="text-xs text-slate-700 font-semibold">Variability</p>
        <p className="text-2xl font-bold text-slate-900 mt-2">{variability.toFixed(0)}%</p>
        <p className="text-xs text-slate-600 mt-1">range</p>
      </div>
    </div>
  );
}

export function UtilizationDashboard() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");
  const [fromDate, setFromDate] = useState(pastDateString(30));
  const [toDate, setToDate] = useState(todayDateString());
  const [scope, setScope] = useState("campus"); // 'campus' or 'facility'
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
  const [rawAnalytics, setRawAnalytics] = useState(null);
  const [debugEnabled, setDebugEnabled] = useState(true);
  const [showRawJson, setShowRawJson] = useState(false);

  useEffect(() => {
    const loadFacilities = async () => {
      try {
        const payload = await fetchFacilities({ page: 0, size: 100 });
        const content = Array.isArray(payload?.content) ? payload.content : [];
        setFacilityOptions(content);
        if (content.length > 0) {
          setFacilityId((current) => current || content[0].id);
        }
        if (debugEnabled) console.log("Loaded facilities", content.length, content.slice(0, 3));
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

  const analyticsStats = useMemo(() => {
    if (!rawAnalytics) return { facilitiesAnalyzed: 0, heatmapPoints: 0, underutilizedCount: underutilizedFacilities.length };
    const normalized = normalizeAnalytics(rawAnalytics);
    const heat = Array.isArray(normalized.heatmap) ? normalized.heatmap : [];
    const facilitySet = new Set(heat.map((h) => h.facilityId).filter(Boolean));
    return { facilitiesAnalyzed: facilitySet.size, heatmapPoints: heat.length, underutilizedCount: underutilizedFacilities.length };
  }, [rawAnalytics, underutilizedFacilities]);

  const [topN, setTopN] = useState(10);
  const [showAllUnderutilized, setShowAllUnderutilized] = useState(false);
  const [dayHourHeatmap, setDayHourHeatmap] = useState([]);

  const topUnderutilized = useMemo(() => {
    const src = Array.isArray(underutilizedFacilities) ? [...underutilizedFacilities] : [];
    src.sort((a, b) => Number(a.utilizationPercentage || 0) - Number(b.utilizationPercentage || 0));
    return src.slice(0, topN).map((f) => ({ label: f.facilityName, value: Number(f.utilizationPercentage || 0), facilityId: f.facilityId }));
  }, [underutilizedFacilities, topN]);

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
    if (!fromDate || !toDate || fromDate > toDate) {
      setError("Select a valid date range.");
      return;
    }

    try {
      setLoading(true);
      setError(null);

      if (scope === "facility") {
        if (!facilityId) {
          setError("Select a facility for facility-scoped analytics.");
          setLoading(false);
          return;
        }

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
      } else {
        // Campus-wide analytics
        const analyticsRaw = await fetchCampusUtilization(fromDate, toDate);
        if (debugEnabled) {
          console.log("fetchCampusUtilization raw response:", analyticsRaw);
          console.log("response keys:", analyticsRaw ? Object.keys(analyticsRaw) : analyticsRaw);
        }

        setRawAnalytics(analyticsRaw);
        const analytics = normalizeAnalytics(analyticsRaw);
        const heatmap = Array.isArray(analytics?.heatmap) ? analytics.heatmap : [];
        setDayHourHeatmap(heatmap);

        const avgUtil = heatmap.length
          ? heatmap.reduce((s, e) => s + Number(e.utilizationPercent || 0), 0) / heatmap.length
          : 0;

        setMetrics({
          totalAvailableHours: 0,
          totalBookedHours: 0,
          utilizationPercentage: Number(avgUtil.toFixed(2)),
        });

        // Map backend underutilized format to frontend-friendly shape (normalized above)
        const underutilized = Array.isArray(analytics?.underutilizedFacilities)
          ? analytics.underutilizedFacilities.map((f) => ({
              facilityId: f.facilityId,
              facilityName: f.facilityName,
              utilizationPercentage: Number(f.utilizationPercent || 0),
              consecutiveUnderutilizedDays: f.consecutiveUnderutilizedDays || 0,
              status: f.recommendation || "Observed",
              persistentForSevenDays: (f.consecutiveUnderutilizedDays || 0) >= 7,
            }))
          : [];

        if (debugEnabled) {
          console.log("Normalized heatmap sample:", heatmap?.slice?.(0, 4));
          console.log("Underutilized (normalized):", underutilized);
        }

        setUnderutilizedFacilities(underutilized);

        // Build a 7-point weekly average series from heatmap (Mon-Sun)
        const labels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
        const points = labels.map((lab, idx) => {
          const entries = heatmap.filter((h) => Number(h.dayOfWeek) === idx);
          const avg = entries.length ? entries.reduce((s, e) => s + Number(e.utilizationPercent || 0), 0) / entries.length : 0;
          return { label: lab, value: Number(avg.toFixed(2)) };
        });

        setChartPoints(points);
      }
    } catch (err) {
      setError("Failed to load utilization analytics.");
    } finally {
      setLoading(false);
    }
  };

  if (!isAdmin) {
    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-6">
        <h2 className="text-lg font-bold text-red-900">Access Restricted</h2>
        <p className="mt-2 text-sm text-red-700">The Utilization Dashboard is only available to administrators.</p>
      </section>
    );
  }

  return (
    <section className="space-y-5">
      <header className="rounded-lg border border-slate-200 bg-white p-5">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">
            {scope === "campus" ? "Campus Utilization Analysis" : "Facility Utilization Analysis"}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {scope === "campus"
              ? "View campus-wide patterns, identify bottlenecks, and optimize facility allocation"
              : "Analyze facility-level trends and performance metrics"}
          </p>
        </div>
      </header>

      <form onSubmit={fetchAnalytics} className="rounded-lg border border-slate-200 bg-white p-5">
        <h3 className="text-sm font-bold text-slate-900 mb-4">Select Scope and Date Range</h3>
        <div className="grid gap-4 sm:grid-cols-4">
          <div className="flex items-center gap-4">
            <label className="inline-flex items-center">
              <input
                type="radio"
                name="scope"
                value="campus"
                checked={scope === "campus"}
                onChange={() => setScope("campus")}
                className="mr-2"
              />
              <span className="text-sm font-medium text-slate-700">Campus-wide</span>
            </label>
            <label className="inline-flex items-center">
              <input
                type="radio"
                name="scope"
                value="facility"
                checked={scope === "facility"}
                onChange={() => setScope("facility")}
                className="mr-2"
              />
              <span className="text-sm font-medium text-slate-700">Facility</span>
            </label>
          </div>

          {scope === "facility" && (
            <label className="text-sm">
              <span className="mb-2 block font-medium text-slate-700">Facility</span>
              <select
                value={facilityId}
                onChange={(event) => setFacilityId(event.target.value)}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-600"
              >
                {facilityOptions.map((facility) => (
                  <option key={facility.id ?? facility.facilityId ?? facility.name} value={facility.id}>
                    {facility.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="text-sm">
            <span className="mb-2 block font-medium text-slate-700">From Date</span>
            <input
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-600"
            />
          </label>

          <label className="text-sm">
            <span className="mb-2 block font-medium text-slate-700">To Date</span>
            <input
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-600"
            />
          </label>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? "Analyzing..." : "Analyze"}
            </button>
          </div>
        </div>
      </form>

          {debugEnabled && rawAnalytics && (
            <section className="rounded-lg border border-slate-200 bg-white p-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-700">Debug: Raw Analytics</h3>
                <label className="flex items-center text-xs text-slate-600">
                  <input type="checkbox" checked={showRawJson} onChange={() => setShowRawJson((v) => !v)} className="mr-2" />
                  Show JSON
                </label>
              </div>
              {showRawJson && (
                <pre className="mt-2 max-h-64 overflow-auto text-xs text-slate-700">{JSON.stringify(rawAnalytics, null, 2)}</pre>
              )}
            </section>
          )}

          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4">
              <h4 className="font-semibold text-red-800">Analysis Failed</h4>
              <p className="text-sm text-red-700 mt-1">{error}</p>
            </div>
          )}

          <section className="grid gap-4 sm:grid-cols-3">
        {scope === "campus" ? (
          <>
            <article className="rounded-lg border border-blue-200 bg-blue-50 p-4">
              <p className="text-xs font-semibold text-blue-700">Campus Utilization</p>
              <p className="mt-2 text-2xl font-bold text-blue-900">{formatPercent(summaryCards.utilizationPercentage)}</p>
              <p className="text-xs text-blue-600 mt-1">Average across all facilities</p>
            </article>
            <article className="rounded-lg border border-green-200 bg-green-50 p-4">
              <p className="text-xs font-semibold text-green-700">Facilities Analyzed</p>
              <p className="mt-2 text-2xl font-bold text-green-900">{analyticsStats.facilitiesAnalyzed}</p>
              <p className="text-xs text-green-600 mt-1">With heatmap data</p>
            </article>
            <article className="rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="text-xs font-semibold text-red-700">Underutilized</p>
              <p className="mt-2 text-2xl font-bold text-red-900">{analyticsStats.underutilizedCount}</p>
              <p className="text-xs text-red-600 mt-1">Below 30% threshold</p>
            </article>
          </>
        ) : (
          <>
            <article className="rounded-lg border border-slate-200 bg-white p-4">
              <p className="text-xs font-semibold text-slate-700">Utilization</p>
              <p className="mt-2 text-2xl font-bold text-slate-900">{formatPercent(summaryCards.utilizationPercentage)}</p>
              <p className="text-xs text-slate-500 mt-1">{selectedFacilityName}</p>
            </article>
            <article className="rounded-lg border border-slate-200 bg-white p-4">
              <p className="text-xs font-semibold text-slate-700">Available Hours</p>
              <p className="mt-2 text-2xl font-bold text-slate-900">{summaryCards.totalAvailableHours.toFixed(2)}</p>
              <p className="text-xs text-slate-500 mt-1">Within selected range</p>
            </article>
            <article className="rounded-lg border border-slate-200 bg-white p-4">
              <p className="text-xs font-semibold text-slate-700">Booked Hours</p>
              <p className="mt-2 text-2xl font-bold text-slate-900">{summaryCards.totalBookedHours.toFixed(2)}</p>
              <p className="text-xs text-slate-500 mt-1">From booking data</p>
            </article>
          </>
        )}
      </section>

      {scope === "campus" && dayHourHeatmap.length > 0 && (
        <>
          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h3 className="text-sm font-bold text-slate-900 mb-4">Summary Metrics</h3>
            <InsightCards heatmapData={dayHourHeatmap} underutilizedFacilities={underutilizedFacilities} />
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h3 className="text-sm font-bold text-slate-900 mb-4">Utilization Heatmap (Day × Hour)</h3>
            <p className="text-xs text-slate-600 mb-3">Color intensity shows utilization across all campus facilities.</p>
            <DayHourHeatmap heatmapData={dayHourHeatmap} />
          </section>

          <div className="grid gap-5 lg:grid-cols-2">
            <section className="rounded-lg border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-bold text-slate-900 mb-4">Peak Hours Analysis</h3>
              <PeakHoursChart heatmapData={dayHourHeatmap} />
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-bold text-slate-900 mb-4">Daily Patterns</h3>
              <DailyPatterns heatmapData={dayHourHeatmap} />
            </section>
          </div>

          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h3 className="text-sm font-bold text-slate-900 mb-4">Utilization Distribution</h3>
            <p className="text-xs text-slate-600 mb-4">Breakdown of time slots by utilization range across campus</p>
            <UtilizationDistribution heatmapData={dayHourHeatmap} />
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h3 className="text-sm font-bold text-slate-900 mb-4">Risk Assessment</h3>
            <RiskAssessment underutilizedFacilities={underutilizedFacilities} heatmapData={dayHourHeatmap} />
          </section>
        </>
      )}

      {scope === "facility" && chartPoints.length > 0 && (
        <>
          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h3 className="text-sm font-bold text-slate-900 mb-4">7-Day Trend Analysis</h3>
            <LineChart points={chartPoints} />
          </section>

          <div className="grid gap-5 lg:grid-cols-2">
            <section className="rounded-lg border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-bold text-slate-900 mb-4">Daily Breakdown</h3>
              <div className="space-y-3">
                {chartPoints.map((point, idx) => (
                  <div key={`day-${idx}`}>
                    <div className="flex justify-between mb-1">
                      <span className="text-sm text-slate-700 font-medium">{point.label}</span>
                      <span className="text-sm font-bold text-slate-900">{formatPercent(point.value)}</span>
                    </div>
                    <div className="w-full h-2 bg-slate-100 rounded overflow-hidden">
                      <div
                        className={`h-2 rounded ${
                          point.value < 30 ? "bg-red-600" : point.value < 60 ? "bg-yellow-500" : point.value < 80 ? "bg-blue-500" : "bg-green-600"
                        }`}
                        style={{ width: `${point.value}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-bold text-slate-900 mb-4">Statistics</h3>
              <div className="space-y-3">
                <div className="border-l-4 border-l-blue-600 bg-blue-50 p-3 rounded">
                  <p className="text-xs text-blue-700 font-semibold">Average Utilization</p>
                  <p className="text-xl font-bold text-blue-900 mt-1">
                    {(chartPoints.reduce((s, p) => s + Number(p.value), 0) / chartPoints.length).toFixed(1)}%
                  </p>
                </div>
                <div className="border-l-4 border-l-green-600 bg-green-50 p-3 rounded">
                  <p className="text-xs text-green-700 font-semibold">Peak Day</p>
                  <p className="text-xl font-bold text-green-900 mt-1">
                    {chartPoints.reduce((max, p) => (Number(p.value) > Number(max.value) ? p : max)).label}
                  </p>
                  <p className="text-xs text-green-600 mt-1">
                    {formatPercent(Math.max(...chartPoints.map((p) => Number(p.value))))}
                  </p>
                </div>
                <div className="border-l-4 border-l-orange-600 bg-orange-50 p-3 rounded">
                  <p className="text-xs text-orange-700 font-semibold">Lowest Day</p>
                  <p className="text-xl font-bold text-orange-900 mt-1">
                    {chartPoints.reduce((min, p) => (Number(p.value) < Number(min.value) ? p : min)).label}
                  </p>
                  <p className="text-xs text-orange-600 mt-1">
                    {formatPercent(Math.min(...chartPoints.map((p) => Number(p.value))))}
                  </p>
                </div>
              </div>
            </section>
          </div>
        </>
      )}

      <section className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500">Top Underutilized Facilities</h3>
          <div className="flex items-center gap-2">
            <label className="text-xs text-gray-600">Top</label>
            <select value={topN} onChange={(e) => setTopN(Number(e.target.value))} className="text-sm rounded border px-2 py-1">
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <HorizontalBarChart items={topUnderutilized} labelKey="label" valueKey="value" maxItems={topN} />
          </div>
          <div>
            <div className="text-xs text-gray-600 mb-2">Quick Stats</div>
            <div className="grid grid-cols-3 gap-3">
              <div className="rounded-lg border border-gray-200 bg-white p-3 text-center">
                <div className="text-xs text-gray-500">Facilities</div>
                <div className="mt-1 font-semibold text-gray-900">{analyticsStats.facilitiesAnalyzed}</div>
              </div>
              <div className="rounded-lg border border-gray-200 bg-white p-3 text-center">
                <div className="text-xs text-gray-500">Heatmap Points</div>
                <div className="mt-1 font-semibold text-gray-900">{analyticsStats.heatmapPoints}</div>
              </div>
              <div className="rounded-lg border border-gray-200 bg-white p-3 text-center">
                <div className="text-xs text-gray-500">Underutilized</div>
                <div className="mt-1 font-semibold text-gray-900">{analyticsStats.underutilizedCount}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold text-slate-900">Underutilized Facilities ({underutilizedFacilities.length})</h3>
          {!showAllUnderutilized && underutilizedFacilities.length > 10 && (
            <button onClick={() => setShowAllUnderutilized(true)} className="text-xs text-blue-600 hover:text-blue-700 font-semibold">View all →</button>
          )}
          {showAllUnderutilized && underutilizedFacilities.length > 10 && (
            <button onClick={() => setShowAllUnderutilized(false)} className="text-xs text-blue-600 hover:text-blue-700 font-semibold">Show less ↑</button>
          )}
        </div>
        {underutilizedFacilities.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">No underutilized facilities found for this range. Great usage patterns!</p>
        ) : (
          <div className="space-y-2 max-h-96 overflow-y-auto pr-2">
            {(showAllUnderutilized ? underutilizedFacilities : underutilizedFacilities.slice(0, 10)).map((facility, idx) => {
              const critical = Number(facility.utilizationPercentage) < 20;
              return (
                <div key={`under-${facility.facilityId ?? facility.facilityName ?? idx}`} className={`border-l-4 rounded p-3 transition ${critical ? "border-l-red-600 bg-red-50" : "border-l-yellow-500 bg-yellow-50"}`}>
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <h4 className="font-semibold text-slate-900 text-sm">{facility.facilityName}</h4>
                      <p className="text-xs text-slate-600 mt-1">{facility.consecutiveUnderutilizedDays || 0} low days • {facility.status}</p>
                    </div>
                    <div className={`px-2 py-1 rounded text-xs font-bold ${critical ? "bg-red-200 text-red-800" : "bg-yellow-200 text-yellow-800"}`}>{formatPercent(facility.utilizationPercentage)}</div>
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
