import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * AdminBookingsView Component
 * Allows ADMIN and FACILITY_MANAGER to view all approved and upcoming bookings.
 * Filter by facility, date range, and status.
 */
export default function AdminBookingsView() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [actionError, setActionError] = useState(null);

  // Filter state
  const [filters, setFilters] = useState({
    facilityId: "",
    status: "", // Empty = show all statuses
    from: new Date(new Date().getFullYear(), new Date().getMonth(), 1)
      .toISOString()
      .split("T")[0],
    to: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0)
      .toISOString()
      .split("T")[0],
  });

  useEffect(() => {
    fetchAdminBookings();
  }, []);

  const fetchAdminBookings = async (appliedFilters = filters) => {
    try {
      setLoading(true);
      setError(null);
      
      // Use the dedicated admin endpoint for all bookings
      const params = {};
      if (appliedFilters.facilityId) {
        params.facilityId = appliedFilters.facilityId;
      }
      if (appliedFilters.status) {
        params.status = appliedFilters.status;
      }
      if (appliedFilters.from) {
        params.from = appliedFilters.from;
      }
      if (appliedFilters.to) {
        params.to = appliedFilters.to;
      }
      
      const response = await apiClient.get("/v1/bookings/admin/all", { params });
      const allBookings = Array.isArray(response.data) ? response.data : [];
      
      console.log("Admin bookings fetched:", {
        count: allBookings.length,
        params: params,
        sampleBooking: allBookings[0]
      });
      
      if (allBookings.length === 0) {
        setError("No bookings found with current filters.");
        setBookings([]);
        return;
      }
      
      // Sort by date descending
      allBookings.sort((a, b) => {
        if (!a.bookingDate || !b.bookingDate) return 0;
        return new Date(b.bookingDate) - new Date(a.bookingDate);
      });
      
      setBookings(allBookings);
      setError(null);
    } catch (err) {
      console.error("Failed to fetch admin bookings:", err);
      const errorMessage = err.response?.data?.message || err.message || "Failed to load bookings";
      setError(errorMessage);
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (field, value) => {
    const newFilters = { ...filters, [field]: value };
    setFilters(newFilters);
  };

  const handleApplyFilters = () => {
    fetchAdminBookings(filters);
  };

  const handleReset = () => {
    const defaultFilters = {
      facilityId: "",
      status: "", // Show all statuses by default
      from: new Date(new Date().getFullYear(), new Date().getMonth(), 1)
        .toISOString()
        .split("T")[0],
      to: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0)
        .toISOString()
        .split("T")[0],
    };
    setFilters(defaultFilters);
    fetchAdminBookings(defaultFilters);
  };

  const handleCancelBooking = async (bookingId) => {
    if (
      !window.confirm(
        "Are you sure you want to cancel this booking? This action cannot be undone."
      )
    ) {
      return;
    }

    try {
      setActionLoading(bookingId);
      setActionError(null);
      await apiClient.post(`/v1/bookings/${bookingId}/cancel`);
      await fetchAdminBookings(filters);
    } catch (err) {
      console.error("Failed to cancel booking:", err);
      setActionError(
        err.response?.data?.message || "Failed to cancel booking"
      );
    } finally {
      setActionLoading(null);
    }
  };

  const formatDate = (dateStr) => {
    return new Date(dateStr).toLocaleDateString("en-US", {
      weekday: "short",
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return "N/A";
    try {
      const parts = timeStr.split(":");
      const hour = parseInt(parts[0]);
      const minute = parts[1];
      const period = hour >= 12 ? "PM" : "AM";
      const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
      return `${displayHour}:${minute} ${period}`;
    } catch (e) {
      return timeStr || "N/A";
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100 text-green-800";
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      case "REJECTED":
        return "bg-red-100 text-red-800";
      case "CANCELLED":
        return "bg-slate-100 text-slate-800";
      default:
        return "bg-slate-100 text-slate-800";
    }
  };

  if (loading && bookings.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-slate-600">Loading bookings...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">
          All Bookings
        </h1>
        <p className="text-slate-600">
          View and manage all facility bookings across campus
        </p>
      </div>

      {/* Filter Section */}
      <div className="bg-white rounded-lg shadow-md p-6 border border-slate-100">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">Filters</h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Status
            </label>
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange("status", e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-slate-900 focus:border-indigo-500 focus:outline-none"
            >
              <option value="">All Statuses</option>
              <option value="APPROVED">Approved</option>
              <option value="PENDING">Pending</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              From Date
            </label>
            <input
              type="date"
              value={filters.from}
              onChange={(e) => handleFilterChange("from", e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-slate-900 focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              To Date
            </label>
            <input
              type="date"
              value={filters.to}
              onChange={(e) => handleFilterChange("to", e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-slate-900 focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div className="flex items-end gap-2">
            <button
              onClick={handleApplyFilters}
              className="flex-1 rounded bg-indigo-600 px-4 py-2 text-white font-medium hover:bg-indigo-700 transition-colors"
            >
              Apply Filters
            </button>
            <button
              onClick={handleReset}
              className="flex-1 rounded bg-slate-300 px-4 py-2 text-slate-900 font-medium hover:bg-slate-400 transition-colors"
            >
              Reset
            </button>
          </div>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="rounded-md bg-red-50 p-4 border border-red-200">
          <p className="text-sm text-red-600">{error}</p>
        </div>
      )}

      {actionError && (
        <div className="rounded-md bg-red-50 p-4 border border-red-200">
          <p className="text-sm text-red-600">{actionError}</p>
        </div>
      )}

      {/* Bookings Table */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-slate-100">
        {bookings.length === 0 ? (
          <div className="p-6 text-center">
            <p className="text-slate-600">No bookings found with current filters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Facility
                  </th>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Date & Time
                  </th>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Booked For
                  </th>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Purpose
                  </th>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Attendees
                  </th>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {bookings.map((booking) => (
                  <tr key={booking.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-medium text-slate-900">
                          {booking.facility?.name || "Unknown"}
                        </p>
                        <p className="text-sm text-slate-600">
                          {booking.facility?.location}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm">
                        <p className="font-medium text-slate-900">
                          {formatDate(booking.bookingDate)}
                        </p>
                        <p className="text-slate-600">
                          {formatTime(booking.startTime)} -{" "}
                          {formatTime(booking.endTime)}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm text-slate-900">
                        {booking.bookedFor?.displayName ||
                          booking.bookedFor?.email ||
                          "Unknown"}
                      </p>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm text-slate-600 max-w-xs truncate">
                        {booking.purpose}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-900">
                      {booking.attendees}
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(
                          booking.status
                        )}`}
                      >
                        {booking.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {booking.status !== "CANCELLED" &&
                        booking.status !== "REJECTED" && (
                          <button
                            title="Cancel booking feature coming soon - backend endpoint not yet implemented"
                            disabled={true}
                            className="px-3 py-1.5 bg-gray-300 text-gray-600 rounded text-xs font-medium cursor-not-allowed"
                          >
                            Cancel (Coming Soon)
                          </button>
                        )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Summary */}
      {bookings.length > 0 && (
        <div className="bg-indigo-50 rounded-lg border border-indigo-200 p-4">
          <p className="text-sm text-indigo-900">
            <span className="font-semibold">{bookings.length}</span> booking
            {bookings.length !== 1 ? "s" : ""} found matching your filters
          </p>
        </div>
      )}
    </div>
  );
}
