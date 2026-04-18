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
        if (!a.booking_date || !b.booking_date) return 0;
        return new Date(b.booking_date) - new Date(a.booking_date);
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

  const handleCancelBooking = async (bookingId, version) => {
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
      const params = {};
      if (version) {
        params.version = version;
      }
      await apiClient.post(`/v1/bookings/${bookingId}/cancel`, null, { params });
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
        return "bg-green-50 text-green-700 border border-green-200";
      case "PENDING":
        return "bg-orange-50 text-[#49BBBB] border border-orange-200";
      case "REJECTED":
        return "bg-red-50 text-red-700 border border-red-200";
      case "CANCELLED":
        return "bg-slate-50 text-slate-600 border border-slate-200";
      default:
        return "bg-slate-50 text-slate-700 border border-slate-200";
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
    <div className="space-y-8 bg-[#f8f9fa] min-h-screen p-6 md:p-8 rounded-3xl">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-2">
        <div>
          <h1 className="text-4xl font-bold tracking-tight text-slate-900">
            All Bookings
          </h1>
          <p className="text-slate-500 mt-2 text-sm font-medium">
            View and manage all facility bookings across campus
          </p>
        </div>
      </div>

      {/* Filter Section */}
      <div className="bg-white rounded-3xl shadow-[0_4px_24px_rgb(0,0,0,0.02)] p-6 border border-slate-100">
        <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4">Focus View</h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-2">
              Status
            </label>
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange("status", e.target.value)}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            >
              <option value="">All Statuses</option>
              <option value="APPROVED">Approved</option>
              <option value="PENDING">Pending</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-2">
              From Date
            </label>
            <input
              type="date"
              value={filters.from}
              onChange={(e) => handleFilterChange("from", e.target.value)}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-2">
              To Date
            </label>
            <input
              type="date"
              value={filters.to}
              onChange={(e) => handleFilterChange("to", e.target.value)}
              className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none"
            />
          </div>

          <div className="flex items-end gap-3">
            <button
              onClick={handleApplyFilters}
              className="flex-1 rounded-xl bg-[#49BBBB] hover:bg-[#3CA0A0] text-white shadow-[0_4px_14px_rgba(73,187,187,0.3)] px-4 py-3 text-sm font-bold transition-all transform hover:-translate-y-0.5 active:translate-y-0"
            >
              Filter
            </button>
            <button
              onClick={handleReset}
              className="flex-1 rounded-xl bg-slate-50 text-slate-600 border border-slate-100 hover:bg-slate-100 hover:text-slate-900 px-4 py-3 text-sm font-bold transition-colors"
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
      <div className="bg-white rounded-3xl shadow-[0_4px_24px_rgb(0,0,0,0.02)] overflow-hidden border border-slate-100">
        {bookings.length === 0 ? (
          <div className="p-16 text-center">
            <div className="w-16 h-16 bg-slate-50 flex items-center justify-center rounded-full mx-auto mb-4 border border-slate-100">
              <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>
            </div>
            <p className="text-slate-500 font-medium">No bookings found with current filters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-50/50 border-b border-slate-100">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Facility
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Date & Time
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Booked For
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Pupose
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Attendees
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100/80">
                {bookings.map((booking) => (
                  <tr key={booking.id} className="hover:bg-slate-50/50 transition-colors group">
                    <td className="px-6 py-5">
                      <div>
                        <p className="font-bold text-sm text-slate-900 group-hover:text-slate-800 transition-colors">
                          {booking.facility?.name || "Unknown"}
                        </p>
                        <p className="text-xs font-medium text-slate-500 mt-1 flex items-center gap-1">
                          {booking.facility?.location || ""}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <div>
                        <p className="font-bold text-sm text-slate-900">
                          {formatDate(booking.booking_date)}
                        </p>
                        <p className="text-xs font-medium text-slate-500 mt-1">
                          {formatTime(booking.start_time)} -{" "}
                          {formatTime(booking.end_time)}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <p className="text-sm font-bold text-slate-700">
                        {booking.booked_for?.displayName ||
                          booking.booked_for?.email ||
                          "Unknown"}
                      </p>
                    </td>
                    <td className="px-6 py-5">
                      <p className="text-sm font-medium text-slate-600 max-w-[12rem] truncate">
                        {booking.purpose}
                      </p>
                    </td>
                    <td className="px-6 py-5">
                      <div className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-slate-100 text-xs font-bold text-slate-600">
                        {booking.attendees}
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <span
                        className={`inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${getStatusColor(
                          booking.status
                        )}`}
                      >
                        {booking.status}
                      </span>
                    </td>
                    <td className="px-6 py-5">
                      {booking.status !== "CANCELLED" &&
                        booking.status !== "REJECTED" && (
                          <button
                            onClick={() => handleCancelBooking(booking.id, booking.version)}
                            disabled={actionLoading === booking.id}
                            className="px-3 py-1.5 bg-white border border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 rounded-lg text-xs font-bold transition-all disabled:opacity-50"
                          >
                            {actionLoading === booking.id ? "Wait..." : "Cancel"}
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
        <div className="bg-slate-900 rounded-2xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] p-4 flex items-center justify-center">
          <p className="text-sm font-medium text-slate-300">
            <span className="font-bold text-white mr-1">{bookings.length}</span> booking
            {bookings.length !== 1 ? "s" : ""} match your criteria
          </p>
        </div>
      )}
    </div>
  );
}
