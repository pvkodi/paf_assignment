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
    <section className="space-y-6 max-w-6xl mx-auto">
      <header className="rounded-[2rem] border border-slate-100 bg-white p-6 md:p-8 shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900">Underutilized Facilities</h2>
        <p className="mt-2 text-sm font-medium text-slate-500">
          Facilities under 30% utilization in the last 30 days are listed here.
        </p>
      </header>

      {error && (
         <div className="rounded-2xl border border-red-100 bg-red-50 p-4 shadow-sm">
           <p className="text-sm font-bold text-red-600">{error}</p>
         </div>
      )}

      <section className="overflow-hidden rounded-[2rem] border border-slate-100 bg-white shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-[#f8f9fa]">
                <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Facility</th>
                <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Utilization %</th>
                <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Status</th>
                <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400 text-right">Persisted Days</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center">
                    <p className="text-slate-400 font-bold text-sm animate-pulse">Loading underutilized facilities...</p>
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center text-sm font-bold text-slate-500">
                    No underutilized facilities found.
                  </td>
                </tr>
              ) : (
                rows.map((row, idx) => {
                  const critical = Number(row.utilizationPercentage) < 20;
                  const key = row.facilityId ?? row.facilityName ?? idx;
                  return (
                    <tr key={key} className={critical ? "bg-red-50/30 hover:bg-red-50/50 transition-colors" : "hover:bg-slate-50/50 transition-colors"}>
                      <td className="px-6 py-4 text-sm font-bold text-slate-900">{row.facilityName}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${
                          critical ? "bg-red-50 text-red-700" : "bg-amber-50 text-amber-700"
                        }`}>
                          {Number(row.utilizationPercentage).toFixed(2)}%
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className="inline-flex px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider bg-slate-100 text-slate-600">
                           {toDisplayLabel(row.status)}
                        </span>
                      </td>
                      <td className={`px-6 py-4 text-sm font-bold text-right ${critical ? "text-red-600" : "text-slate-600"}`}>
                        {row.consecutiveUnderutilizedDays ?? 0}
                      </td>
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
