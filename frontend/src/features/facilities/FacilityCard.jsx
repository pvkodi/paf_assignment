import React, { useMemo } from "react";
import { toLabel } from "./api"; // I'll assume it's in api or I'll define it locally to be safe

const DAY_ABBR = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun",
};

function StatusBadge({ status }) {
  const config = {
    ACTIVE: { dot: "bg-emerald-500", text: "text-emerald-700", bg: "bg-emerald-50 ring-emerald-200" },
    MAINTENANCE: { dot: "bg-amber-500", text: "text-amber-700", bg: "bg-amber-50 ring-amber-200" },
    OUT_OF_SERVICE: { dot: "bg-red-500", text: "text-red-700", bg: "bg-red-50 ring-red-200" },
  }[status] || { dot: "bg-gray-400", text: "text-gray-600", bg: "bg-gray-50 ring-gray-200" };

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${config.bg} ${config.text}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
      {status ? status.replace(/_/g, " ") : "—"}
    </span>
  );
}

function ResourceTags({ subtypeAttributes }) {
  if (!subtypeAttributes) return null;
  const tags = [];

  if (subtypeAttributes.avEquipment || subtypeAttributes.avEnabled || subtypeAttributes.soundSystem) {
    tags.push("AV Equipped");
  }
  if (subtypeAttributes.wheelchairAccessible) tags.push("Accessible");
  if (subtypeAttributes.cateringAllowed) tags.push("Catering");
  if (subtypeAttributes.labType) tags.push(subtypeAttributes.labType);
  if (subtypeAttributes.softwareList) tags.push("Software Lab");
  if (subtypeAttributes.safetyEquipment) tags.push("Safety Gear");
  if (subtypeAttributes.sportsType) tags.push(subtypeAttributes.sportsType);
  if (subtypeAttributes.equipmentAvailable) tags.push("Equipment");

  if (tags.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-1.5 mt-2">
      {tags.map((tag) => (
        <span
          key={tag}
          className="rounded-md bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600"
        >
          {tag}
        </span>
      ))}
    </div>
  );
}

export default function FacilityCard({ 
  facility, 
  isAdmin, 
  onEdit, 
  onMarkOOS, 
  onView, 
  isSelected, 
  onSelect, 
  onHardDelete 
}) {
  const toggleSelection = (e) => {
    e.stopPropagation();
    onSelect(facility.id);
  };

  return (
    <article
      className={`group relative flex flex-col rounded-xl border transition-all duration-300 ${
        isSelected 
          ? "border-indigo-500 bg-indigo-50/30 shadow-md ring-1 ring-indigo-500" 
          : "border-gray-200 bg-white shadow-sm hover:shadow-md hover:-translate-y-0.5"
      }`}
      aria-labelledby={`fac-${facility.id}-name`}
    >
      {/* Selection Checkbox Overlay */}
      <div className="absolute top-3 left-3 z-10">
        <input
          type="checkbox"
          checked={isSelected}
          onChange={toggleSelection}
          className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
        />
      </div>

      <div className="p-5 flex-1 flex flex-col pl-10">
        {/* Header row */}
        <div className="flex items-start justify-between gap-3 mb-3">
          <div className="min-w-0">
            <h3
              id={`fac-${facility.id}-name`}
              className="truncate text-sm font-bold text-gray-900 leading-snug"
            >
              {facility.name}
            </h3>
            <p className="mt-0.5 text-[10px] font-black uppercase tracking-widest text-indigo-500">
              {facility.type ? facility.type.replace(/_/g, " ") : "—"}
            </p>
          </div>
          <StatusBadge status={facility.status} />
        </div>

        {/* Info rows */}
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-xs text-gray-600 mb-4">
          <div className="flex flex-col">
            <dt className="text-[10px] font-bold text-gray-400 uppercase tracking-tight">Capacity</dt>
            <dd className="font-semibold text-gray-900">{facility.capacity ?? "—"}</dd>
          </div>
          <div className="flex flex-col">
            <dt className="text-[10px] font-bold text-gray-400 uppercase tracking-tight">Building</dt>
            <dd className="font-semibold text-gray-900 truncate">{facility.building || "—"}</dd>
          </div>
        </dl>

        {/* Resource tags */}
        <ResourceTags subtypeAttributes={facility.subtypeAttributes} />

        {/* Actions */}
        <div className="mt-6 pt-4 flex items-center justify-between border-t border-gray-100">
          <button
            type="button"
            onClick={() => onView(facility.id)}
            className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 transition-colors uppercase tracking-wider"
          >
            Details
          </button>
          {isAdmin && (
            <div className="flex gap-1.5">
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); onEdit(facility); }}
                className="p-1.5 rounded-md text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 transition-all"
                title="Edit Facility"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); onMarkOOS(facility.id); }}
                className={`p-1.5 rounded-md transition-all ${
                  facility.status === "OUT_OF_SERVICE" 
                    ? "text-emerald-500 hover:bg-emerald-50" 
                    : "text-amber-500 hover:bg-amber-50"
                }`}
                title={facility.status === "OUT_OF_SERVICE" ? "Reactivate" : "Deactivate (Soft Delete)"}
              >
                {facility.status === "OUT_OF_SERVICE" ? (
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                ) : (
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                  </svg>
                )}
              </button>

              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); onHardDelete(facility.id); }}
                className="p-1.5 rounded-md text-gray-400 hover:text-red-600 hover:bg-red-50 transition-all"
                title="Permanently Delete"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          )}
        </div>
      </div>
    </article>
  );
}
