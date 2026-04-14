import React from "react";
import { Link } from "react-router-dom";

function statusClasses(status) {
  switch (status) {
    case "ACTIVE":
      return "bg-green-600 text-white";
    case "MAINTENANCE":
      return "bg-yellow-600 text-black";
    case "OUT_OF_SERVICE":
      return "bg-red-600 text-white";
    default:
      return "bg-gray-200 text-gray-900";
  }
}

function Sparkline({ data = [] }) {
  if (!data || data.length === 0) {
    return (
      <div className="h-8 w-24 text-xs text-gray-500" aria-hidden>
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
      className="h-8 w-24"
      role="img"
      aria-label={`Utilization sparkline, average ${avg} percent`}
      focusable="false"
    >
      <title>Utilization sparkline</title>
      <polyline
        fill="none"
        stroke="#4f46e5"
        strokeWidth="2"
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
      className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm"
      aria-labelledby={`facility-${facility.id}-title`}
      role="group"
    >
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 id={`facility-${facility.id}-title`} className="text-sm font-semibold text-gray-900">
            {name}
          </h3>
          <p className="mt-1 text-xs text-gray-500">
            <span className="sr-only">Location:</span>
            {facility.building || "-"} <span aria-hidden>•</span>{' '}
            <span className="sr-only">Type:</span>{typeLabel}
          </p>
        </div>

        <div>
          <span
            className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${statusClasses(
              facility.status,
            )}`}
            aria-label={`Status: ${facility.status ?? 'UNKNOWN'}`}
          >
            {facility.status || "UNKNOWN"}
          </span>
        </div>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <div className="text-xs text-gray-600">
          <div>
            <span className="sr-only">Capacity: </span>
            Capacity: <span className="font-medium">{facility.capacity ?? "-"}</span>
          </div>
          <div className="mt-1">
            <span className="sr-only">Average utilization: </span>
            Avg Util: <span className="font-medium">{avgUtil}%</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Sparkline data={utilizationHistory} />
        </div>
      </div>

      <div className="mt-4 flex justify-end">
        <Link
          to={`/facilities/${facility.id}`}
          className="text-xs inline-flex items-center gap-2 rounded px-3 py-1 text-indigo-600 hover:bg-indigo-50 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          aria-label={`View details for ${name}`}
        >
          <span>View</span>
          <span className="sr-only">{` ${name}`}</span>
        </Link>
      </div>
    </article>
  );
}
