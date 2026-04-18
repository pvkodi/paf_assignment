import React, { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../../contexts/AuthContext";
import { 
  fetchFacilityById, 
  fetchFacilityTimetable, 
  markFacilityOutOfService, 
  hardDeleteFacility, 
  updateFacility 
} from "./api";
import ConfirmationModal from "../../components/ConfirmationModal";

// ─── Constants ────────────────────────────────────────────────────────────────

const ALL_DAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const DAY_LABEL = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
  SUNDAY: "Sunday",
};

// ─── Utilities ────────────────────────────────────────────────────────────────

function toLabel(value) {
  return value ? value.replace(/_/g, " ") : "—";
}

function formatTime(t) {
  return t ? String(t).slice(0, 5) : "—";
}

function currentDayOfWeek() {
  const days = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
  return days[new Date().getDay()];
}

function currentTime() {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
}

function isTimeInWindow(startTime, endTime) {
  const now = currentTime();
  return now >= String(startTime).slice(0, 5) && now < String(endTime).slice(0, 5);
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

function StatusBadge({ status }) {
  const config = {
    ACTIVE: { dot: "bg-emerald-500", text: "text-emerald-700", bg: "bg-emerald-50 ring-emerald-200" },
    MAINTENANCE: { dot: "bg-amber-500", text: "text-amber-700", bg: "bg-amber-50 ring-amber-200" },
    OUT_OF_SERVICE: { dot: "bg-red-500", text: "text-red-700", bg: "bg-red-50 ring-red-200" },
  }[status] || { dot: "bg-gray-400", text: "text-[#475569]", bg: "bg-[#f8fafc] ring-gray-200" };

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ring-1 ring-inset ${config.bg} ${config.text}`}
    >
      <span className={`h-2 w-2 rounded-full ${config.dot}`} aria-hidden="true" />
      {toLabel(status)}
    </span>
  );
}

// ─── Detail Row ───────────────────────────────────────────────────────────────

function DetailRow({ label, value }) {
  return (
    <div className="flex justify-between gap-4 py-2.5 border-b border-[#f1f5f9] last:border-0">
      <dt className="text-sm text-[#64748b] shrink-0">{label}</dt>
      <dd className="text-sm font-medium text-[#0f172a] text-right">{value || "—"}</dd>
    </div>
  );
}

// ─── Weekly Schedule Grid ─────────────────────────────────────────────────────

function WeeklyScheduleGrid({ windows, status }) {
  const today = currentDayOfWeek();

  const windowsByDay = useMemo(() => {
    const map = {};
    ALL_DAYS.forEach((d) => { map[d] = []; });
    (windows ?? []).forEach((w) => {
      if (map[w.dayOfWeek]) map[w.dayOfWeek].push(w);
    });
    return map;
  }, [windows]);

  const isCurrentlyAvailable = useMemo(() => {
    if (status !== "ACTIVE") return false;
    return (windowsByDay[today] ?? []).some((w) =>
      isTimeInWindow(w.startTime, w.endTime)
    );
  }, [windowsByDay, today, status]);

  if (!windows || windows.length === 0) {
    return (
      <p className="text-sm text-[#94a3b8] italic">No availability windows configured.</p>
    );
  }

  return (
    <div className="space-y-3">
      {/* Live indicator */}
      <div className="flex items-center gap-2">
        <span
          className={`h-2 w-2 rounded-full ${isCurrentlyAvailable ? "bg-emerald-500 animate-pulse" : "bg-gray-300"}`}
          aria-hidden="true"
        />
        <span className={`text-xs font-medium ${isCurrentlyAvailable ? "text-emerald-700" : "text-[#64748b]"}`}>
          {isCurrentlyAvailable ? "Currently available" : "Currently unavailable"}
        </span>
      </div>

      {/* Day rows */}
      <div className="space-y-1.5">
        {ALL_DAYS.map((day) => {
          const dayWindows = windowsByDay[day];
          const isToday = day === today;
          return (
            <div
              key={day}
              className={`flex items-center gap-3 rounded-xl px-3 py-2 ${isToday ? "bg-indigo-50 ring-1 ring-inset ring-indigo-100" : "bg-[#f8fafc]"}`}
            >
              <span className={`w-24 shrink-0 text-xs font-medium ${isToday ? "text-indigo-700" : "text-[#64748b]"}`}>
                {DAY_LABEL[day]}
                {isToday && (
                  <span className="ml-1.5 rounded-sm bg-indigo-100 px-1 py-0.5 text-[9px] uppercase tracking-wider text-indigo-600 font-bold">
                    Today
                  </span>
                )}
              </span>

              {dayWindows.length === 0 ? (
                <span className="text-xs text-[#cbd5e1]">—</span>
              ) : (
                <div className="flex flex-wrap gap-1.5">
                  {dayWindows.map((w, i) => (
                    <span
                      key={i}
                      className="rounded-xl bg-white border border-[#e2e8f0] px-2 py-0.5 text-xs font-medium text-[#334155] shadow-sm"
                    >
                      {formatTime(w.startTime)}–{formatTime(w.endTime)}
                    </span>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Timetable Live Grid ──────────────────────────────────────────────────────

function TimetableLiveGrid({ facilityId }) {
  const [day, setDay] = useState(currentDayOfWeek());
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const fetchTimetable = async () => {
      setLoading(true);
      try {
        const result = await fetchFacilityTimetable(facilityId, day);
        if (!cancelled) setData(result);
      } catch (err) {
        if (!cancelled) setData(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchTimetable();
    return () => { cancelled = true; };
  }, [facilityId, day]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <label htmlFor="timetable-day-select" className="sr-only">Select Day</label>
        <select
          id="timetable-day-select"
          value={day}
          onChange={(e) => setDay(e.target.value)}
          className="rounded-xl border border-[#e2e8f0] px-2.5 py-1 text-xs font-medium text-[#334155] focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          {ALL_DAYS.map((d) => (
            <option key={d} value={d}>{DAY_LABEL[d]}{d === currentDayOfWeek() ? " (Today)" : ""}</option>
          ))}
        </select>
        
        {data && data.timetableLoaded && (
          <div className="flex gap-3 text-[10px] font-medium">
            <span className="text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded leading-none flex items-center">{data.totalAvailableCount} Free</span>
            <span className="text-red-600 bg-red-50 px-1.5 py-0.5 rounded leading-none flex items-center">{data.totalOccupiedCount} Busy</span>
          </div>
        )}
      </div>

      {loading ? (
        <div className="h-16 rounded-xl bg-[#f1f5f9] animate-pulse" />
      ) : !data || !data.timetableLoaded ? (
        <div className="rounded-xl border border-dashed border-[#e2e8f0] px-3 py-4 text-center">
          <p className="text-xs text-[#94a3b8] italic">No timetable data loaded for this facility.</p>
        </div>
      ) : (
        <div className="flex flex-wrap gap-1.5">
          {(() => {
            // Merge all slots to show a full timeline
            const allTimes = new Set([...(data.occupiedSlots || []), ...(data.availableSlots || [])]);
            if (allTimes.size === 0) {
              return <p className="text-xs text-[#94a3b8] italic w-full text-center py-2">No slots defined</p>;
            }
            
            return Array.from(allTimes)
              .sort()
              .map((time) => {
                const isOccupied = data.occupiedSlots?.includes(time);
                return (
                  <span
                    key={time}
                    className={`rounded-xl border px-2 py-1 text-xs font-medium shadow-sm transition-colors ${
                      isOccupied 
                        ? "border-red-200 bg-red-50 text-red-700 opacity-75" 
                        : "border-emerald-200 bg-emerald-50 text-emerald-700"
                    }`}
                    title={isOccupied ? "Occupied in Timetable" : "Available in Timetable"}
                  >
                    {time}
                  </span>
                );
              });
          })()}
        </div>
      )}
    </div>
  );
}

// ─── Resource Tags ─────────────────────────────────────────────────────────────

function ResourceTags({ subtypeAttributes }) {
  if (!subtypeAttributes) return null;
  const tags = [];
  if (subtypeAttributes.avEquipment || subtypeAttributes.avEnabled || subtypeAttributes.soundSystem)
    tags.push("AV Equipped");
  if (subtypeAttributes.wheelchairAccessible) tags.push("Wheelchair Accessible");
  if (subtypeAttributes.cateringAllowed) tags.push("Catering Allowed");
  if (subtypeAttributes.labType) tags.push(subtypeAttributes.labType);
  if (subtypeAttributes.softwareList) tags.push("Software Lab");
  if (subtypeAttributes.safetyEquipment) tags.push("Safety Equipment");
  if (subtypeAttributes.sportsType) tags.push(subtypeAttributes.sportsType);
  if (subtypeAttributes.equipmentAvailable) tags.push("Equipment Available");
  if (tags.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-2">
      {tags.map((tag) => (
        <span
          key={tag}
          className="rounded-xl bg-[#f1f5f9] px-2.5 py-1 text-xs font-medium text-[#334155]"
        >
          {tag}
        </span>
      ))}
    </div>
  );
}

// ─── Facility Details Page ────────────────────────────────────────────────────

export default function FacilityDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const isAdmin = hasRole(["ADMIN"]);

  const [facility, setFacility] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal states
  const [confirmModal, setConfirmModal] = useState({ 
    isOpen: false, 
    type: null, // 'DEACTIVATE' | 'DELETE'
    loading: false 
  });

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchFacilityById(id);
        if (!cancelled) setFacility(data);
      } catch {
        if (!cancelled) setError("Failed to load facility details.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [id]);

  const handleStatusToggle = async () => {
    if (!facility) return;
    const isOOS = facility.status === "OUT_OF_SERVICE";
    
    try {
      setConfirmModal(prev => ({ ...prev, loading: true }));
      if (isOOS) {
        const updated = await updateFacility(id, { ...facility, status: "ACTIVE" });
        setFacility(updated);
        toast.success("Facility reactivated successfully.");
      } else {
        await markFacilityOutOfService(id);
        setFacility(prev => ({ ...prev, status: "OUT_OF_SERVICE" }));
        toast.success("Facility deactivated.");
      }
    } catch {
      toast.error("Failed to update facility status.");
    } finally {
      setConfirmModal({ isOpen: false, type: null, loading: false });
    }
  };

  const handleDelete = async () => {
    try {
      setConfirmModal(prev => ({ ...prev, loading: true }));
      await hardDeleteFacility(id);
      toast.success("Facility permanently deleted.");
      navigate("/facilities");
    } catch {
      toast.error("Failed to delete facility. It may have related records.");
    } finally {
      setConfirmModal({ isOpen: false, type: null, loading: false });
    }
  };

  // ── Loading ──────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="h-8 w-56 rounded-xl bg-gray-200" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="h-48 rounded-2xl bg-gray-200" />
          <div className="h-48 rounded-2xl bg-gray-200" />
        </div>
      </div>
    );
  }

  // ── Error ────────────────────────────────────────────────────────────────

  if (error || !facility) {
    return (
      <div className="space-y-4">
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3">
          <p className="text-sm text-red-700">{error || "Facility not found."}</p>
        </div>
        <Link
          to="/facilities"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-indigo-600 hover:text-indigo-800 transition-colors"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 16 16" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10 12l-4-4 4-4" />
          </svg>
          Back to Facilities
        </Link>
      </div>
    );
  }

  // ── Main ─────────────────────────────────────────────────────────────────

  const hasTags = facility.subtypeAttributes &&
    Object.keys(facility.subtypeAttributes).some((k) => facility.subtypeAttributes[k]);

  return (
    <div className="space-y-6">
      {/* Back link */}
      <Link
        to="/facilities"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-[#64748b] hover:text-[#1e293b] transition-colors"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 16 16" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10 12l-4-4 4-4" />
        </svg>
        Facilities
      </Link>

      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-6">
        <div>
          <h2 className="text-3xl font-black text-[#0f172a] tracking-tight">{facility.name}</h2>
          <p className="mt-1 text-sm text-[#64748b] font-medium">
            {toLabel(facility.type)}&nbsp;&middot;&nbsp;{facility.building || "Unknown building"}
            {facility.floor ? `, ${facility.floor}` : ""}
          </p>
          <div className="mt-4">
            <StatusBadge status={facility.status} />
          </div>
        </div>
        
        {isAdmin && (
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setConfirmModal({ isOpen: true, type: "DEACTIVATE", loading: false })}
              className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-black transition-all active:scale-95 ${
                facility.status === "OUT_OF_SERVICE"
                  ? "bg-emerald-50 text-emerald-700 hover:bg-emerald-100 ring-1 ring-emerald-200"
                  : "bg-amber-50 text-amber-700 hover:bg-amber-100 ring-1 ring-amber-200"
              }`}
            >
              {facility.status === "OUT_OF_SERVICE" ? (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                  Reactivate
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636" /></svg>
                  Deactivate
                </>
              )}
            </button>
            <button
              onClick={() => setConfirmModal({ isOpen: true, type: "DELETE", loading: false })}
              className="inline-flex items-center gap-2 rounded-xl bg-red-50 px-4 py-2.5 text-sm font-black text-red-700 hover:bg-red-100 ring-1 ring-red-200 transition-all active:scale-95"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              Delete Permanently
            </button>
          </div>
        )}
      </div>

      {/* Confirmation Modals */}
      <ConfirmationModal
        isOpen={confirmModal.isOpen && confirmModal.type === "DEACTIVATE"}
        onClose={() => setConfirmModal({ isOpen: false, type: null, loading: false })}
        onConfirm={handleStatusToggle}
        isLoading={confirmModal.loading}
        variant="warning"
        title={facility.status === "OUT_OF_SERVICE" ? "Reactivate Facility?" : "Deactivate Facility?"}
        message={
          facility.status === "OUT_OF_SERVICE"
            ? `This will make ${facility.name} available for bookings once again.`
            : `Are you sure you want to deactivate ${facility.name}? It will be hidden from the public booking flow, but all records will be preserved.`
        }
        confirmText={facility.status === "OUT_OF_SERVICE" ? "Yes, Reactivate" : "Yes, Deactivate"}
      />

      <ConfirmationModal
        isOpen={confirmModal.isOpen && confirmModal.type === "DELETE"}
        onClose={() => setConfirmModal({ isOpen: false, type: null, loading: false })}
        onConfirm={handleDelete}
        isLoading={confirmModal.loading}
        variant="danger"
        confirmWord="DELETE"
        title="Permanent Deletion"
        message={`Are you absolutely sure you want to delete ${facility.name}? This operation is IRREVERSIBLE and will attempt to remove all associated data.`}
        confirmText="Yes, Delete Permanently"
      />

      {/* Info grid */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">

        {/* Core details */}
        <section
          className="rounded-2xl border border-[#e2e8f0] bg-white p-5"
          aria-label="Core details"
        >
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-3">
            Core Information
          </h3>
          <dl>
            <DetailRow label="Facility Code" value={facility.facilityCode} />
            <DetailRow label="Type" value={toLabel(facility.type)} />
            <DetailRow label="Capacity" value={facility.capacity} />
            <DetailRow label="Status" value={toLabel(facility.status)} />
            <DetailRow label="Building" value={facility.building} />
            <DetailRow label="Floor" value={facility.floor} />
            <DetailRow label="Location" value={facility.locationDescription} />
          </dl>
        </section>

        {/* Availability */}
        <section
          className="rounded-2xl border border-[#e2e8f0] bg-white p-5"
          aria-label="Availability schedule"
        >
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-4">
            Availability Schedule
          </h3>
          <WeeklyScheduleGrid
            windows={facility.availabilityWindows}
            status={facility.status}
          />
        </section>

        {/* Live Timetable */}
        <section
          className="rounded-2xl border border-[#e2e8f0] bg-white p-5"
          aria-label="Live Timetable Occupancy"
        >
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-4">
            Live Timetable Occupancy
          </h3>
          <TimetableLiveGrid facilityId={facility.id} />
        </section>

        {/* Resource tags */}
        {hasTags && (
          <section
            className="rounded-2xl border border-[#e2e8f0] bg-white p-5 md:col-span-2"
            aria-label="Resource features"
          >
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-3">
              Features &amp; Resources
            </h3>
            <ResourceTags subtypeAttributes={facility.subtypeAttributes} />
          </section>
        )}

        {/* Metadata */}
        <section
          className="rounded-2xl border border-[#e2e8f0] bg-white p-5 md:col-span-2"
          aria-label="Record metadata"
        >
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-3">
            Record Information
          </h3>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
            <DetailRow
              label="Created"
              value={
                facility.createdAt
                  ? new Date(facility.createdAt).toLocaleString(undefined, {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })
                  : null
              }
            />
            <DetailRow
              label="Last Updated"
              value={
                facility.updatedAt
                  ? new Date(facility.updatedAt).toLocaleString(undefined, {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })
                  : null
              }
            />
          </dl>
        </section>
      </div>
    </div>
  );
}
