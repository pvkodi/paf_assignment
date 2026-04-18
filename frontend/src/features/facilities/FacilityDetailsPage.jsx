import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { fetchFacilityById } from "./api";

function toDisplayLabel(value) {
  return value ? value.replaceAll("_", " ") : "-";
}

function statusTone(status) {
  if (status === "ACTIVE") return "bg-green-50 text-green-700 border-green-200";
  if (status === "MAINTENANCE") return "bg-amber-50 text-amber-700 border-amber-200";
  return "bg-red-50 text-red-700 border-red-200";
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
    return (
      <div className="flex justify-center items-center h-48 bg-white rounded-3xl shadow-[0_4px_24px_rgb(0,0,0,0.02)] border border-slate-100">
        <p className="text-slate-400 font-bold text-sm animate-pulse">Loading facility details...</p>
      </div>
    );
  }

  if (error || !facility) {
    return (
      <section className="bg-white rounded-3xl shadow-[0_4px_24px_rgb(0,0,0,0.02)] border border-slate-100 p-8 text-center max-w-md mx-auto mt-10">
        <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-red-100">
           <svg className="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
        </div>
        <p className="text-lg font-bold text-slate-800 mb-6">
          {error || "Facility not found"}
        </p>
        <Link to="/facilities" className="inline-block px-6 py-3 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all">
          Back to Facilities
        </Link>
      </section>
    );
  }

  return (
    <section className="max-w-4xl mx-auto space-y-6">
      <div className="mb-6">
         <Link to="/facilities" className="inline-flex items-center gap-2 text-sm font-bold text-slate-400 hover:text-slate-800 transition-colors">
            <span className="text-lg leading-none">‹</span> Back to Facilities
         </Link>
      </div>

      <header className="flex flex-col md:flex-row items-start md:items-center justify-between rounded-[2rem] border border-slate-100 bg-white p-8 shadow-[0_4px_24px_rgb(0,0,0,0.02)] gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider border mb-3 bg-slate-50 text-slate-600 border-slate-200">
            {toDisplayLabel(facility.type)}
          </div>
          <h2 className="text-3xl font-bold tracking-tight text-slate-900">{facility.name}</h2>
          <p className="mt-2 text-sm font-medium text-slate-500">Facility ID: {facility.id} • Building: {facility.building || "N/A"}</p>
        </div>
        <div className={`shrink-0 px-4 py-2 rounded-xl text-xs font-bold uppercase tracking-wider border shadow-sm ${statusTone(facility.status)}`}>
          {toDisplayLabel(facility.status)}
        </div>
      </header>

      <div className="bg-white rounded-[2rem] shadow-[0_4px_24px_rgb(0,0,0,0.02)] border border-slate-100 p-8">
        <section className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <article className="rounded-3xl border-none bg-[#f8f9fa] p-6 lg:p-8">
            <h3 className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-6 flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              Core Info
            </h3>
            <dl className="space-y-4">
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Type</dt>
                <dd className="text-sm font-bold text-slate-900">{toDisplayLabel(facility.type)}</dd>
              </div>
              <div className="h-px bg-slate-200 w-full" />
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Capacity</dt>
                <dd className="text-sm font-bold text-slate-900">{facility.capacity}</dd>
              </div>
              <div className="h-px bg-slate-200 w-full" />
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Building</dt>
                <dd className="text-sm font-bold text-slate-900">{facility.building || "-"}</dd>
              </div>
              <div className="h-px bg-slate-200 w-full" />
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Floor</dt>
                <dd className="text-sm font-bold text-slate-900">{facility.floor || "-"}</dd>
              </div>
            </dl>
          </article>

          <article className="rounded-3xl border-none bg-[#f8f9fa] p-6 lg:p-8">
            <h3 className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-6 flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              Availability & Location
            </h3>
            <dl className="space-y-4">
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Start Time</dt>
                <dd className="text-sm font-bold text-slate-900">{facility.availabilityStartTime || "-"}</dd>
              </div>
              <div className="h-px bg-slate-200 w-full" />
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">End Time</dt>
                <dd className="text-sm font-bold text-slate-900">{facility.availabilityEndTime || "-"}</dd>
              </div>
              <div className="h-px bg-slate-200 w-full" />
              <div className="flex flex-col">
                <dt className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Location Details</dt>
                <dd className="text-sm font-bold text-slate-900 leading-relaxed">{facility.locationDescription || facility.location || "-"}</dd>
              </div>
            </dl>
          </article>
        </section>
      </div>
    </section>
  );
}
