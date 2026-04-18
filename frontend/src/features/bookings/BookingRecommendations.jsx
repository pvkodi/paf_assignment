import React, { useState } from "react";
import { getRecommendations } from "./api";
import FacilityCard from "../facilities/FacilityCard";

export default function BookingRecommendations() {
  const [criteria, setCriteria] = useState({ capacity: 10, startAt: "", endAt: "" });
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setCriteria((p) => ({ ...p, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await getRecommendations({
        capacity: Number(criteria.capacity),
        startAt: criteria.startAt,
        endAt: criteria.endAt,
      });
      setResults(Array.isArray(data) ? data : data.recommendations || []);
    } catch (err) {
      console.error(err);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="bg-white rounded-3xl shadow-[0_4px_24px_rgb(0,0,0,0.02)] p-6 md:p-8 border border-slate-100">
      <div className="mb-6">
        <h2 className="text-xl font-bold tracking-tight text-slate-900 mb-1">AI Recommendations</h2>
        <p className="text-sm font-medium text-slate-500">Find the optimal facility for your next booking</p>
      </div>
      
      <form onSubmit={handleSubmit} className="flex flex-col gap-5 sm:flex-row sm:items-end mb-8 bg-[#f8f9fa] p-5 rounded-2xl">
        <label className="text-sm text-slate-700 flex-1">
          <div className="mb-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">Capacity Need</div>
          <input
            name="capacity"
            type="number"
            min="1"
            value={criteria.capacity}
            onChange={handleChange}
            className="w-full rounded-xl border-none bg-white shadow-sm px-4 py-3 text-sm font-bold text-slate-800 focus:outline-none focus:ring-4 focus:ring-slate-200 transition-all"
          />
        </label>

        <label className="text-sm text-slate-700 flex-1">
          <div className="mb-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">Start Time</div>
          <input
            name="startAt"
            type="datetime-local"
            value={criteria.startAt}
            onChange={handleChange}
            className="w-full rounded-xl border-none bg-white shadow-sm px-4 py-3 text-sm font-bold text-slate-800 focus:outline-none focus:ring-4 focus:ring-slate-200 transition-all"
          />
        </label>

        <label className="text-sm text-slate-700 flex-1">
          <div className="mb-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">End Time</div>
          <input
            name="endAt"
            type="datetime-local"
            value={criteria.endAt}
            onChange={handleChange}
            className="w-full rounded-xl border-none bg-white shadow-sm px-4 py-3 text-sm font-bold text-slate-800 focus:outline-none focus:ring-4 focus:ring-slate-200 transition-all"
          />
        </label>

        <div className="flex-none">
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-slate-900 px-6 py-3 text-sm font-bold text-white shadow-[0_4px_14px_rgba(73,187,187,0.3)] hover:bg-slate-800 disabled:opacity-50 transition-all hover:-translate-y-0.5 active:translate-y-0"
          >
            {loading ? "Searching..." : "Get Recommendations"}
          </button>
        </div>
      </form>

      <div>
        {results.length === 0 ? (
          <div className="text-center py-10">
             <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-3">
               <svg className="w-6 h-6 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
             </div>
            <p className="text-sm font-bold text-slate-500">No recommendations found yet</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 md:grid-cols-3">
            {results.map((r) => (
              <FacilityCard
                key={r.facilityId ?? r.id}
                facility={{
                  id: r.facilityId ?? r.id,
                  name: r.facilityName ?? r.name,
                  building: r.building,
                  type: r.facilityType ?? r.type,
                  capacity: r.capacity,
                  status: r.status || "ACTIVE",
                }}
                utilizationHistory={r.utilizationHistory || r.history || []}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
