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
    <section className="space-y-4">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <label className="text-sm text-gray-700">
          <div className="mb-1 text-xs font-medium">Capacity</div>
          <input
            name="capacity"
            type="number"
            min="1"
            value={criteria.capacity}
            onChange={handleChange}
            className="w-32 rounded-md border border-gray-300 px-2 py-1"
          />
        </label>

        <label className="text-sm text-gray-700">
          <div className="mb-1 text-xs font-medium">Start</div>
          <input
            name="startAt"
            type="datetime-local"
            value={criteria.startAt}
            onChange={handleChange}
            className="rounded-md border border-gray-300 px-2 py-1"
          />
        </label>

        <label className="text-sm text-gray-700">
          <div className="mb-1 text-xs font-medium">End</div>
          <input
            name="endAt"
            type="datetime-local"
            value={criteria.endAt}
            onChange={handleChange}
            className="rounded-md border border-gray-300 px-2 py-1"
          />
        </label>

        <div>
          <button
            type="submit"
            disabled={loading}
            className="rounded bg-indigo-600 px-4 py-2 text-xs font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {loading ? "Searching..." : "Get Recommendations"}
          </button>
        </div>
      </form>

      <div>
        {results.length === 0 ? (
          <p className="text-sm text-gray-500">No recommendations yet</p>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
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
