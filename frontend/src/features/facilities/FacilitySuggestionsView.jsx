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
    <section className="space-y-5">
      <header className="rounded-lg border border-gray-200 bg-white p-6">
        <h2 className="text-2xl font-semibold text-gray-900">Facility Suggestions</h2>
        <p className="mt-2 text-sm text-gray-600">
          Suggest alternatives by type, capacity, and time range. Booking conflict checks are excluded by module boundary.
        </p>
      </header>

      <form onSubmit={handleSubmit} className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Type</span>
            <select
              value={form.type}
              onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            >
              {facilityTypes.map((type) => (
                <option key={type} value={type}>
                  {toDisplayLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Capacity</span>
            <input
              required
              min="1"
              type="number"
              value={form.capacity}
              onChange={(event) => setForm((prev) => ({ ...prev, capacity: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            />
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Start</span>
            <input
              required
              type="datetime-local"
              value={form.start}
              onChange={(event) => setForm((prev) => ({ ...prev, start: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            />
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">End</span>
            <input
              required
              type="datetime-local"
              value={form.end}
              onChange={(event) => setForm((prev) => ({ ...prev, end: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            />
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Preferred Building</span>
            <input
              type="text"
              value={form.preferredBuilding}
              onChange={(event) => setForm((prev) => ({ ...prev, preferredBuilding: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            />
          </label>
        </div>

        <div className="mt-4 flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
          >
            {loading ? "Searching..." : "Suggest Facilities"}
          </button>
        </div>
      </form>

      {error && <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

      <section className="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Name</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Type</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Capacity</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Building</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Availability</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-gray-500">
                    No suggestions yet.
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.facilityId}>
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">{row.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{toDisplayLabel(row.type)}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{row.capacity}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{row.building || "-"}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{toDisplayLabel(row.status)}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{row.operational ? "Operational" : "Unavailable"}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}
