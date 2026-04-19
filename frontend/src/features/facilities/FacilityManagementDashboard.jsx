import React, {
  useEffect,
  useCallback,
  useMemo,
  useRef,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../../contexts/AuthContext";
import {
  createFacility,
  fetchFacilities,
  markFacilityOutOfService,
  searchFacilities,
  updateFacility,
  uploadFacilityTimetable,
  batchCreateFacilities,
  hardDeleteFacility,
  bulkActionFacilities,
  toLabel,
} from "./api";
import ConfirmationModal from "../../components/ConfirmationModal";
import FacilityCard from "./FacilityCard";
import FacilityTable from "./FacilityTable";

// ─── Constants ───────────────────────────────────────────────────────────────

const FACILITY_TYPES = [
  "LECTURE_HALL",
  "LAB",
  "MEETING_ROOM",
  "AUDITORIUM",
  "EQUIPMENT",
  "SPORTS",
];

const FACILITY_STATUSES = ["ACTIVE", "MAINTENANCE", "OUT_OF_SERVICE"];

const DAYS_OF_WEEK = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const DAY_ABBR = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun",
};

const BLANK_FORM = {
  name: "",
  type: "LECTURE_HALL",
  capacity: "",
  building: "",
  floor: "",
  locationDescription: "",
  availabilityStartTime: "08:00:00",
  availabilityEndTime: "17:00:00",
  status: "ACTIVE",
  availabilityWindows: [],
  outOfServiceStart: "",
  outOfServiceEnd: "",
  latitude: "",
  longitude: "",
  geofenceRadiusMeters: 100,
};

const BLANK_WINDOW = {
  dayOfWeek: "MONDAY",
  startTime: "08:00",
  endTime: "17:00",
};

// ─── Timetable Uploader ────────────────────────────────────────────────────────

function TimetableUploader({ onUploadComplete }) {
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);
  const navigate = useNavigate();

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.name.endsWith(".html")) {
      toast.error("Please select a valid HTML timetable file.");
      return;
    }

    try {
      setUploading(true);
      const toastId = toast.loading("Parsing timetable HTML...");

      const res = await uploadFacilityTimetable(file);

      toast.update(toastId, {
        render: `Successfully parsed timetable. Proceeding to preview...`,
        type: "success",
        isLoading: false,
        autoClose: 2000,
      });

      if (onUploadComplete) onUploadComplete();
      navigate("/facilities/timetable-preview", { state: { result: res } });
    } catch (err) {
      toast.error(
        "Failed to upload timetable: " +
          (err?.response?.data?.error || err.message),
      );
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  return (
    <>
      <div className="inline-block relative">
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileChange}
          accept=".html"
          className="hidden"
          disabled={uploading}
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="shrink-0 flex items-center gap-1.5 rounded-2xl border border-[#e2e8f0] bg-white px-4 py-2 text-sm font-medium text-[#334155] shadow-sm hover:bg-[#f8fafc] focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1 transition-colors disabled:opacity-50"
        >
          <svg
            className="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"
            />
          </svg>
          {uploading ? "Uploading..." : "Upload Timetable"}
        </button>
      </div>
    </>
  );
}

// ─── Utility ─────────────────────────────────────────────────────────────────

function formatTime(t) {
  if (!t) return "—";
  return String(t).slice(0, 5);
}

// ─── Status Badge (Contextual for Dashboard) ───────────────────────────────────

function StatusBadge({ status }) {
  const config = {
    ACTIVE: {
      dot: "bg-emerald-500",
      text: "text-emerald-700",
      bg: "bg-emerald-50 ring-emerald-200",
    },
    MAINTENANCE: {
      dot: "bg-amber-500",
      text: "text-amber-700",
      bg: "bg-amber-50 ring-amber-200",
    },
    OUT_OF_SERVICE: {
      dot: "bg-red-500",
      text: "text-red-700",
      bg: "bg-red-50 ring-red-200",
    },
  }[status] || {
    dot: "bg-gray-400",
    text: "text-[#475569]",
    bg: "bg-[#f8fafc] ring-gray-200",
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${config.bg} ${config.text}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
      {toLabel(status)}
    </span>
  );
}

// ─── Skeleton Card ────────────────────────────────────────────────────────────

function SkeletonCard() {
  return (
    <div className="rounded-2xl border border-[#e2e8f0] bg-white p-5 animate-pulse">
      <div className="flex items-start justify-between mb-4">
        <div className="h-4 w-2/3 rounded bg-gray-200" />
        <div className="h-5 w-16 rounded-full bg-gray-200" />
      </div>
      <div className="space-y-2 mb-4">
        <div className="h-3 w-1/2 rounded bg-gray-200" />
        <div className="h-3 w-1/3 rounded bg-gray-200" />
      </div>
      <div className="flex gap-2">
        <div className="h-3 w-12 rounded bg-gray-200" />
        <div className="h-3 w-12 rounded bg-gray-200" />
        <div className="h-3 w-12 rounded bg-gray-200" />
      </div>
    </div>
  );
}

// ─── Window Schedule Builder ──────────────────────────────────────────────────

function WindowScheduleBuilder({ windows, onChange }) {
  const [draft, setDraft] = useState(BLANK_WINDOW);

  const add = () => {
    if (!draft.dayOfWeek || !draft.startTime || !draft.endTime) return;
    if (draft.startTime >= draft.endTime) {
      toast.error("Start time must be before end time.");
      return;
    }
    onChange([...windows, { ...draft }]);
    setDraft(BLANK_WINDOW);
  };

  const remove = (idx) => {
    onChange(windows.filter((_, i) => i !== idx));
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        {windows.map((w, i) => (
          <span
            key={i}
            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-50 px-2.5 py-1 text-xs font-medium text-indigo-700 ring-1 ring-inset ring-indigo-200"
          >
            {DAY_ABBR[w.dayOfWeek]} {formatTime(w.startTime)}–
            {formatTime(w.endTime)}
            <button
              type="button"
              onClick={() => remove(i)}
              className="ml-0.5 rounded-sm text-indigo-400 hover:text-indigo-700 transition-colors"
              aria-label={`Remove ${w.dayOfWeek} window`}
            >
              <svg className="h-3 w-3" viewBox="0 0 12 12" fill="currentColor">
                <path
                  d="M9 3L3 9M3 3l6 6"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
              </svg>
            </button>
          </span>
        ))}
        {windows.length === 0 && (
          <p className="text-xs text-[#94a3b8] italic">No windows added yet.</p>
        )}
      </div>

      <div className="flex flex-wrap items-end gap-2">
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-[#475569]">Day</label>
          <select
            value={draft.dayOfWeek}
            onChange={(e) =>
              setDraft((p) => ({ ...p, dayOfWeek: e.target.value }))
            }
            className="rounded-xl border border-[#e2e8f0] px-2 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            {DAYS_OF_WEEK.map((d) => (
              <option key={d} value={d}>
                {DAY_ABBR[d]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-[#475569]">From</label>
          <input
            type="time"
            value={draft.startTime}
            onChange={(e) =>
              setDraft((p) => ({ ...p, startTime: e.target.value }))
            }
            className="rounded-xl border border-[#e2e8f0] px-2 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-[#475569]">To</label>
          <input
            type="time"
            value={draft.endTime}
            onChange={(e) =>
              setDraft((p) => ({ ...p, endTime: e.target.value }))
            }
            className="rounded-xl border border-[#e2e8f0] px-2 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        <button
          type="button"
          onClick={add}
          className="rounded-xl bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700 transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1"
        >
          Add Window
        </button>
      </div>
    </div>
  );
}

// ─── Bulk Action Bar ───────────────────────────────────────────────────────────

function BulkActionBar({ selectedCount, onDeactivate, onDelete, onClear }) {
  if (selectedCount === 0) return null;

  return (
    <div className="fixed bottom-8 left-1/2 -translate-x-1/2 z-40 animate-in slide-in-from-bottom-8 duration-300">
      <div className="bg-[#0f172a] text-white rounded-2xl shadow-2xl ring-1 ring-white/10 px-6 py-4 flex items-center gap-8 backdrop-blur-md bg-opacity-95">
        <div className="flex items-center gap-3 pr-8 border-r border-gray-700">
          <span className="flex h-6 w-6 items-center justify-center rounded-full bg-indigo-500 text-[11px] font-black">
            {selectedCount}
          </span>
          <span className="text-sm font-semibold tracking-tight">
            Items selected
          </span>
        </div>

        <div className="flex items-center gap-4">
          <button
            onClick={onDeactivate}
            className="flex items-center gap-2 text-xs font-bold text-amber-400 hover:text-amber-300 transition-colors uppercase tracking-widest"
          >
            <svg
              className="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M18.364 18.364A9 9 0 005.636 5.636"
              />
            </svg>
            Deactivate
          </button>

          <button
            onClick={onDelete}
            className="flex items-center gap-2 text-xs font-bold text-red-500 hover:text-red-400 transition-colors uppercase tracking-widest"
          >
            <svg
              className="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6"
              />
            </svg>
            Delete Permanently
          </button>
        </div>

        <button
          onClick={onClear}
          className="ml-4 text-xs font-bold text-[#94a3b8] hover:text-white transition-colors uppercase tracking-widest"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}

// ─── Form Modal ───────────────────────────────────────────────────────────────

function FacilityFormModal({
  isOpen,
  editing,
  formData,
  setFormData,
  onSubmit,
  onClose,
  saving,
}) {
  if (!isOpen) return null;

  const [locatingGeofence, setLocatingGeofence] = useState(false);

  const handleGetCurrentLocation = () => {
    if (!navigator.geolocation) {
      toast.error("Geolocation is not supported by your browser.");
      return;
    }

    setLocatingGeofence(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        setFormData((p) => ({
          ...p,
          latitude: parseFloat(latitude.toFixed(6)),
          longitude: parseFloat(longitude.toFixed(6)),
        }));
        toast.success(
          `Location updated: ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
        );
        setLocatingGeofence(false);
      },
      (error) => {
        let errorMsg = "Failed to get your location.";
        if (error.code === error.PERMISSION_DENIED) {
          errorMsg =
            "Location permission denied. Please enable location access in your browser settings.";
        } else if (error.code === error.POSITION_UNAVAILABLE) {
          errorMsg = "Location information is unavailable.";
        } else if (error.code === error.TIMEOUT) {
          errorMsg = "Location request timed out.";
        }
        toast.error(errorMsg);
        setLocatingGeofence(false);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    );
  };

  const errors = useMemo(() => {
    const e = {};
    if (!formData.name.trim()) e.name = "Name is required.";
    const cap = Number(formData.capacity);
    if (!formData.capacity || isNaN(cap) || cap < 1)
      e.capacity = "Capacity must be greater than 0.";
    if (!formData.building.trim()) e.building = "Building is required.";
    if (!formData.locationDescription.trim())
      e.locationDescription = "Location description is required.";
    return e;
  }, [formData]);

  const hasErrors = Object.keys(errors).length > 0;

  const field = (id, label, children, error) => (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-xs font-medium text-[#334155]">
        {label}
      </label>
      {children}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );

  const inputCls = (err) =>
    `w-full rounded-xl border px-3 py-2 text-sm focus:outline-none focus:ring-2 transition-shadow ${
      err
        ? "border-red-400 focus:ring-red-300"
        : "border-[#e2e8f0] focus:border-indigo-500 focus:ring-indigo-200"
    }`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
      <div
        className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-xl border border-[#e2e8f0] bg-white shadow-xl"
        role="dialog"
        aria-modal="true"
        aria-label={editing ? "Edit facility" : "Create facility"}
      >
        <div className="border-b border-[#f1f5f9] px-6 py-4 flex items-center justify-between">
          <h2 className="text-base font-semibold text-[#0f172a]">
            {editing ? "Edit Facility" : "Create Facility"}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1 text-[#94a3b8] hover:bg-[#f1f5f9] hover:text-[#334155] transition-colors"
            aria-label="Close dialog"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 20 20"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M6 6l8 8M6 14L14 6"
              />
            </svg>
          </button>
        </div>

        <form onSubmit={onSubmit} noValidate>
          <div className="px-6 py-5 space-y-6">
            {/* Basic info */}
            <fieldset>
              <legend className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-3">
                Basic Information
              </legend>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {field(
                  "fac-name",
                  "Name *",
                  <input
                    id="fac-name"
                    type="text"
                    value={formData.name}
                    onChange={(e) =>
                      setFormData((p) => ({ ...p, name: e.target.value }))
                    }
                    className={inputCls(errors.name)}
                    placeholder="e.g. Lecture Hall A"
                  />,
                  errors.name,
                )}

                {field(
                  "fac-type",
                  "Type *",
                  <select
                    id="fac-type"
                    value={formData.type}
                    onChange={(e) =>
                      setFormData((p) => ({ ...p, type: e.target.value }))
                    }
                    className={inputCls()}
                  >
                    {FACILITY_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {toLabel(t)}
                      </option>
                    ))}
                  </select>,
                )}

                {field(
                  "fac-capacity",
                  "Capacity *",
                  <input
                    id="fac-capacity"
                    type="number"
                    min="1"
                    value={formData.capacity}
                    onChange={(e) =>
                      setFormData((p) => ({ ...p, capacity: e.target.value }))
                    }
                    className={inputCls(errors.capacity)}
                    placeholder="e.g. 120"
                  />,
                  errors.capacity,
                )}

                {field(
                  "fac-status",
                  "Status",
                  <select
                    id="fac-status"
                    value={formData.status}
                    onChange={(e) =>
                      setFormData((p) => ({ ...p, status: e.target.value }))
                    }
                    className={inputCls()}
                  >
                    {FACILITY_STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {toLabel(s)}
                      </option>
                    ))}
                  </select>,
                )}
              </div>
            </fieldset>

            {/* Location */}
            <fieldset>
              <legend className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-3">
                Location
              </legend>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {field(
                  "fac-building",
                  "Building *",
                  <input
                    id="fac-building"
                    type="text"
                    value={formData.building}
                    onChange={(e) =>
                      setFormData((p) => ({ ...p, building: e.target.value }))
                    }
                    className={inputCls(errors.building)}
                    placeholder="e.g. Engineering Block"
                  />,
                  errors.building,
                )}

                {field(
                  "fac-floor",
                  "Floor",
                  <input
                    id="fac-floor"
                    type="text"
                    value={formData.floor}
                    onChange={(e) =>
                      setFormData((p) => ({ ...p, floor: e.target.value }))
                    }
                    className={inputCls()}
                    placeholder="e.g. 2nd Floor"
                  />,
                )}

                <div className="sm:col-span-2">
                  {field("fac-location", "Location Description *",
                    <input
                      id="fac-location"
                      type="text"
                      value={formData.locationDescription}
                      onChange={(e) =>
                        setFormData((p) => ({
                          ...p,
                          locationDescription: e.target.value,
                        }))
                      }
                      className={inputCls(errors.locationDescription)}
                      placeholder="e.g. North wing, near the main entrance"
                    />,
                    errors.locationDescription,
                  )}
                </div>
              </div>
            </fieldset>

            {/* Geofencing */}
            <fieldset>
              <legend className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-3">
                Geofencing (Optional)
              </legend>
              <div className="flex items-center justify-between gap-3 mb-3">
                <p className="text-xs text-[#64748b]">
                  Add GPS coordinates for location-based check-in verification.
                </p>
                <button
                  type="button"
                  onClick={handleGetCurrentLocation}
                  disabled={locatingGeofence}
                  className="flex items-center gap-1.5 rounded-xl bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-2 focus:ring-blue-300"
                  title="Auto-fill latitude and longitude based on your current location"
                >
                  {locatingGeofence ? (
                    <>
                      <svg
                        className="h-4 w-4 animate-spin"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                        />
                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                        />
                      </svg>
                      Locating...
                    </>
                  ) : (
                    <>
                      <svg
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                        />
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                        />
                      </svg>
                      Use Current Location
                    </>
                  )}
                </button>
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {field(
                  "fac-latitude",
                  "Latitude",
                  <input
                    id="fac-latitude"
                    type="number"
                    step="0.000001"
                    value={formData.latitude}
                    onChange={(e) =>
                      setFormData((p) => ({
                        ...p,
                        latitude: e.target.value
                          ? parseFloat(e.target.value)
                          : "",
                      }))
                    }
                    className={inputCls()}
                    placeholder="e.g. 6.9271"
                  />,
                )}

                {field(
                  "fac-longitude",
                  "Longitude",
                  <input
                    id="fac-longitude"
                    type="number"
                    step="0.000001"
                    value={formData.longitude}
                    onChange={(e) =>
                      setFormData((p) => ({
                        ...p,
                        longitude: e.target.value
                          ? parseFloat(e.target.value)
                          : "",
                      }))
                    }
                    className={inputCls()}
                    placeholder="e.g. 80.7744"
                  />,
                )}

                {field(
                  "fac-geofence-radius",
                  "Geofence Radius (meters)",
                  <input
                    id="fac-geofence-radius"
                    type="number"
                    min="10"
                    max="1000"
                    value={formData.geofenceRadiusMeters}
                    onChange={(e) =>
                      setFormData((p) => ({
                        ...p,
                        geofenceRadiusMeters: e.target.value
                          ? parseInt(e.target.value)
                          : 100,
                      }))
                    }
                    className={inputCls()}
                    placeholder="e.g. 100"
                  />,
                )}
              </div>
            </fieldset>

            {/* Availability Schedule */}
            <fieldset>
              <legend className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-1">
                Availability Schedule
              </legend>
              <p className="text-xs text-[#64748b] mb-3">
                Add one or more time windows. Multiple windows per day are
                supported.
              </p>
              <WindowScheduleBuilder
                windows={formData.availabilityWindows}
                onChange={(ws) =>
                  setFormData((p) => ({ ...p, availabilityWindows: ws }))
                }
              />
            </fieldset>

            {/* Scheduled Maintenance / Out of Service */}
            <fieldset>
              <legend className="text-xs font-semibold uppercase tracking-wider text-[#94a3b8] mb-1">
                Out of Service Schedule
              </legend>
              <p className="text-xs text-[#64748b] mb-3">
                Schedule a period where the facility will be marked as out of service.
              </p>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {field("fac-oos-start", "Scheduled Start",
                  <input
                    id="fac-oos-start"
                    type="datetime-local"
                    value={formData.outOfServiceStart ? formData.outOfServiceStart.slice(0, 16) : ""}
                    onChange={(e) => setFormData((p) => ({ ...p, outOfServiceStart: e.target.value }))}
                    className={inputCls()}
                  />
                )}

                {field("fac-oos-end", "Scheduled End (Optional)",
                  <input
                    id="fac-oos-end"
                    type="datetime-local"
                    value={formData.outOfServiceEnd ? formData.outOfServiceEnd.slice(0, 16) : ""}
                    onChange={(e) => setFormData((p) => ({ ...p, outOfServiceEnd: e.target.value }))}
                    className={inputCls()}
                  />
                )}
              </div>
            </fieldset>
          </div>

          <div className="border-t border-[#f1f5f9] px-6 py-4 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="rounded-2xl border border-[#e2e8f0] px-4 py-2 text-sm font-medium text-[#334155] hover:bg-[#f8fafc] transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving || hasErrors}
              className="rounded-2xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1"
            >
              {saving
                ? "Saving..."
                : editing
                  ? "Save Changes"
                  : "Create Facility"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Dashboard ───────────────────────────────────────────────────────────

export default function FacilityManagementDashboard() {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [filters, setFilters] = useState({
    name: "",
    type: "",
    minCapacity: "",
    building: "",
    status: "",
  });
  const [facilities, setFacilities] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const PAGE_SIZE = 12;
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingFacility, setEditingFacility] = useState(null);
  const [formData, setFormData] = useState(BLANK_FORM);
  const [saving, setSaving] = useState(false);

  // New states for overhaul
  const [viewMode, setViewMode] = useState("grid"); // 'grid' | 'table'
  const [selectedIds, setSelectedIds] = useState(new Set());

  // Secure deletion states
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    type: null, // 'SINGLE_DELETE' | 'BULK_DEACTIVATE' | 'BULK_DELETE'
    targetId: null,
    loading: false,
    isForce: false,
    errorMessage: null,
  });

  const debounceRef = useRef(null);

  const filtersActive = useMemo(
    () => Object.values(filters).some((v) => String(v).trim() !== ""),
    [filters],
  );

  const [searchQuery, setSearchQuery] = useState("");
  const searchActive = searchQuery.trim().length > 0;

  const displayedFacilities = facilities;

  // ── Data loading ──────────────────────────────────────────────────────────

  const load = useCallback(
    async (p = 0) => {
      try {
        setLoading(true);
        const params = { page: p, size: PAGE_SIZE };
        const keyword = searchQuery.trim();
        const payload = (filtersActive || keyword)
          ? await searchFacilities({
              ...params,
              ...(filters.name ? { name: filters.name } : {}),
              ...(keyword ? { query: keyword } : {}),
              ...(filters.type ? { type: filters.type } : {}),
              ...(filters.minCapacity
                ? { minCapacity: Number(filters.minCapacity) }
                : {}),
              ...(filters.building ? { building: filters.building } : {}),
              ...(filters.status ? { status: filters.status } : {}),
            })
          : await fetchFacilities(params);

        setFacilities(payload?.content ?? []);
        setTotal(payload?.totalElements ?? 0);
        setTotalPages(Math.max(1, payload?.totalPages ?? 1));
        setPage(payload?.number ?? p);
      } catch {
        toast.error("Failed to load facilities.");
        setFacilities([]);
      } finally {
        setLoading(false);
      }
    },
    [filtersActive, filters, searchQuery],
  );

  // Debounced filter reaction
  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => load(0), 300);
    return () => clearTimeout(debounceRef.current);
  }, [load]);

  // ── Handlers ──────────────────────────────────────────────────────────────

  const openCreate = () => {
    setEditingFacility(null);
    setFormData(BLANK_FORM);
    setModalOpen(true);
  };

  const openEdit = (facility) => {
    setEditingFacility(facility);
    setFormData({
      name: facility.name ?? "",
      type: facility.type ?? "LECTURE_HALL",
      capacity: facility.capacity ?? "",
      building: facility.building ?? "",
      floor: facility.floor ?? "",
      locationDescription: facility.locationDescription ?? "",
      availabilityStartTime: facility.availabilityStartTime ?? "08:00:00",
      availabilityEndTime: facility.availabilityEndTime ?? "17:00:00",
      status: facility.status ?? "ACTIVE",
      availabilityWindows: (facility.availabilityWindows ?? []).map((w) => ({
        dayOfWeek: w.dayOfWeek,
        startTime: w.startTime ? String(w.startTime).slice(0, 5) : "08:00",
        endTime: w.endTime ? String(w.endTime).slice(0, 5) : "17:00",
      })),
      outOfServiceStart: facility.outOfServiceStart ?? "",
      outOfServiceEnd: facility.outOfServiceEnd ?? "",
      latitude: facility.latitude ?? "",
      longitude: facility.longitude ?? "",
      geofenceRadiusMeters: facility.geofenceRadiusMeters ?? 100,
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isAdmin) {
      toast.error("Only admins can modify facilities.");
      return;
    }
    try {
      setSaving(true);
      const payload = {
        ...formData,
        capacity: Number(formData.capacity),
        latitude: formData.latitude ? parseFloat(formData.latitude) : null,
        longitude: formData.longitude ? parseFloat(formData.longitude) : null,
        geofenceRadiusMeters: formData.geofenceRadiusMeters
          ? parseInt(formData.geofenceRadiusMeters)
          : 100,
        availabilityWindows: formData.availabilityWindows.map((w) => ({
          dayOfWeek: w.dayOfWeek,
          startTime:
            w.startTime.length === 5 ? `${w.startTime}:00` : w.startTime,
          endTime: w.endTime.length === 5 ? `${w.endTime}:00` : w.endTime,
        })),
        outOfServiceStart: formData.outOfServiceStart || null,
        outOfServiceEnd: formData.outOfServiceEnd || null,
      };
      console.log("📤 Sending facility payload:", payload);
      if (editingFacility) {
        const updatedFacility = await updateFacility(editingFacility.id, payload);
        // Immediately patch the updated facility into local state so the card
        // reflects changes without waiting for the async refetch to resolve.
        setFacilities((prev) =>
          prev.map((f) => (f.id === editingFacility.id ? { ...f, ...updatedFacility } : f))
        );
        toast.success("Facility updated successfully.");
        console.log("✅ Facility updated successfully");

        const detailId = updatedFacility?.id ?? editingFacility.id;
        setModalOpen(false);
        navigate(`/facilities/${detailId}`);
        return;
      } else {
        const createdFacility = await createFacility(payload);
        toast.success("Facility created successfully.");
        console.log("✅ Facility created successfully");

        const detailId = createdFacility?.id;
        setModalOpen(false);
        if (detailId) {
          navigate(`/facilities/${detailId}`);
          return;
        }
      }
      setModalOpen(false);
      await load(page);
    } catch (err) {
      console.error("❌ Error saving facility:", err);
      toast.error(err?.response?.data?.message || "Failed to save facility.");
    } finally {
      setSaving(false);
    }
  };

  const handleMarkOOS = async (id) => {
    if (!isAdmin) {
      toast.error("Only admins can modify facilities.");
      return;
    }
    try {
      const facility = facilities.find((f) => f.id === id);
      const isOOS = facility?.status === "OUT_OF_SERVICE";

      if (isOOS) {
        // Reactivate: send PUT with full proper payload including all required fields
        const reactivationPayload = {
          facilityCode: facility.facilityCode,
          name: facility.name,
          type: facility.type,
          capacity: facility.capacity,
          building:
            facility.building && facility.building.trim()
              ? facility.building
              : "TBD",
          floor:
            facility.floor && facility.floor.trim() ? facility.floor : "N/A",
          locationDescription:
            facility.locationDescription && facility.locationDescription.trim()
              ? facility.locationDescription
              : "Facility",
          availabilityStartTime: facility.availabilityStartTime,
          availabilityEndTime: facility.availabilityEndTime,
          status: "ACTIVE",
          availabilityWindows: facility.availabilityWindows || [],
          subtypeAttributes: facility.subtypeAttributes || {},
        };
        await updateFacility(id, reactivationPayload);
        setFacilities((prev) =>
          prev.map((f) => (f.id === id ? { ...f, status: "ACTIVE" } : f))
        );
        toast.success("Facility reactivated successfully.");
      } else {
        // Deactivate (soft delete)
        await markFacilityOutOfService(id);
        setFacilities((prev) =>
          prev.map((f) => (f.id === id ? { ...f, status: "OUT_OF_SERVICE" } : f))
        );
        toast.success("Facility deactivated.");
      }
      await load(page);
    } catch {
      toast.error("Failed to update facility status.");
    }
  };

  const handleHardDelete = (id) => {
    if (!isAdmin) return;
    setConfirmModal({
      isOpen: true,
      type: "SINGLE_DELETE",
      targetId: id,
      loading: false,
    });
  };

  const executeHardDelete = async () => {
    const id = confirmModal.targetId;
    if (!id) return;

    try {
      setConfirmModal((prev) => ({
        ...prev,
        loading: true,
        errorMessage: null,
      }));
      await hardDeleteFacility(id, confirmModal.isForce);
      toast.success(
        confirmModal.isForce
          ? "Facility and all associated data deleted."
          : "Facility permanently deleted.",
      );
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
      setConfirmModal({
        isOpen: false,
        type: null,
        targetId: null,
        loading: false,
        isForce: false,
        errorMessage: null,
      });
      await load(page);
    } catch (err) {
      if (err?.response?.status === 409) {
        setConfirmModal((prev) => ({
          ...prev,
          loading: false,
          isForce: true,
          errorMessage:
            err.response.data.message || "This facility has active bookings.",
        }));
      } else {
        toast.error(
          err?.response?.data?.message || "Failed to delete facility.",
        );
        setConfirmModal({
          isOpen: false,
          type: null,
          targetId: null,
          loading: false,
          isForce: false,
          errorMessage: null,
        });
      }
    }
  };

  const handleBulkAction = (action) => {
    if (!isAdmin || selectedIds.size === 0) return;
    setConfirmModal({
      isOpen: true,
      type: action === "DELETE" ? "BULK_DELETE" : "BULK_DEACTIVATE",
      targetId: null,
      loading: false,
    });
  };

  const executeBulkAction = async () => {
    const action =
      confirmModal.type === "BULK_DELETE" ? "DELETE" : "DEACTIVATE";
    if (!isAdmin || selectedIds.size === 0) return;

    try {
      setConfirmModal((prev) => ({
        ...prev,
        loading: true,
        errorMessage: null,
      }));
      await bulkActionFacilities(
        Array.from(selectedIds),
        action,
        confirmModal.isForce,
      );
      toast.success(
        `Successfully ${action === "DELETE" ? "deleted" : "deactivated"} ${selectedIds.size} facilities.`,
      );
      setSelectedIds(new Set());
      setConfirmModal({
        isOpen: false,
        type: null,
        targetId: null,
        loading: false,
        isForce: false,
        errorMessage: null,
      });
      await load(page);
    } catch (err) {
      if (err?.response?.status === 409) {
        setConfirmModal((prev) => ({
          ...prev,
          loading: false,
          isForce: true,
          errorMessage:
            "One or more facilities have active bookings. Force deletion will cancel these bookings and notify users.",
        }));
      } else {
        toast.error("Bulk operation failed.");
        setConfirmModal({
          isOpen: false,
          type: null,
          targetId: null,
          loading: false,
          isForce: false,
          errorMessage: null,
        });
      }
    }
  };

  const closeConfirmModal = () => {
    setConfirmModal({
      isOpen: false,
      type: null,
      targetId: null,
      loading: false,
      isForce: false,
      errorMessage: null,
    });
  };

  const toggleSelection = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    const allOnPage = facilities.every((f) => selectedIds.has(f.id));
    setSelectedIds((prev) => {
      const next = new Set(prev);
      facilities.forEach((f) => {
        if (allOnPage) next.delete(f.id);
        else next.add(f.id);
      });
      return next;
    });
  };

  const resetFilters = () =>
    setFilters({
      name: "",
      type: "",
      minCapacity: "",
      building: "",
      status: "",
    });

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-[#0f172a]">
            Facilities
          </h2>
          <p className="mt-1 text-sm text-[#64748b]">
            Manage campus resources, availability schedules, and operational
            status.
          </p>
        </div>

        {/* Inline search */}
        <div className="flex-1 max-w-sm">
          <div className="relative">
            <svg
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
              fill="none" viewBox="0 0 24 24" stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
            </svg>
            <input
              id="facility-search"
              type="search"
              placeholder="Search by name, type, building…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-9 pr-3 text-sm shadow-sm placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-shadow"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                aria-label="Clear search"
              >
                <svg className="h-3.5 w-3.5" viewBox="0 0 12 12" fill="currentColor">
                  <path d="M9 3L3 9M3 3l6 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </button>
            )}
          </div>
        </div>
        <div className="flex items-center gap-4">
          {/* View Toggle */}
          <div className="flex items-center p-1 bg-[#f1f5f9] rounded-2xl border border-[#e2e8f0]">
            <button
              onClick={() => setViewMode("grid")}
              className={`p-1.5 rounded-xl transition-all ${viewMode === "grid" ? "bg-white shadow text-indigo-600" : "text-[#64748b] hover:text-[#0f172a]"}`}
              title="Grid View"
            >
              <svg
                className="w-5 h-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
                />
              </svg>
            </button>
            <button
              onClick={() => setViewMode("table")}
              className={`p-1.5 rounded-xl transition-all ${viewMode === "table" ? "bg-white shadow text-indigo-600" : "text-[#64748b] hover:text-[#0f172a]"}`}
              title="Table View"
            >
              <svg
                className="w-5 h-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6h16M4 12h16M4 18h16"
                />
              </svg>
            </button>
          </div>

          {isAdmin && (
            <div className="flex items-center gap-3">
              <TimetableUploader onUploadComplete={() => load(page)} />
              <button
                type="button"
                id="btn-add-facility"
                onClick={openCreate}
                className="shrink-0 flex items-center gap-1.5 rounded-2xl bg-indigo-600 px-4 py-2 text-sm font-black text-white shadow-lg shadow-indigo-100 hover:bg-indigo-700 transition-all active:scale-95"
              >
                <svg
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 4v16m8-8H4"
                  />
                </svg>
                Add Facility
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Filters bar */}
      <div className="rounded-2xl border border-[#e2e8f0] bg-white p-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <div className="flex flex-col gap-1">
            <label
              htmlFor="filter-name"
              className="text-xs font-medium text-[#64748b]"
            >
              Facility Name
            </label>
            <input
              id="filter-name"
              type="text"
              placeholder="Search by name..."
              value={filters.name}
              onChange={(e) =>
                setFilters((p) => ({ ...p, name: e.target.value }))
              }
              className="rounded-xl border border-[#e2e8f0] px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="filter-type"
              className="text-xs font-medium text-[#64748b]"
            >
              Type
            </label>
            <select
              id="filter-type"
              value={filters.type}
              onChange={(e) =>
                setFilters((p) => ({ ...p, type: e.target.value }))
              }
              className="rounded-xl border border-[#e2e8f0] px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">All types</option>
              {FACILITY_TYPES.map((t) => (
                <option key={t} value={t}>
                  {toLabel(t)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="filter-capacity"
              className="text-xs font-medium text-[#64748b]"
            >
              Min Capacity
            </label>
            <input
              id="filter-capacity"
              type="number"
              min="1"
              placeholder="e.g. 50"
              value={filters.minCapacity}
              onChange={(e) =>
                setFilters((p) => ({ ...p, minCapacity: e.target.value }))
              }
              className="rounded-xl border border-[#e2e8f0] px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="filter-building"
              className="text-xs font-medium text-[#64748b]"
            >
              Building
            </label>
            <input
              id="filter-building"
              type="text"
              placeholder="e.g. Block A"
              value={filters.building}
              onChange={(e) =>
                setFilters((p) => ({ ...p, building: e.target.value }))
              }
              className="rounded-xl border border-[#e2e8f0] px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label
              htmlFor="filter-status"
              className="text-xs font-medium text-[#64748b]"
            >
              Status
            </label>
            <select
              id="filter-status"
              value={filters.status}
              onChange={(e) =>
                setFilters((p) => ({ ...p, status: e.target.value }))
              }
              className="rounded-xl border border-[#e2e8f0] px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">All statuses</option>
              {FACILITY_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {toLabel(s)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-end">
            <button
              type="button"
              onClick={resetFilters}
              disabled={!filtersActive}
              className="w-full rounded-xl border border-[#e2e8f0] px-3 py-2 text-sm font-medium text-[#334155] hover:bg-[#f8fafc] disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Clear filters
            </button>
          </div>
        </div>

        {total > 0 && (
          <p className="mt-3 text-xs text-[#94a3b8]">
            {total} {total === 1 ? "result" : "results"} found
            {filtersActive || searchActive ? " matching current search criteria" : ""}
          </p>
        )}
      </div>

      {/* Result View (Grid or Table) */}
      {loading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : displayedFacilities.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-[#e2e8f0] bg-white py-16 text-center">
          <p className="text-sm font-medium text-[#0f172a]">
            No facilities found
          </p>
          <p className="mt-1 text-xs text-[#64748b]">
            {searchQuery
              ? `No results for "${searchQuery}". Try a different search term.`
              : filtersActive
              ? "Try adjusting or clearing your filters."
              : "Add a facility to get started."}
          </p>
        </div>
      ) : viewMode === "grid" ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {displayedFacilities.map((f) => (
            <FacilityCard
              key={f.id}
              facility={f}
              isAdmin={isAdmin}
              isSelected={selectedIds.has(f.id)}
              onSelect={toggleSelection}
              onView={(id) => navigate(`/facilities/${id}`)}
              onEdit={openEdit}
              onMarkOOS={handleMarkOOS}
              onHardDelete={handleHardDelete}
            />
          ))}
        </div>
      ) : (
  <FacilityTable
          facilities={displayedFacilities}
          selectedIds={selectedIds}
          onSelect={toggleSelection}
          onSelectAll={toggleAll}
          isAdmin={isAdmin}
          onEdit={openEdit}
          onMarkOOS={handleMarkOOS}
          onHardDelete={handleHardDelete}
          onView={(id) => navigate(`/facilities/${id}`)}
        />
      )}

      {/* Bulk Action Bar */}
      <BulkActionBar
        selectedCount={selectedIds.size}
        onDeactivate={() => handleBulkAction("DEACTIVATE")}
        onDelete={() => handleBulkAction("DELETE")}
        onClear={() => setSelectedIds(new Set())}
      />

      {/* Confirmation Modals */}
      <ConfirmationModal
        isOpen={confirmModal.isOpen && confirmModal.type === "SINGLE_DELETE"}
        onClose={closeConfirmModal}
        onConfirm={executeHardDelete}
        isLoading={confirmModal.loading}
        variant={confirmModal.isForce ? "warning" : "danger"}
        confirmWord={confirmModal.isForce ? null : "DELETE"}
        title={
          confirmModal.isForce
            ? "⚠️ Warning: Active Bookings"
            : "Delete Facility?"
        }
        message={
          confirmModal.errorMessage ||
          "Are you sure you want to permanently delete this facility? This operation is IRREVERSIBLE."
        }
        confirmText={
          confirmModal.isForce
            ? "Force Delete & Notify Users"
            : "Yes, Delete Permanently"
        }
      />

      <ConfirmationModal
        isOpen={confirmModal.isOpen && confirmModal.type === "BULK_DEACTIVATE"}
        onClose={closeConfirmModal}
        onConfirm={executeBulkAction}
        isLoading={confirmModal.loading}
        variant="warning"
        title="Bulk Deactivation"
        message={`Are you sure you want to deactivate ${selectedIds.size} selected facilities? They will be hidden from the public booking flow.`}
        confirmText={`Deactivate ${selectedIds.size} Items`}
      />

      <ConfirmationModal
        isOpen={confirmModal.isOpen && confirmModal.type === "BULK_DELETE"}
        onClose={closeConfirmModal}
        onConfirm={executeBulkAction}
        isLoading={confirmModal.loading}
        variant={confirmModal.isForce ? "warning" : "danger"}
        confirmWord={confirmModal.isForce ? null : "DELETE"}
        title={
          confirmModal.isForce
            ? "⚠️ Warning: Active Bookings Found"
            : "Bulk Permanent Deletion"
        }
        message={
          confirmModal.errorMessage ||
          `Are you absolutely sure you want to permanently delete ${selectedIds.size} facilities? This operation is IRREVERSIBLE and will attempt to remove all associated data.`
        }
        confirmText={
          confirmModal.isForce
            ? "Force Delete All & Notify"
            : `Delete ${selectedIds.size} Items Permanently`
        }
      />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-[#e2e8f0] pt-4">
          <p className="text-xs text-[#64748b]">
            Page {page + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => load(Math.max(0, page - 1))}
              disabled={page === 0}
              className="rounded-xl border border-[#e2e8f0] px-3 py-1.5 text-xs font-medium text-[#334155] hover:bg-[#f8fafc] disabled:opacity-40 transition-colors"
            >
              Previous
            </button>
            <button
              type="button"
              onClick={() => load(Math.min(totalPages - 1, page + 1))}
              disabled={page + 1 >= totalPages}
              className="rounded-xl border border-[#e2e8f0] px-3 py-1.5 text-xs font-medium text-[#334155] hover:bg-[#f8fafc] disabled:opacity-40 transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* Modal */}
      <FacilityFormModal
        isOpen={modalOpen}
        editing={editingFacility}
        formData={formData}
        setFormData={setFormData}
        onSubmit={handleSubmit}
        onClose={() => setModalOpen(false)}
        saving={saving}
      />
    </div>
  );
}
