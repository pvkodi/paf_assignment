import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import {
  createFacility,
  fetchFacilities,
  markFacilityOutOfService,
  searchFacilities,
  updateFacility,
} from "./api";
import BookingRecommendations from "../bookings/BookingRecommendations";

const facilityTypes = [
  "LECTURE_HALL",
  "LAB",
  "MEETING_ROOM",
  "AUDITORIUM",
  "EQUIPMENT",
  "SPORTS",
];

const facilityStatuses = ["ACTIVE", "MAINTENANCE", "OUT_OF_SERVICE"];

const blankForm = {
  name: "",
  type: "LECTURE_HALL",
  capacity: 1,
  building: "",
  floor: "",
  locationDescription: "",
  availabilityStartTime: "08:00:00",
  availabilityEndTime: "17:00:00",
  status: "ACTIVE",
};

function toDisplayLabel(value) {
  return value ? value.replaceAll("_", " ") : "-";
}

export default function FacilityManagementDashboard() {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [filters, setFilters] = useState({
    type: "",
    minCapacity: "",
    building: "",
    status: "",
  });
  const [facilities, setFacilities] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingFacility, setEditingFacility] = useState(null);
  const [formData, setFormData] = useState(blankForm);
  const [saving, setSaving] = useState(false);

  const filtersActive = useMemo(
    () => Object.values(filters).some((value) => String(value).trim() !== ""),
    [filters],
  );

  const loadFacilities = async (nextPage = page, nextSize = size) => {
    try {
      setLoading(true);
      setError(null);

      const baseParams = {
        page: nextPage,
        size: nextSize,
      };

      const payload = filtersActive
        ? await searchFacilities({
            ...baseParams,
            ...(filters.type ? { type: filters.type } : {}),
            ...(filters.minCapacity ? { minCapacity: Number(filters.minCapacity) } : {}),
            ...(filters.building ? { building: filters.building } : {}),
            ...(filters.status ? { status: filters.status } : {}),
          })
        : await fetchFacilities(baseParams);

      setFacilities(Array.isArray(payload?.content) ? payload.content : []);
      setTotal(Number(payload?.totalElements ?? 0));
      setPage(Number(payload?.number ?? nextPage));
      setSize(Number(payload?.size ?? nextSize));
    } catch {
      setError("Failed to load facilities");
      setFacilities([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFacilities(0, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.type, filters.minCapacity, filters.building, filters.status]);

  const openCreateModal = () => {
    setEditingFacility(null);
    setFormData(blankForm);
    setModalOpen(true);
  };

  const openEditModal = (facility) => {
    setEditingFacility(facility);
    setFormData({
      name: facility.name ?? "",
      type: facility.type ?? "LECTURE_HALL",
      capacity: facility.capacity ?? 1,
      building: facility.building ?? "",
      floor: facility.floor ?? "",
      locationDescription: facility.locationDescription ?? "",
      availabilityStartTime: facility.availabilityStartTime ?? "08:00:00",
      availabilityEndTime: facility.availabilityEndTime ?? "17:00:00",
      status: facility.status ?? "ACTIVE",
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingFacility(null);
    setFormData(blankForm);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!isAdmin) {
      setError("Only admin users can modify facilities");
      return;
    }

    try {
      setSaving(true);
      setError(null);

      const payload = {
        ...formData,
        capacity: Number(formData.capacity),
      };

      if (editingFacility) {
        await updateFacility(editingFacility.id, payload);
      } else {
        await createFacility(payload);
      }

      closeModal();
      await loadFacilities(page, size);
    } catch (requestError) {
      setError(requestError?.response?.data?.message || "Failed to save facility");
    } finally {
      setSaving(false);
    }
  };

  const handleMarkOutOfService = async (facilityId) => {
    if (!isAdmin) {
      setError("Only admin users can modify facilities");
      return;
    }

    try {
      await markFacilityOutOfService(facilityId);
      await loadFacilities(page, size);
    } catch {
      setError("Failed to mark facility as out of service");
    }
  };

  const totalPages = Math.max(1, Math.ceil(total / size));

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-100 bg-white p-6 md:p-8 shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900">Facility Management</h2>
        <p className="mt-2 text-sm font-medium text-slate-500">
          Manage campus facility catalog, operational status, and discoverability.
        </p>
      </header>

      <section className="rounded-[2rem] border border-slate-100 bg-white p-6 shadow-[0_4px_24px_rgb(0,0,0,0.02)] pt-8">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-5 items-end">
          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Type</span>
            <select
              value={filters.type}
              onChange={(event) => setFilters((prev) => ({ ...prev, type: event.target.value }))}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            >
              <option value="">All</option>
              {facilityTypes.map((type) => (
                <option key={type} value={type}>
                  {toDisplayLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Min Capacity</span>
            <input
              type="number"
              min="1"
              value={filters.minCapacity}
              onChange={(event) => setFilters((prev) => ({ ...prev, minCapacity: event.target.value }))}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            />
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Building</span>
            <input
              type="text"
              value={filters.building}
              onChange={(event) => setFilters((prev) => ({ ...prev, building: event.target.value }))}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            />
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Status</span>
            <select
              value={filters.status}
              onChange={(event) => setFilters((prev) => ({ ...prev, status: event.target.value }))}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            >
              <option value="">All</option>
              {facilityStatuses.map((status) => (
                <option key={status} value={status}>
                  {toDisplayLabel(status)}
                </option>
              ))}
            </select>
          </label>

          <div className="flex items-end justify-end gap-2 md:col-start-5">
            <button
              type="button"
              onClick={() => setFilters({ type: "", minCapacity: "", building: "", status: "" })}
              className="px-4 py-3 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-xl shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all flex-1"
            >
              Reset
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              disabled={!isAdmin}
              className="px-4 py-3 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all flex-[2] whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Add Facility
            </button>
          </div>
        </div>
      </section>

      <section>
        <BookingRecommendations />
      </section>

      {error && (
         <div className="rounded-2xl border border-red-100 bg-red-50 p-4">
           <p className="text-sm font-bold text-red-600">{error}</p>
         </div>
      )}

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
                <th className="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-slate-400 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center">
                    <p className="text-slate-400 font-bold text-sm animate-pulse">Loading facilities...</p>
                  </td>
                </tr>
              ) : facilities.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center">
                    <p className="text-slate-500 font-bold text-sm">No facilities found.</p>
                  </td>
                </tr>
              ) : (
                facilities.map((facility) => (
                  <tr key={facility.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4 text-sm font-bold text-slate-900">{facility.name}</td>
                    <td className="px-6 py-4 text-sm font-semibold text-slate-600">{toDisplayLabel(facility.type)}</td>
                    <td className="px-6 py-4 text-sm font-bold text-slate-900 text-center">{facility.capacity}</td>
                    <td className="px-6 py-4 text-sm font-medium text-slate-600">{facility.building || "-"}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${
                        facility.status === "ACTIVE" ? "bg-green-50 text-green-700" :
                        facility.status === "MAINTENANCE" ? "bg-amber-50 text-amber-700" : "bg-red-50 text-red-700"
                      }`}>
                        {toDisplayLabel(facility.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="inline-flex gap-2">
                        <button
                          type="button"
                          onClick={() => navigate(`/facilities/${facility.id}`)}
                          className="px-3 py-1.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-lg text-xs transition-all shadow-[0_2px_8px_rgb(0,0,0,0.02)]"
                        >
                          View
                        </button>
                        <button
                          type="button"
                          onClick={() => openEditModal(facility)}
                          disabled={!isAdmin}
                          className="px-3 py-1.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-900 font-bold rounded-lg text-xs transition-all shadow-[0_2px_8px_rgb(0,0,0,0.02)] disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => handleMarkOutOfService(facility.id)}
                          disabled={!isAdmin || facility.status === "OUT_OF_SERVICE"}
                          className="px-3 py-1.5 bg-red-50 border border-red-100 hover:bg-red-100 text-red-700 font-bold rounded-lg text-xs transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          Out of Service
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between border-t border-slate-100 bg-[#f8f9fa] px-6 py-4">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => loadFacilities(Math.max(0, page - 1), size)}
              disabled={page === 0}
              className="px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-lg text-xs shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all disabled:opacity-50"
            >
              Previous
            </button>
            <button
              type="button"
              onClick={() => loadFacilities(Math.min(totalPages - 1, page + 1), size)}
              disabled={page + 1 >= totalPages}
              className="px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-lg text-xs shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      </section>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
          <form
            onSubmit={handleSubmit}
            className="w-full max-w-2xl rounded-[2rem] border border-slate-100 bg-white p-6 md:p-8 shadow-[0_20px_60px_rgb(0,0,0,0.1)] transition-all transform scale-100"
          >
            <h3 className="text-2xl font-bold tracking-tight text-slate-900 mb-6">
              {editingFacility ? "Edit Facility" : "Add Facility"}
            </h3>

            <div className="grid grid-cols-1 gap-5 md:grid-cols-2 bg-[#f8f9fa] p-6 rounded-3xl">
              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Name</span>
                <input
                  required
                  value={formData.name}
                  onChange={(event) => setFormData((prev) => ({ ...prev, name: event.target.value }))}
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>

              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Type</span>
                <select
                  required
                  value={formData.type}
                  onChange={(event) => setFormData((prev) => ({ ...prev, type: event.target.value }))}
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
                  value={formData.capacity}
                  onChange={(event) => setFormData((prev) => ({ ...prev, capacity: event.target.value }))}
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>

              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Building</span>
                <input
                  required
                  value={formData.building}
                  onChange={(event) => setFormData((prev) => ({ ...prev, building: event.target.value }))}
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>

              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Floor</span>
                <input
                  value={formData.floor}
                  onChange={(event) => setFormData((prev) => ({ ...prev, floor: event.target.value }))}
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>

              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Status</span>
                <select
                  value={formData.status}
                  onChange={(event) => setFormData((prev) => ({ ...prev, status: event.target.value }))}
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                >
                  {facilityStatuses.map((status) => (
                    <option key={status} value={status}>
                      {toDisplayLabel(status)}
                    </option>
                  ))}
                </select>
              </label>

              <label className="text-sm text-slate-700 md:col-span-2">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Location Description</span>
                <input
                  required
                  value={formData.locationDescription}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, locationDescription: event.target.value }))
                  }
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>

              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Availability Start</span>
                <input
                  required
                  type="time"
                  value={String(formData.availabilityStartTime).slice(0, 5)}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, availabilityStartTime: `${event.target.value}:00` }))
                  }
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>

              <label className="text-sm text-slate-700">
                <span className="mb-2 block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Availability End</span>
                <input
                  required
                  type="time"
                  value={String(formData.availabilityEndTime).slice(0, 5)}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, availabilityEndTime: `${event.target.value}:00` }))
                  }
                  className="w-full rounded-xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 focus:ring-4 focus:ring-slate-200 transition-all shadow-sm outline-none"
                />
              </label>
            </div>

            <div className="mt-8 flex justify-end gap-3">
              <button
                type="button"
                onClick={closeModal}
                className="px-6 py-3.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-xl shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="px-8 py-3.5 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-60"
              >
                {saving ? "Saving..." : editingFacility ? "Save Changes" : "Create Facility"}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
