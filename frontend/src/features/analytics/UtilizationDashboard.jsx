import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import {
  fetchFacilities,
  fetchFacilityInsights,
  fetchFacilityHeatmap,
} from "../facilities/api";
import { fetchCampusUtilization, fetchRealTimeStatus } from "./api";

/* ═══════════════════════════════════════════════════════════════════════════
   PURE SVG CHART COMPONENTS — zero external dependencies
   ═══════════════════════════════════════════════════════════════════════════ */

// ── Radial Gauge ──────────────────────────────────────────────────────────────
function GaugeChart({ value = 0, max = 100, label, color = "#6366f1", size = 120 }) {
  const pct = Math.min(value / max, 1);
  const r = (size - 16) / 2;
  const circ = 2 * Math.PI * r;
  const half = size / 2;
  return (
    <div className="flex flex-col items-center gap-1">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={half} cy={half} r={r} fill="none" stroke="#f1f5f9" strokeWidth="10" />
        <circle cx={half} cy={half} r={r} fill="none" stroke={color} strokeWidth="10"
          strokeDasharray={circ} strokeDashoffset={circ * (1 - pct)}
          strokeLinecap="round" transform={`rotate(-90 ${half} ${half})`}
          style={{ transition: "stroke-dashoffset 1s ease" }} />
        <text x={half} y={half - 4} textAnchor="middle" className="fill-gray-900 text-lg font-black"
          dominantBaseline="central" style={{ fontSize: size * 0.22 }}>
          {typeof value === "number" ? value.toFixed(1) : value}
        </text>
        <text x={half} y={half + size * 0.14} textAnchor="middle" className="fill-gray-400"
          style={{ fontSize: size * 0.09, fontWeight: 700 }}>
          {label || "%"}
        </text>
      </svg>
    </div>
  );
}

// ── Vertical Bar Chart ────────────────────────────────────────────────────────
function BarChart({ data = [], width = 320, height = 160, barColor = "#6366f1", label }) {
  if (!data.length) return <p className="text-xs text-gray-400 italic p-4">No data</p>;
  const maxVal = Math.max(...data.map(d => d.value), 1);
  const barW = Math.max(6, Math.min(32, (width - 48) / data.length - 5));
  const chartH = height - 36;
  return (
    <div>
      {label && <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">{label}</p>}
      <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} style={{ filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.05))' }}>
        {data.map((d, i) => {
          const h = (d.value / maxVal) * chartH;
          const x = 28 + i * ((width - 48) / data.length);
          return (
            <g key={i}>
              <rect x={x} y={chartH - h} width={barW} height={h} rx={Math.max(2, barW / 6)}
                fill={d.color || barColor} opacity={0.9} style={{ transition: 'opacity 0.2s' }}>
                <title>{`${d.name}: ${d.value.toFixed(1)}%`}</title>
              </rect>
              <text x={x + barW / 2} y={height - 6} textAnchor="middle"
                style={{ fontSize: 11, fontWeight: 700 }} className="fill-gray-500">{d.name}</text>
            </g>
          );
        })}
        {/* Y-axis ticks */}
        {[0, 25, 50, 75, 100].map(v => {
          const y = chartH - (v / maxVal) * chartH;
          return y >= 0 && y <= chartH ? (
            <g key={v}>
              <line x1={20} y1={y} x2={width} y2={y} stroke="#f1f5f9" strokeWidth="1" />
              <text x={16} y={y + 4} textAnchor="end" style={{ fontSize: 9 }} className="fill-gray-400">{v}</text>
            </g>
          ) : null;
        })}
      </svg>
    </div>
  );
}

// ── Horizontal Bar Chart (Rankings) ───────────────────────────────────────────
function HBarChart({ data = [], maxItems = 5, height = 160, color = "#6366f1", label }) {
  const items = data.slice(0, maxItems);
  if (!items.length) return <p className="text-xs text-gray-400 italic p-4">No data</p>;
  const maxVal = Math.max(...items.map(d => d.value), 1);
  const barH = Math.min(20, (height - 10) / items.length - 6);
  return (
    <div>
      {label && <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">{label}</p>}
      <div className="space-y-2">
        {items.map((d, i) => (
          <div key={i} className="flex items-center gap-3 group">
            <span className="text-[11px] font-bold text-gray-600 min-w-[120px] truncate text-right group-hover:text-gray-900 transition" title={d.fullName || d.name}>{d.name}</span>
            <div className="flex-1 bg-gray-100 rounded-full h-5 overflow-hidden relative shadow-inner">
              <div className="h-full rounded-full transition-all duration-700"
                style={{ width: `${(d.value / maxVal) * 100}%`, background: d.color || color }} />
            </div>
            <span className="text-xs font-black text-gray-800 w-12 text-right">{d.value.toFixed(1)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Donut Chart ───────────────────────────────────────────────────────────────
function DonutChart({ segments = [], size = 140, label }) {
  const total = segments.reduce((s, seg) => s + seg.value, 0) || 1;
  const r = (size - 40) / 2;
  const circ = 2 * Math.PI * r;
  const half = size / 2;
  let cumOffset = 0;
  return (
    <div className="flex flex-col items-center">
      {label && <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">{label}</p>}
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.05))' }}>
        {segments.map((seg, i) => {
          const pct = seg.value / total;
          const dash = circ * pct;
          const offset = circ * cumOffset;
          cumOffset += pct;
          return (
            <circle key={i} cx={half} cy={half} r={r} fill="none"
              stroke={seg.color} strokeWidth="11"
              strokeDasharray={`${dash} ${circ - dash}`}
              strokeDashoffset={-offset}
              transform={`rotate(-90 ${half} ${half})`}
              style={{ transition: 'stroke-dashoffset 0.5s ease' }}>
              <title>{`${seg.name}: ${seg.value}`}</title>
            </circle>
          );
        })}
        <circle cx={half} cy={half} r={r - 12} fill="white" />
        <text x={half} y={half - 2} textAnchor="middle" dominantBaseline="central"
          style={{ fontSize: size * 0.12, fontWeight: 900, letterSpacing: '-0.02em' }} className="fill-gray-600">{total >= 1000 ? Math.round(total / 1000) + 'K' : Math.round(total)}</text>
      </svg>
      <div className="flex flex-wrap justify-center gap-x-4 gap-y-1.5 mt-4">
        {segments.map((seg, i) => (
          <span key={i} className="flex items-center gap-1.5 text-[11px] font-bold text-gray-600 whitespace-nowrap">
            <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: seg.color }} />
            <span>{seg.name}</span>
          </span>
        ))}
      </div>
    </div>
  );
}

// ── Area Sparkline ────────────────────────────────────────────────────────────
function AreaChart({ data = [], width = 300, height = 80, color = "#6366f1", label, showDots }) {
  if (data.length < 2) return <p className="text-xs text-gray-400 italic p-4">Insufficient data</p>;
  const maxVal = Math.max(...data.map(d => d.value), 1);
  const minVal = 0;
  const range = maxVal - minVal || 1;
  const pts = data.map((d, i) => ({
    x: 12 + (i / (data.length - 1)) * (width - 24),
    y: height - 14 - ((d.value - minVal) / range) * (height - 28),
  }));
  const line = pts.map((p, i) => `${i === 0 ? "M" : "L"}${p.x},${p.y}`).join(" ");
  const area = `${line} L${pts[pts.length - 1].x},${height - 14} L${pts[0].x},${height - 14} Z`;
  return (
    <div>
      {label && <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">{label}</p>}
      <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} style={{ filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.05))' }}>
        <defs>
          <linearGradient id={`ag-${label}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.25" />
            <stop offset="100%" stopColor={color} stopOpacity="0.02" />
          </linearGradient>
        </defs>
        <path d={area} fill={`url(#ag-${label})`} />
        <path d={line} fill="none" stroke={color} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
        {showDots && pts.map((p, i) => (
          <circle key={i} cx={p.x} cy={p.y} r={3.5} fill={color} stroke="white" strokeWidth={2.5} style={{ filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))' }}>
            <title>{`${data[i].name}: ${data[i].value.toFixed(1)}%`}</title>
          </circle>
        ))}
      </svg>
    </div>
  );
}

// ── Mini Heatmap (improved) ──────────────────────────────────────────────────
function MiniHeatmap({ heatmapData = [], compact }) {
  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
  const daysShort = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const grid = Array(7).fill(0).map(() => Array(24).fill(0));
  const counts = Array(7).fill(0).map(() => Array(24).fill(0));
  heatmapData.forEach(d => {
    const day = Number(d.dayOfWeek ?? d.dayOfWeekIndex ?? 0);
    const hr = Number(d.hourOfDay ?? d.hour ?? 0);
    if (day >= 0 && day < 7 && hr >= 0 && hr < 24) {
      grid[day][hr] += Number(d.utilizationPercent ?? d.utilizationPercentage ?? 0);
      counts[day][hr]++;
    }
  });
  const sz = compact ? 16 : 24;
  const gap = compact ? 2 : 2.5;
  const colors = ["#f1f5f9", "#bfdbfe", "#60a5fa", "#3b82f6", "#1d4ed8", "#1e3a8a"];
  const getColor = v => v === 0 ? colors[0] : v < 20 ? colors[1] : v < 40 ? colors[2] : v < 60 ? colors[3] : v < 80 ? colors[4] : colors[5];
  
  return (
    <div className="w-full">
      <div className="w-full">
        <div className="flex mb-3 ml-14 gap-1">
          {Array(24).fill(0).map((_, h) => (
            <div key={h} style={{ width: sz, marginRight: gap }}
              className="text-center text-[10px] font-bold text-gray-400 leading-none">
              {h % 4 === 0 ? h : ""}
            </div>
          ))}
        </div>
        {grid.map((row, d) => (
          <div key={d} className="flex items-center gap-0.5 mb-1.5">
            <div className="text-gray-600 font-bold whitespace-nowrap" style={{ width: 12, fontSize: 11, letterSpacing: '-0.5px', minWidth: 14 }}>
              {daysShort[d]}
            </div>
            <div className="flex gap-1">
              {row.map((val, h) => {
                const avg = counts[d][h] > 0 ? val / counts[d][h] : 0;
                return (
                  <div 
                    key={h} 
                    style={{ 
                      width: sz, 
                      height: sz, 
                      background: getColor(avg), 
                      borderRadius: 3
                    }}
                    title={`${days[d]} ${h.toString().padStart(2, '0')}:00 — ${avg.toFixed(1)}% utilization`} 
                    className="cursor-help hover:ring-2 ring-indigo-500 hover:ring-offset-1 hover:shadow-lg transition-all duration-150 flex-shrink-0"
                  />
                );
              })}
            </div>
          </div>
        ))}
      </div>
      <div className="mt-5 flex justify-start items-center gap-4 text-[11px] text-gray-600 font-bold ml-14">
        <div className="flex items-center gap-2">
          <span>Low</span>
          {colors.slice(1).map((c, i) => <div key={i} className="rounded" style={{ width: 10, height: 10, background: c }} />)}
          <span>Peak</span>
        </div>
      </div>
    </div>
  );
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
function StatCard({ title, value, subtitle, icon, color = "#6366f1", trend }) {
  return (
    <div className="bg-white/90 backdrop-blur-md rounded-2xl border border-gray-100 p-4 shadow-sm hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 group relative overflow-hidden">
      <div className="absolute top-0 left-0 w-1 h-full bg-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity" />
      <div className="flex items-start justify-between relative z-10 gap-2">
        <div className="flex-1 min-w-0">
          <p className="text-[9px] font-black text-gray-400 uppercase tracking-widest truncate">{title}</p>
          <p className="text-xl font-black text-gray-900 mt-1.5 group-hover:text-indigo-600 transition break-words">{value}</p>
          {subtitle && <p className="text-[9px] text-gray-500 font-bold mt-0.5 truncate">{subtitle}</p>}
          {trend !== undefined && (
            <div className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[8px] font-black mt-2 whitespace-nowrap ${trend >= 0 ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-red-600"}`}>
              {trend >= 0 ? "↑" : "↓"} {Math.abs(trend).toFixed(1)}%
            </div>
          )}
        </div>
        <div className="w-9 h-9 rounded-lg bg-gray-50 flex items-center justify-center text-base flex-shrink-0 group-hover:bg-indigo-50 group-hover:scale-110 transition-all duration-300 shadow-inner">
          {icon || (
            <svg className="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          )}
        </div>
      </div>
      <div className="absolute -right-3 -bottom-3 w-12 h-12 bg-indigo-500/5 rounded-full blur-lg group-hover:scale-150 transition-transform duration-700" />
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   DATA DERIVATION — squeeze every possible insight from existing API data
   ═══════════════════════════════════════════════════════════════════════════ */

function deriveInsights(analytics, facList) {
  if (!analytics) return {};
  const hm = analytics.heatmap || [];
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const periods = { Morning: [6, 12], Afternoon: [12, 17], Evening: [17, 22], Night: [22, 6] };

  // By day of week
  const byDay = days.map((name, d) => {
    const cells = hm.filter(h => Number(h.dayOfWeek) === d);
    const avg = cells.length ? cells.reduce((s, c) => s + Number(c.utilizationPercent), 0) / cells.length : 0;
    return { name, value: avg };
  });

  // By hour of day
  const byHour = Array(24).fill(0).map((_, h) => {
    const cells = hm.filter(c => Number(c.hourOfDay) === h);
    const avg = cells.length ? cells.reduce((s, c) => s + Number(c.utilizationPercent), 0) / cells.length : 0;
    return { name: `${h}h`, value: avg };
  });

  // By period
  const byPeriod = Object.entries(periods).map(([name, [start, end]]) => {
    const cells = hm.filter(c => {
      const h = Number(c.hourOfDay);
      return start < end ? (h >= start && h < end) : (h >= start || h < end);
    });
    const avg = cells.length ? cells.reduce((s, c) => s + Number(c.utilizationPercent), 0) / cells.length : 0;
    const colors = { Morning: "#f59e0b", Afternoon: "#6366f1", Evening: "#8b5cf6", Night: "#1e293b" };
    return { name, value: avg, color: colors[name] };
  });

  // Per-facility averages
  const facMap = {};
  hm.forEach(c => {
    const id = c.facilityId;
    if (!facMap[id]) facMap[id] = { name: c.facilityName, total: 0, count: 0 };
    facMap[id].total += Number(c.utilizationPercent);
    facMap[id].count++;
  });
  const perFacility = Object.values(facMap).map(f => ({
    name: f.name?.length > 20 ? f.name.slice(0, 18) + "…" : f.name,
    fullName: f.name,
    value: f.count ? f.total / f.count : 0,
  })).sort((a, b) => b.value - a.value);

  // Utilization distribution (histogram)
  const brackets = ["0-20%", "20-40%", "40-60%", "60-80%", "80-100%"];
  const bracketColors = ["#ef4444", "#f97316", "#eab308", "#22c55e", "#6366f1"];
  const distribution = brackets.map((name, i) => {
    const lo = i * 20, hi = (i + 1) * 20;
    const count = perFacility.filter(f => f.value >= lo && f.value < (i === 4 ? 101 : hi)).length;
    return { name, value: count, color: bracketColors[i] };
  });

  // Overall
  const overallAvg = hm.length ? hm.reduce((s, c) => s + Number(c.utilizationPercent), 0) / hm.length : 0;
  const peakHour = byHour.reduce((best, h) => h.value > best.value ? h : best, { value: 0, name: "-" });
  const quietHour = byHour.filter(h => h.value > 0).reduce((best, h) => h.value < best.value ? h : best, { value: 999, name: "-" });
  const busiestDay = byDay.reduce((best, d) => d.value > best.value ? d : best, { value: 0, name: "-" });
  const quietDay = byDay.reduce((best, d) => d.value < best.value ? d : best, { value: 999, name: "-" });

  // Weekday vs weekend
  const weekdayAvg = byDay.slice(0, 5).reduce((s, d) => s + d.value, 0) / 5;
  const weekendAvg = byDay.slice(5).reduce((s, d) => s + d.value, 0) / 2;

  // Status breakdown from facList
  const statusCounts = { ACTIVE: 0, MAINTENANCE: 0, OUT_OF_SERVICE: 0 };
  (facList || []).forEach(f => { if (statusCounts[f.status] !== undefined) statusCounts[f.status]++; });
  const statusSegments = [
    { name: "Active", value: statusCounts.ACTIVE, color: "#22c55e" },
    { name: "Maintenance", value: statusCounts.MAINTENANCE, color: "#f59e0b" },
    { name: "OOS", value: statusCounts.OUT_OF_SERVICE, color: "#ef4444" },
  ].filter(s => s.value > 0);

  // Type breakdown from facList
  const typeMap = {};
  (facList || []).forEach(f => { typeMap[f.type] = (typeMap[f.type] || 0) + 1; });
  const typeColors = ["#6366f1", "#8b5cf6", "#a78bfa", "#c4b5fd", "#e0e7ff", "#818cf8"];
  const typeSegments = Object.entries(typeMap).map(([name, value], i) => ({
    name: name.replace(/_/g, " "), value, color: typeColors[i % typeColors.length],
  }));

  // Capacity utilization correlation (top facilities by capacity)
  const capacityData = (facList || []).slice(0, 10).map(f => {
    const facAvg = perFacility.find(pf => pf.name?.startsWith(f.name?.slice(0, 14)));
    return { name: f.name?.length > 10 ? f.name.slice(0, 8) + ".." : f.name, value: facAvg?.value || 0 };
  });

  return {
    byDay, byHour, byPeriod, perFacility, distribution,
    overallAvg, peakHour, quietHour, busiestDay, quietDay,
    weekdayAvg, weekendAvg, statusSegments, typeSegments, capacityData,
  };
}

/* ═══════════════════════════════════════════════════════════════════════════
   MAIN DASHBOARD COMPONENT
   ═══════════════════════════════════════════════════════════════════════════ */

export function UtilizationDashboard() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [dateRange, setDateRange] = useState({
    from: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
    to: new Date().toISOString().slice(0, 10),
  });
  const [scope, setScope] = useState("campus");
  const [selectedFacility, setSelectedFacility] = useState("");
  const [facList, setFacList] = useState([]);
  const [realTime, setRealTime] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [facilityInsights, setFacilityInsights] = useState(null);
  const [facilityHeatmap, setFacilityHeatmap] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isAdmin) return;
    (async () => {
      const facs = await fetchFacilities({ page: 0, size: 100 });
      setFacList(facs.content || []);
      if (facs.content?.length > 0) setSelectedFacility(facs.content[0].id);
      try { setRealTime(await fetchRealTimeStatus()); } catch {}
    })();
  }, [isAdmin]);

  useEffect(() => {
    if (!isAdmin) return;
    if (scope === "campus") loadCampus();
    else if (selectedFacility) loadFacility();
  }, [scope, selectedFacility, dateRange]);

  const loadCampus = async () => {
    setLoading(true);
    try { setAnalytics(await fetchCampusUtilization(dateRange.from, dateRange.to)); } catch {}
    finally { setLoading(false); }
  };

  const loadFacility = async () => {
    setLoading(true);
    setFacilityInsights(null);
    setFacilityHeatmap(null);
    try {
      const [ins, hm] = await Promise.all([
        fetchFacilityInsights(selectedFacility).catch(() => null),
        fetchFacilityHeatmap(selectedFacility, dateRange.from, dateRange.to).catch(() => null),
      ]);
      setFacilityInsights(ins);
      setFacilityHeatmap(hm);
    } catch {}
    finally { setLoading(false); }
  };

  const insights = useMemo(() => deriveInsights(analytics, facList), [analytics, facList]);

  const selectedFacData = useMemo(() => facList.find(f => f.id === selectedFacility), [facList, selectedFacility]);

  if (!isAdmin) {
    return (
      <div className="p-10 text-center bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200">
        <h2 className="text-xl font-bold text-gray-400">Administration Access Required</h2>
      </div>
    );
  }

  /* ─── CAMPUS VIEW ───────────────────────────────────────────────────────── */
  const renderCampusView = () => (
    <div className="space-y-5">
      {/* Row 1: KPI Stat Cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <StatCard title="Overall Avg" value={`${insights.overallAvg?.toFixed(1) || 0}%`} />
        <StatCard title="Peak Hour" value={insights.peakHour?.name || "-"} subtitle={`${insights.peakHour?.value?.toFixed(1)}%`} />
        <StatCard title="Busiest Day" value={insights.busiestDay?.name || "-"} subtitle={`${insights.busiestDay?.value?.toFixed(1)}%`} />
        <StatCard title="Total Capacity" value={(facList?.reduce((s,f) => s + (f.capacity || 0), 0) || 0).toLocaleString()} subtitle="Active Slots" />
        <StatCard title="Fleet Health" value={`${((insights.statusSegments?.find(s=>s.name==="Active")?.value || 0) / (facList?.length || 1) * 100).toFixed(0)}%`} subtitle="Online" />
        <StatCard title="Period" value={`${Math.round((new Date(dateRange.to) - new Date(dateRange.from)) / (1000 * 60 * 60 * 24))}d`} subtitle="Analyzed" />
      </div>

      {/* Row 2: Heavy Visuals */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
        <div className="md:col-span-2 bg-white/90 backdrop-blur-xl rounded-3xl border border-gray-100 p-6 shadow-sm flex flex-col items-center justify-center min-h-[280px]">
          <GaugeChart value={insights.overallAvg || 0} label="Efficiency Score" color="#6366f1" size={160} />
          <p className="text-[9px] font-bold text-gray-400 uppercase mt-3 text-center leading-tight">Weighted capacity &amp; throughput</p>
        </div>
        <div className="md:col-span-7 bg-white/90 backdrop-blur-xl rounded-3xl border border-gray-100 p-6 shadow-sm">
          <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4 flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-indigo-500 animate-pulse" />
            Campus Utilization Heatmap (24H × 7 Days)
          </p>
          <MiniHeatmap heatmapData={analytics?.heatmap || []} />
        </div>
        <div className="md:col-span-3 bg-white/90 backdrop-blur-xl rounded-3xl border border-gray-100 p-8 shadow-sm flex flex-col items-center justify-center">
          <DonutChart segments={insights.typeSegments || []} size={170} label="Asset Types" />
        </div>
      </div>

      {/* Row 3: Deep Analytics */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        <div className="lg:col-span-4 bg-white rounded-3xl border border-gray-100 p-7 shadow-sm">
          <AreaChart data={insights.byHour || []} width={380} height={200} label="Hourly Utilization (24H)" color="#6366f1" showDots />
        </div>
        <div className="lg:col-span-4 bg-white rounded-3xl border border-gray-100 p-7 shadow-sm">
          <BarChart data={insights.byDay || []} width={380} height={200} label="Weekly Pattern (7 Days)" barColor="#818cf8" />
        </div>
        <div className="lg:col-span-4 bg-white rounded-3xl border border-gray-100 p-7 shadow-sm flex flex-col items-center justify-center">
          <DonutChart segments={insights.byPeriod || []} size={160} label="Daily Shift Distribution" />
        </div>
      </div>

      {/* Row 4: Rankings & Alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        <div className="lg:col-span-4 bg-white/80 backdrop-blur-xl rounded-3xl border border-gray-100 p-6 shadow-sm">
           <HBarChart data={(insights.perFacility || []).slice(0, 8)} maxItems={8} height={280} color="#6366f1" label="Best Performers" />
        </div>
        <div className="lg:col-span-4 bg-white/80 backdrop-blur-xl rounded-3xl border border-gray-100 p-6 shadow-sm">
           <HBarChart data={(insights.perFacility || []).slice(-8).reverse()}
            maxItems={8} height={280} color="#6366f1" label="Needs Attention" />
        </div>
        <div className="lg:col-span-4 flex flex-col gap-4">
          <div className="bg-gradient-to-br from-indigo-600 to-indigo-700 rounded-3xl p-6 text-white shadow-xl flex-1 flex flex-col justify-between">
            <div>
              <p className="text-[9px] font-black uppercase tracking-widest text-indigo-200 mb-2">Smart Insights</p>
              <p className="text-sm font-bold leading-snug">
                {analytics?.underutilizedFacilities?.length > 3 
                  ? "Load imbalance detected. Recommend shifting core lectures to underutilized zones." 
                  : "Campus optimization at peak efficiency. System stable."}
              </p>
            </div>
            <div className="mt-4 pt-4 border-t border-white/20">
              <div className="flex justify-between text-[9px] font-black text-indigo-200 mb-1.5">
                <span>MAX CONCURRENCY</span>
                <span>{(insights.overallAvg * 1.2).toFixed(1)}%</span>
              </div>
              <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full bg-white rounded-full shadow-lg" style={{ width: `${Math.min(insights.overallAvg, 100)}%` }} />
              </div>
            </div>
          </div>
          <div className="bg-gray-950 rounded-3xl p-6 text-white shadow-xl flex-1">
            <p className="text-[9px] font-black uppercase tracking-widest text-gray-400 mb-3">Utilization Gaps</p>
            <div className="space-y-3 mt-4">
               {insights.distribution?.slice(0,2).map((d,i) => (
                 <div key={i} className="flex justify-between items-center text-[11px]">
                    <span className="font-bold text-gray-400">{d.name}</span>
                    <span className="font-black text-white">{d.value} units</span>
                 </div>
               ))}
            </div>
            <p className="text-[9px] text-gray-500 italic mt-5 border-t border-gray-700 pt-3">* Utilities &lt;10% avg warrant operational audit</p>
          </div>
        </div>
      </div>
    </div>
  );

  /* ─── FACILITY VIEW ─────────────────────────────────────────────────────── */
  const renderFacilityView = () => {
    const ins = facilityInsights || {};
    const u30 = ins.avg_utilization_30day ?? 0;
    const u7 = ins.avg_utilization_7day ?? 0;
    const trendPct = ins.trend_percent_change ?? (u7 - u30);

    // Derive hourly/daily from facility-specific heatmap
    const cells = facilityHeatmap?.cells || [];
    const fDays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    const fByDay = fDays.map((name, d) => {
      const c = cells.filter(h => Number(h.dayOfWeek ?? h.dayOfWeekIndex) === d);
      return { name, value: c.length ? c.reduce((s, x) => s + Number(x.utilizationPercent ?? x.utilizationPercentage ?? 0), 0) / c.length : 0 };
    });
    const fByHour = Array(24).fill(0).map((_, h) => {
      const c = cells.filter(x => Number(x.hourOfDay ?? x.hour) === h);
      return { name: `${h}h`, value: c.length ? c.reduce((s, x) => s + Number(x.utilizationPercent ?? x.utilizationPercentage ?? 0), 0) / c.length : 0 };
    });
    const fOverall = cells.length ? cells.reduce((s, c) => s + Number(c.utilizationPercent ?? c.utilizationPercentage ?? 0), 0) / cells.length : u30;
    const fPeakHour = fByHour.reduce((b, h) => h.value > b.value ? h : b, { value: 0, name: "-" });
    const fQuietHour = fByHour.filter(h => h.value > 0).reduce((b, h) => h.value < b.value ? h : b, { value: 999, name: "-" });

    const periodData = [
      { name: "Morning", value: fByHour.slice(6, 12).reduce((s, h) => s + h.value, 0) / 6, color: "#f59e0b" },
      { name: "Afternoon", value: fByHour.slice(12, 17).reduce((s, h) => s + h.value, 0) / 5, color: "#6366f1" },
      { name: "Evening", value: fByHour.slice(17, 22).reduce((s, h) => s + h.value, 0) / 5, color: "#8b5cf6" },
    ];

    return (
      <div className="space-y-6">
        {/* Facility identity header */}
        <div className="bg-gradient-to-r from-indigo-600 to-violet-600 rounded-2xl p-6 text-white shadow-xl">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-indigo-200">Facility Focus Mode</p>
              <h3 className="text-3xl font-black mt-1">{ins.facility_name || selectedFacData?.name || "Loading..."}</h3>
              <p className="text-sm text-indigo-100 mt-1">
                {selectedFacData?.type?.replace(/_/g, " ")} · {selectedFacData?.building} · Cap: {selectedFacData?.capacity}
              </p>
            </div>
            <div className="flex items-center gap-2 bg-white/10 backdrop-blur px-4 py-2 rounded-xl border border-white/20">
              <span className={`w-3 h-3 rounded-full ${ins.current_status === "BUSY" ? "bg-red-400" : ins.current_status === "FULL" ? "bg-red-600" : "bg-emerald-400"}`} />
              <span className="text-sm font-black">{ins.current_status || "—"}</span>
            </div>
          </div>
        </div>

        {/* Row 1: KPI cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
          <StatCard title="30-Day Avg" value={`${u30}%`} />
          <StatCard title="7-Day Avg" value={`${u7}%`} trend={trendPct} />
          <StatCard title="Peak Hour" value={fPeakHour.name} subtitle={`${fPeakHour.value.toFixed(1)}%`} />
          <StatCard title="Quietest" value={fQuietHour.name} subtitle={`${fQuietHour.value.toFixed(1)}%`} />
          <StatCard title="Spots Free" value={ins.spots_available ?? "—"} />
          <StatCard title="Next Booking" value={ins.next_booking_time ? ins.next_booking_time.slice(0, 5) : "None"} />
        </div>

        {/* Row 2: Gauges */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm flex flex-col items-center">
            <GaugeChart value={fOverall} label="Current Rate" color="#6366f1" size={130} />
          </div>
          <div className="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm flex flex-col items-center">
            <GaugeChart value={u30} label="30-Day" color="#6366f1" size={130} />
          </div>
          <div className="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm flex flex-col items-center">
            <GaugeChart value={u7} label="7-Day" color="#6366f1" size={130} />
          </div>
          <div className="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm flex flex-col items-center">
            <DonutChart segments={periodData} size={130} label="Usage by Period" />
          </div>
        </div>

        {/* Row 3: Heatmap + hourly curve */}
        <div className="grid grid-cols-1 lg:grid-cols-7 gap-4">
          <div className="lg:col-span-5 bg-white rounded-2xl border border-gray-100 p-8 shadow-sm">
            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-5">This Facility Utilization Heatmap</p>
            <MiniHeatmap heatmapData={cells} />
          </div>
          <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-100 p-6 shadow-sm flex flex-col justify-center">
            <AreaChart data={fByHour} width={320} height={180} label="24-Hour Pattern" color="#8b5cf6" showDots />
          </div>
        </div>

        {/* Row 4: Day bars + Period bars */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-white rounded-2xl border border-gray-100 p-7 shadow-sm">
            <BarChart data={fByDay} width={360} height={200} label="Utilization by Day" barColor="#818cf8" />
          </div>
          <div className="bg-white rounded-2xl border border-gray-100 p-7 shadow-sm">
            <BarChart data={periodData} width={360} height={200} label="Utilization by Period" />
          </div>
        </div>

        {/* Row 5: Best booking slots */}
        <div className="bg-gray-900 rounded-2xl p-6 text-white shadow-xl">
            <p className="text-[10px] font-black uppercase tracking-widest text-indigo-300 mb-4">Optimal Booking Windows</p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {(ins.best_booking_slots || []).slice(0, 8).map((slot, i) => (
              <div key={i} className="bg-white/5 border border-white/10 rounded-xl p-3 backdrop-blur">
                <p className="text-sm font-black">{(slot.dayOfWeek || "").slice(0, 3)} · {slot.time || slot.startTime}</p>
                <p className="text-[10px] text-green-400 font-bold mt-0.5">{slot.confidenceScore ?? "—"}% optimal</p>
              </div>
            ))}
            {(!ins.best_booking_slots || ins.best_booking_slots.length === 0) && (
              <p className="text-xs text-gray-500 col-span-4 italic">No slot data yet — awaiting more booking history.</p>
            )}
          </div>
        </div>
      </div>
    );
  };

  /* ─── SHELL ─────────────────────────────────────────────────────────────── */
  return (
    <div className="max-w-[1400px] mx-auto space-y-5">
      {/* Live pulse header */}
      <section className="bg-gray-900 rounded-2xl p-6 text-white shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-indigo-600 opacity-15 blur-[100px] -mr-40 -mt-20 rounded-full" />
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="relative flex h-3 w-3">
                <span className="animate-ping absolute h-full w-full rounded-full bg-green-400 opacity-75" />
                <span className="relative rounded-full h-3 w-3 bg-green-500" />
              </span>
              <span className="text-[11px] font-black uppercase tracking-[0.2em] text-green-400">Live Feed</span>
            </div>
            <h2 className="text-4xl font-black tracking-tight text-white">Facility Analytics Dashboard</h2>
          </div>
          <div className="flex gap-8 bg-black/30 backdrop-blur-md rounded-2xl p-5 border border-white/10">
            <div className="text-center">
              <p className="text-4xl font-black text-indigo-400">{realTime?.vacantNow ?? "—"}</p>
              <p className="text-[10px] font-bold text-gray-400 uppercase mt-1 tracking-wide">Available</p>
            </div>
            <div className="w-px bg-white/10" />
            <div className="text-center">
              <p className="text-4xl font-black text-indigo-400">{realTime?.occupiedNow ?? "—"}</p>
              <p className="text-[10px] font-bold text-gray-400 uppercase mt-1 tracking-wide">Booked</p>
            </div>
            <div className="w-px bg-white/10" />
            <div className="text-center">
              <p className="text-4xl font-black text-indigo-400">{realTime?.maintenanceCount ?? "—"}</p>
              <p className="text-[10px] font-bold text-gray-400 uppercase mt-1 tracking-wide">Maintenance</p>
            </div>
          </div>
        </div>
      </section>

      {/* Control bar */}
      <section className="bg-white border border-gray-100 rounded-2xl py-4 px-6 flex flex-wrap items-center justify-between gap-5 shadow-sm">
        <div className="flex items-center gap-5">
          <div className="flex bg-gray-100 p-1.5 rounded-lg">
            <button onClick={() => setScope("campus")}
              className={`px-4 py-2 rounded-md text-xs font-black uppercase tracking-wider transition-all duration-200 ${scope === "campus" ? "bg-white shadow-md text-indigo-600" : "text-gray-500 hover:text-gray-700"}`}>
              Campus View
            </button>
            <button onClick={() => setScope("facility")}
              className={`px-4 py-2 rounded-md text-xs font-black uppercase tracking-wider transition-all duration-200 ${scope === "facility" ? "bg-white shadow-md text-indigo-600" : "text-gray-500 hover:text-gray-700"}`}>
              Facility Details
            </button>
          </div>
          {scope === "facility" && (
            <select value={selectedFacility} onChange={e => setSelectedFacility(e.target.value)}
              className="bg-gray-50 rounded-lg px-3 py-2 text-xs font-bold text-gray-700 border border-gray-200 outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
              {facList.map(f => <option key={f.id} value={f.id}>{f.name}</option>)}
            </select>
          )}
        </div>
        <div className="flex items-center gap-3">
          <input type="date" value={dateRange.from} onChange={e => setDateRange(p => ({ ...p, from: e.target.value }))}
            className="bg-gray-50 rounded-lg px-3 py-2 text-xs font-bold text-gray-700 border border-gray-200 outline-none focus:ring-2 focus:ring-indigo-500" />
          <span className="text-gray-300 font-black">→</span>
          <input type="date" value={dateRange.to} onChange={e => setDateRange(p => ({ ...p, to: e.target.value }))}
            className="bg-gray-50 rounded-lg px-3 py-2 text-xs font-bold text-gray-700 border border-gray-200 outline-none focus:ring-2 focus:ring-indigo-500" />
          <button onClick={scope === "campus" ? loadCampus : loadFacility} disabled={loading}
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg text-xs font-black hover:bg-indigo-700 transition-all duration-200 disabled:opacity-50 shadow-sm hover:shadow-md">
            {loading ? "Loading..." : "Analyze"}
          </button>
        </div>
      </section>

      {/* Content */}
      <div className="relative min-h-[400px]">
        {loading && (
          <div className="absolute inset-0 bg-white/60 backdrop-blur-sm z-50 flex items-center justify-center rounded-2xl">
            <div className="animate-spin w-8 h-8 rounded-full border-4 border-indigo-600 border-t-transparent" />
          </div>
        )}
        {scope === "campus" ? renderCampusView() : renderFacilityView()}
      </div>
    </div>
  );
}

export default UtilizationDashboard;
