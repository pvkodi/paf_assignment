import React from "react";
import { Link } from "react-router-dom";

function statusClasses(status) {
  switch (status) {
    case "ACTIVE":
      return "bg-green-50 text-green-700 border border-green-200";
    case "MAINTENANCE":
      return "bg-amber-50 text-amber-700 border border-amber-200";
    case "OUT_OF_SERVICE":
      return "bg-red-50 text-red-700 border border-red-200";
    default:
      return "bg-slate-50 text-slate-600 border border-slate-200";
  }
}

function Sparkline({ data = [] }) {
  if (!data || data.length === 0) {
    return (
      <div className="h-8 w-24 flex items-center justify-end text-[10px] font-bold text-slate-300 uppercase tracking-wider" aria-hidden>
        No data
      </div>
    );
  }

  const max = Math.max(...data, 1);
  const points = data
    .map((v, i) => {
      const x = (i / (data.length - 1 || 1)) * 100;
      const y = 100 - (v / max) * 100;
      return `${x},${y}`;
    })
    .join(" ");

  const avg = Math.round(data.reduce((a, b) => a + Number(b || 0), 0) / data.length);

  return (
    <svg
      viewBox="0 0 100 30"
      className="h-8 w-24 opacity-80"
      role="img"
      aria-label={`Utilization sparkline, average ${avg} percent`}
      focusable="false"
    >
      <title>Utilization sparkline</title>
      <polyline
        fill="none"
        stroke="#49BBBB"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={points}
      />
    </svg>
  );
}

export default function FacilityCard({ facility = {}, utilizationHistory = [] }) {
  const avgUtil = utilizationHistory && utilizationHistory.length
    ? Math.round(
        utilizationHistory.reduce((a, b) => a + Number(b || 0), 0) / utilizationHistory.length,
      )
    : 0;

  const name = facility.name || "Unnamed facility";
  const typeLabel = facility.type ? facility.type.replaceAll("_", " ") : "Unknown type";

  return (
    <article
      className="group rounded-2xl border border-slate-100 bg-white p-5 hover:shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:border-slate-200 transition-all flex flex-col h-full"
      aria-labelledby={`facility-${facility.id}-title`}
      role="group"
    >
      <div className="flex items-start justify-between mb-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider border mb-3 bg-slate-50 text-slate-600 border-slate-200">
            {typeLabel}
          </div>
          <h3 id={`facility-${facility.id}-title`} className="text-lg font-bold text-slate-900 group-hover:text-[#49BBBB] transition-colors line-clamp-1">
            {name}
          </h3>
          <p className="mt-1 flex items-center gap-2 text-sm font-medium text-slate-500">
            <span className="sr-only">Location:</span>
            {facility.building || "-"}
          </p>
        </div>

        <span
          className={`shrink-0 inline-flex items-center px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${statusClasses(
            facility.status,
          )}`}
          aria-label={`Status: ${facility.status ?? 'UNKNOWN'}`}
        >
          {facility.status || "UNKNOWN"}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-6 bg-[#f8f9fa] rounded-xl p-3 flex-grow">
        <div className="flex flex-col justify-center">
          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Capacity</p>
          <p className="text-sm font-bold text-slate-800">{facility.capacity ?? "-"}</p>
        </div>
        <div className="flex flex-col flex-grow items-end justify-center">
          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1 text-right">Avg Util: {avgUtil}%</p>
          <div className="mt-1 w-full flex justify-end">
            <Sparkline data={utilizationHistory} />
          </div>
        </div>
      </div>

      <div className="mt-auto">
        <Link
          to={`/facilities/${facility.id}`}
          className="w-full inline-flex justify-center items-center px-5 py-3 bg-white hover:bg-slate-50 border border-slate-200 text-slate-800 font-bold rounded-xl transition-all shadow-[0_2px_8px_rgb(0,0,0,0.02)] active:scale-95"
          aria-label={`View details for ${name}`}
        >
          View Details
        </Link>
      </div>
    </article>
  );
}
