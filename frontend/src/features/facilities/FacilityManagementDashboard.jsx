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
      <header className="rounded-lg border border-gray-200 bg-white p-6">
        <h2 className="text-2xl font-semibold text-gray-900">Facility Management Dashboard</h2>
        <p className="mt-2 text-sm text-gray-600">
          Manage campus facility catalog, operational status, and discoverability.
        </p>
      </header>

      <section className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Type</span>
            <select
              value={filters.type}
              onChange={(event) => setFilters((prev) => ({ ...prev, type: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            >
              <option value="">All</option>
              {facilityTypes.map((type) => (
                <option key={type} value={type}>
                  {toDisplayLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Min Capacity</span>
            <input
              type="number"
              min="1"
              value={filters.minCapacity}
              onChange={(event) => setFilters((prev) => ({ ...prev, minCapacity: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            />
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Building</span>
            <input
              type="text"
              value={filters.building}
              onChange={(event) => setFilters((prev) => ({ ...prev, building: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            />
          </label>

          <label className="text-sm text-gray-700">
            <span className="mb-1 block font-medium">Status</span>
            <select
              value={filters.status}
              onChange={(event) => setFilters((prev) => ({ ...prev, status: event.target.value }))}
              className="w-full rounded-md border border-gray-300 px-3 py-2"
            >
              <option value="">All</option>
              {facilityStatuses.map((status) => (
                <option key={status} value={status}>
                  {toDisplayLabel(status)}
                </option>
              ))}
            </select>
          </label>

          <div className="flex items-end justify-end gap-2">
            <button
              type="button"
              onClick={() => setFilters({ type: "", minCapacity: "", building: "", status: "" })}
              className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700"
            >
              Reset
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              disabled={!isAdmin}
              className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              Add Facility
            </button>
          </div>
        </div>
      </section>

      <section className="rounded-lg border border-gray-200 bg-white p-5">
        <h3 className="text-lg font-medium text-gray-900">Quick Recommendations</h3>
        <p className="mt-1 text-sm text-gray-600">Find suggested facilities for typical booking needs.</p>
        <div className="mt-4">
          <BookingRecommendations />
        </div>
      </section>

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
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-gray-500">
                    Loading facilities...
                  </td>
                </tr>
              ) : facilities.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-gray-500">
                    No facilities found
                  </td>
                </tr>
              ) : (
                facilities.map((facility) => (
                  <tr key={facility.id}>
                    <td className="px-4 py-3 text-sm text-gray-900">{facility.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{toDisplayLabel(facility.type)}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{facility.capacity}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{facility.building}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className="rounded-full bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700">
                        {toDisplayLabel(facility.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right text-sm">
                      <div className="inline-flex gap-2">
                        <button
                          type="button"
                          onClick={() => navigate(`/facilities/${facility.id}`)}
                          className="rounded border border-gray-300 px-2 py-1 text-xs font-medium text-gray-700"
                        >
                          View
                        </button>
                        <button
                          type="button"
                          onClick={() => openEditModal(facility)}
                          disabled={!isAdmin}
                          className="rounded border border-gray-300 px-2 py-1 text-xs font-medium text-gray-700 disabled:opacity-50"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => handleMarkOutOfService(facility.id)}
                          disabled={!isAdmin || facility.status === "OUT_OF_SERVICE"}
                          className="rounded border border-red-200 px-2 py-1 text-xs font-medium text-red-700 disabled:opacity-50"
                        >
                          Mark Out of Service
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3 text-sm text-gray-600">
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => loadFacilities(Math.max(0, page - 1), size)}
              disabled={page === 0}
              className="rounded border border-gray-300 px-3 py-1 disabled:opacity-50"
            >
              Previous
            </button>
            <button
              type="button"
              onClick={() => loadFacilities(Math.min(totalPages - 1, page + 1), size)}
              disabled={page + 1 >= totalPages}
              className="rounded border border-gray-300 px-3 py-1 disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      </section>

      {modalOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 p-4">
          <form
            onSubmit={handleSubmit}
            className="w-full max-w-2xl rounded-lg border border-gray-200 bg-white p-6 shadow-lg"
          >
            <h3 className="text-lg font-semibold text-gray-900">
              {editingFacility ? "Edit Facility" : "Add Facility"}
            </h3>

            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Name</span>
                <input
                  required
                  value={formData.name}
                  onChange={(event) => setFormData((prev) => ({ ...prev, name: event.target.value }))}
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>

              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Type</span>
                <select
                  required
                  value={formData.type}
                  onChange={(event) => setFormData((prev) => ({ ...prev, type: event.target.value }))}
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
                  value={formData.capacity}
                  onChange={(event) => setFormData((prev) => ({ ...prev, capacity: event.target.value }))}
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>

              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Building</span>
                <input
                  required
                  value={formData.building}
                  onChange={(event) => setFormData((prev) => ({ ...prev, building: event.target.value }))}
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>

              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Floor</span>
                <input
                  value={formData.floor}
                  onChange={(event) => setFormData((prev) => ({ ...prev, floor: event.target.value }))}
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>

              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Status</span>
                <select
                  value={formData.status}
                  onChange={(event) => setFormData((prev) => ({ ...prev, status: event.target.value }))}
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                >
                  {facilityStatuses.map((status) => (
                    <option key={status} value={status}>
                      {toDisplayLabel(status)}
                    </option>
                  ))}
                </select>
              </label>

              <label className="text-sm text-gray-700 md:col-span-2">
                <span className="mb-1 block font-medium">Location Description</span>
                <input
                  required
                  value={formData.locationDescription}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, locationDescription: event.target.value }))
                  }
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>

              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Availability Start</span>
                <input
                  required
                  type="time"
                  value={String(formData.availabilityStartTime).slice(0, 5)}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, availabilityStartTime: `${event.target.value}:00` }))
                  }
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>

              <label className="text-sm text-gray-700">
                <span className="mb-1 block font-medium">Availability End</span>
                <input
                  required
                  type="time"
                  value={String(formData.availabilityEndTime).slice(0, 5)}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, availabilityEndTime: `${event.target.value}:00` }))
                  }
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={closeModal}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
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
