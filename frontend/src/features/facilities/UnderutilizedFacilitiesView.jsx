import React, { useEffect, useState } from "react";
import { fetchUnderutilizedFacilities } from "./api";

function toDisplayLabel(status) {
  if (!status) return "Unknown";
  // Map any internal codes to user-friendly labels if needed
  switch (status) {
    case "UNDERUTILIZED":
      return "Underutilized";
    case "OK":
      return "OK";
    default:
      return status;
  }
}

export default function UnderutilizedFacilitiesView() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadRows = async () => {
      try {
        setLoading(true);
        setError(null);
        const payload = await fetchUnderutilizedFacilities();
        // Accept either an array or a paged response
        const data = Array.isArray(payload) ? payload : payload?.content ?? [];
        setRows(data);
      } catch (e) {
        setError("Failed to load underutilized facilities");
      } finally {
        setLoading(false);
      }
    };

    loadRows();
  }, []);

  return (
    <section className="space-y-5">
      <header className="rounded-lg border border-gray-200 bg-white p-6">
        <h2 className="text-2xl font-semibold text-gray-900">Underutilized Facilities</h2>
        <p className="mt-2 text-sm text-gray-600">Facilities under 30% utilization in the last 30 days are listed here.</p>
      </header>

      {error && (
        <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}

      <section className="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Facility</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Utilization %</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Persisted Days</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-sm text-gray-500">
                    Loading underutilized facilities...
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-sm text-gray-500">No underutilized facilities found.</td>
                </tr>
              ) : (
                rows.map((row, idx) => {
                  const critical = Number(row.utilizationPercentage) < 20;
                  const key = row.facilityId ?? row.facilityName ?? idx;
                  return (
                    <tr key={key} className={critical ? "bg-red-50/60" : ""}>
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">{row.facilityName}</td>
                      <td className={`px-4 py-3 text-sm ${critical ? "font-semibold text-red-700" : "text-gray-700"}`}>
                        {Number(row.utilizationPercentage).toFixed(2)}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-700">{toDisplayLabel(row.status)}</td>
                      <td className="px-4 py-3 text-sm text-gray-700">{row.consecutiveUnderutilizedDays ?? 0}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}
