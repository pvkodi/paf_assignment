import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { fetchFacilityById } from "./api";

function toDisplayLabel(value) {
  return value ? value.replaceAll("_", " ") : "-";
}

function statusTone(status) {
  if (status === "ACTIVE") return "bg-emerald-100 text-emerald-700";
  if (status === "MAINTENANCE") return "bg-amber-100 text-amber-700";
  return "bg-rose-100 text-rose-700";
}

export default function FacilityDetailsPage() {
  const { id } = useParams();
  const [facility, setFacility] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadDetails = async () => {
      try {
        setLoading(true);
        setError(null);
        const payload = await fetchFacilityById(id);
        setFacility(payload);
      } catch {
        setError("Failed to load facility details");
      } finally {
        setLoading(false);
      }
    };

    loadDetails();
  }, [id]);

  if (loading) {
    return <p className="text-sm text-gray-600">Loading facility details...</p>;
  }

  if (error || !facility) {
    return (
      <section className="space-y-3">
        <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error || "Facility not found"}
        </p>
        <Link to="/facilities" className="text-sm font-medium text-gray-700 underline">
          Back to facilities
        </Link>
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <header className="flex items-start justify-between rounded-lg border border-gray-200 bg-white p-6">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">{facility.name}</h2>
          <p className="mt-1 text-sm text-gray-600">Facility details and operational metadata.</p>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusTone(facility.status)}`}>
          {toDisplayLabel(facility.status)}
        </span>
      </header>

      <section className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <article className="rounded-lg border border-gray-200 bg-white p-5">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500">Core Info</h3>
          <dl className="mt-4 space-y-2 text-sm text-gray-700">
            <div className="flex justify-between gap-4">
              <dt>Type</dt>
              <dd className="font-medium">{toDisplayLabel(facility.type)}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt>Capacity</dt>
              <dd className="font-medium">{facility.capacity}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt>Building</dt>
              <dd className="font-medium">{facility.building || "-"}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt>Floor</dt>
              <dd className="font-medium">{facility.floor || "-"}</dd>
            </div>
          </dl>
        </article>

        <article className="rounded-lg border border-gray-200 bg-white p-5">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500">Availability Window</h3>
          <dl className="mt-4 space-y-2 text-sm text-gray-700">
            <div className="flex justify-between gap-4">
              <dt>Start</dt>
              <dd className="font-medium">{facility.availabilityStartTime || "-"}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt>End</dt>
              <dd className="font-medium">{facility.availabilityEndTime || "-"}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt>Location Description</dt>
              <dd className="font-medium">{facility.locationDescription || "-"}</dd>
            </div>
          </dl>
        </article>
      </section>

      <footer>
        <Link to="/facilities" className="text-sm font-medium text-gray-700 underline">
          Back to facilities
        </Link>
      </footer>
    </section>
  );
}
