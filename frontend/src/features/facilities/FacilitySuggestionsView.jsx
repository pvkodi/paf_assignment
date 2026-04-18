import React, { useState } from "react";
import { fetchFacilitySuggestions } from "./api";

const facilityTypes = [
  "LECTURE_HALL",
  "LAB",
  "MEETING_ROOM",
  "AUDITORIUM",
  "EQUIPMENT",
  "SPORTS",
];

function toDisplayLabel(value) {
  return value ? value.replaceAll("_", " ") : "-";
}

function toIsoDateTime(value) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return date.toISOString().slice(0, 19);
}

export default function FacilitySuggestionsView() {
  const [form, setForm] = useState({
    type: "LECTURE_HALL",
    capacity: 1,
    start: "",
    end: "",
    preferredBuilding: "",
  });
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);

    try {
      setLoading(true);

      const payload = {
        type: form.type,
        capacity: Number(form.capacity),
        start: toIsoDateTime(form.start),
        end: toIsoDateTime(form.end),
        preferredBuilding: form.preferredBuilding || null,
      };

      const result = await fetchFacilitySuggestions(payload);
      setRows(Array.isArray(result) ? result : []);
    } catch (requestError) {
      setError(requestError?.response?.data?.message || "Failed to fetch facility suggestions");
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="space-y-6 max-w-6xl mx-auto">
      <header className="rounded-[2rem] border border-slate-100 bg-white p-6 md:p-8 shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900">Facility Suggestions</h2>
        <p className="mt-2 text-sm font-medium text-slate-500">
          Suggest alternatives by type, capacity, and time range. Booking conflict checks are excluded by module boundary.
        </p>
      </header>

      <form onSubmit={handleSubmit} className="rounded-[2rem] border border-slate-100 bg-white p-6 md:p-8 shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
        <div className="grid grid-cols-1 gap-5 md:grid-cols-5 md:items-end bg-[#f8f9fa] p-6 rounded-3xl">
          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Type</span>
            <select
              value={form.type}
              onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))}
              className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
            >
              {facilityTypes.map((type) => (
                <option key={type} value={type}>
                  {toDisplayLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Capacity</span>
            <input
              required
              min="1"
              type="number"
              value={form.capacity}
              onChange={(event) => setForm((prev) => ({ ...prev, capacity: event.target.value }))}
              className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
            />
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Start</span>
            <input
              required
              type="datetime-local"
              value={form.start}
              onChange={(event) => setForm((prev) => ({ ...prev, start: event.target.value }))}
              className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
            />
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">End</span>
            <input
              required
              type="datetime-local"
              value={form.end}
              onChange={(event) => setForm((prev) => ({ ...prev, end: event.target.value }))}
              className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
            />
          </label>

          <div className="flex flex-col">
            <label className="text-sm text-slate-700 mb-2">
              <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Preferred Building</span>
              <input
                type="text"
                value={form.preferredBuilding}
                onChange={(event) => setForm((prev) => ({ ...prev, preferredBuilding: event.target.value }))}
                className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none mt-2"
              />
            </label>
          </div>
        </div>

        <div className="mt-8 flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="px-8 py-3.5 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-60"
          >
            {loading ? "Searching..." : "Suggest Facilities"}
          </button>
        </div>
      </form>

      {error && (
         <div className="rounded-2xl border border-red-100 bg-red-50 p-4">
           <p className="text-sm font-bold text-red-600">{error}</p>
         </div>
      )}

      {rows.length > 0 && (
         <section className="overflow-hidden rounded-[2rem] border border-slate-100 bg-white shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
           <div className="overflow-x-auto">
             <table className="min-w-full text-left border-collapse">
               <thead>
                 <tr className="border-b border-slate-100 bg-[#f8f9fa]">
                   <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Name</th>
                   <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Type</th>
                   <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400 text-center">Capacity</th>
                   <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Building</th>
                   <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Status</th>
                   <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">Availability</th>
                 </tr>
               </thead>
               <tbody className="divide-y divide-slate-100">
                 {rows.map((row) => (
                    <tr key={row.facilityId} className="hover:bg-slate-50/50 transition-colors">
                      <td className="px-6 py-4 text-sm font-bold text-slate-900">{row.name}</td>
                      <td className="px-6 py-4 text-sm font-semibold text-slate-600">{toDisplayLabel(row.type)}</td>
                      <td className="px-6 py-4 text-sm font-bold text-slate-900 text-center">{row.capacity}</td>
                      <td className="px-6 py-4 text-sm font-medium text-slate-600">{row.building || "-"}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${
                          row.status === "ACTIVE" ? "bg-green-50 text-green-700" :
                          row.status === "MAINTENANCE" ? "bg-amber-50 text-amber-700" : "bg-red-50 text-red-700"
                        }`}>
                          {toDisplayLabel(row.status)}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${
                          row.operational ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-600"
                        }`}>
                           {row.operational ? "Operational" : "Unavailable"}
                        </span>
                      </td>
                    </tr>
                 ))}
               </tbody>
             </table>
           </div>
         </section>
      )}
    </section>
  );
}
