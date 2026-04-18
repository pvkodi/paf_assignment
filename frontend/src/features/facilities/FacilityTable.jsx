import React from "react";
import { toLabel } from "./api";

function StatusBadge({ status }) {
  const config = {
    ACTIVE: { dot: "bg-emerald-500", text: "text-emerald-700", bg: "bg-emerald-50 ring-emerald-200" },
    MAINTENANCE: { dot: "bg-amber-500", text: "text-amber-700", bg: "bg-amber-50 ring-amber-200" },
    OUT_OF_SERVICE: { dot: "bg-red-500", text: "text-red-700", bg: "bg-red-50 ring-red-200" },
  }[status] || { dot: "bg-gray-400", text: "text-gray-600", bg: "bg-gray-50 ring-gray-200" };

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${config.bg} ${config.text}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
      {toLabel(status)}
    </span>
  );
}

export default function FacilityTable({ 
  facilities, 
  selectedIds, 
  onSelect, 
  onSelectAll, 
  isAdmin, 
  onEdit, 
  onMarkOOS, 
  onHardDelete, 
  onView 
}) {
  const allSelected = facilities.length > 0 && facilities.every(f => selectedIds.has(f.id));
  const someSelected = facilities.some(f => selectedIds.has(f.id)) && !allSelected;

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200 text-left">
        <thead className="bg-gray-50/50 grayscale-[0.5]">
          <tr>
            <th className="px-6 py-4 w-10">
              <input
                type="checkbox"
                checked={allSelected}
                ref={el => el && (el.indeterminate = someSelected)}
                onChange={onSelectAll}
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
            </th>
            <th className="px-4 py-4 text-[10px] font-black uppercase tracking-widest text-gray-500">Facility</th>
            <th className="px-4 py-4 text-[10px] font-black uppercase tracking-widest text-gray-500">Status</th>
            <th className="px-4 py-4 text-[10px] font-black uppercase tracking-widest text-gray-500">Location</th>
            <th className="px-4 py-4 text-[10px] font-black uppercase tracking-widest text-gray-500 text-center">Capacity</th>
            <th className="px-4 py-4 text-[10px] font-black uppercase tracking-widest text-gray-500 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {facilities.map((f) => (
            <tr 
              key={f.id} 
              className={`group hover:bg-gray-50/80 transition-colors ${selectedIds.has(f.id) ? 'bg-indigo-50/20' : ''}`}
            >
              <td className="px-6 py-4">
                <input
                  type="checkbox"
                  checked={selectedIds.has(f.id)}
                  onChange={() => onSelect(f.id)}
                  className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                />
              </td>
              <td className="px-4 py-4">
                <div className="flex flex-col">
                  <span className="text-sm font-bold text-gray-900 group-hover:text-indigo-600 transition-colors cursor-pointer" onClick={() => onView(f.id)}>{f.name}</span>
                  <span className="text-[10px] font-black text-indigo-400 uppercase tracking-tighter">{toLabel(f.type)}</span>
                </div>
              </td>
              <td className="px-4 py-4">
                <StatusBadge status={f.status} />
              </td>
              <td className="px-4 py-4">
                <div className="flex flex-col text-[11px] text-gray-500">
                  <span className="font-semibold text-gray-700">{f.building || "—"}</span>
                  <span className="opacity-70">{f.floor || "—"}</span>
                </div>
              </td>
              <td className="px-4 py-4 text-center">
                <span className="inline-flex items-center justify-center min-w-[32px] px-2 py-1 rounded-md bg-gray-100 text-xs font-black text-gray-700">
                  {f.capacity ?? "—"}
                </span>
              </td>
              <td className="px-4 py-4 text-right">
                <div className="flex items-center justify-end gap-1 opacity-10 group-hover:opacity-100 transition-opacity">
                   <button onClick={(e) => { e.stopPropagation(); onEdit(f); }} className="p-1.5 hover:text-indigo-600 hover:bg-indigo-50 rounded-md transition-all"><svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg></button>
                   <button onClick={(e) => { e.stopPropagation(); onMarkOOS(f.id); }} className={`p-1.5 rounded-md transition-all ${f.status === "OUT_OF_SERVICE" ? "text-emerald-500 hover:bg-emerald-50" : "text-amber-500 hover:bg-amber-50"}`}>
                     {f.status === "OUT_OF_SERVICE" ? <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg> : <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636" /></svg>}
                   </button>
                   <button onClick={(e) => { e.stopPropagation(); onHardDelete(f.id); }} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-md transition-all"><svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3" /></svg></button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
