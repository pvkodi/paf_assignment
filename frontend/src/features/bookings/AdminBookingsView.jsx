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
        return "bg-[#e8f5e9] text-[#1b5e20] border-[#c8e6c9]";
      case "PENDING":
        return "bg-[#fffbeb] text-[#b45309] border-[#fde68a]";
      case "REJECTED":
        return "bg-[#fef2f2] text-[#991b1b] border-[#fca5a5]";
      case "CANCELLED":
        return "bg-[#f8fafc] text-[#475569] border-[#e2e8f0]";
      default:
        return "bg-[#f8fafc] text-[#475569] border-[#e2e8f0]";
    }
  };

  if (loading && bookings.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-64">
        <div className="w-8 h-8 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mb-4"></div>
        <div className="text-[#64748b] font-medium">Loading admin bookings...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-[#0f172a] tracking-tight mb-2">
          All Bookings
        </h1>
        <p className="text-[#64748b]">
          View and manage all facility bookings across campus
        </p>
      </div>

      {/* Filter Section */}
      <div className="bg-white rounded-2xl shadow-sm p-6 border border-[#e2e8f0]">
        <h2 className="text-sm font-bold text-[#64748b] uppercase tracking-wider mb-4 flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path></svg>
          Filters
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-semibold text-[#64748b] uppercase tracking-wide mb-1.5">
              Status
            </label>
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange("status", e.target.value)}
              className="w-full rounded-xl border border-[#e2e8f0] px-4 py-2.5 text-sm text-[#0f172a] focus:ring-2 focus:ring-[#6366f1] focus:border-transparent outline-none transition-all"
            >
              <option value="">All Statuses</option>
              <option value="APPROVED">Approved</option>
              <option value="PENDING">Pending</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#64748b] uppercase tracking-wide mb-1.5">
              From Date
            </label>
            <input
              type="date"
              value={filters.from}
              onChange={(e) => handleFilterChange("from", e.target.value)}
              className="w-full rounded-xl border border-[#e2e8f0] px-4 py-2.5 text-sm text-[#0f172a] focus:ring-2 focus:ring-[#6366f1] focus:border-transparent outline-none transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#64748b] uppercase tracking-wide mb-1.5">
              To Date
            </label>
            <input
              type="date"
              value={filters.to}
              onChange={(e) => handleFilterChange("to", e.target.value)}
              className="w-full rounded-xl border border-[#e2e8f0] px-4 py-2.5 text-sm text-[#0f172a] focus:ring-2 focus:ring-[#6366f1] focus:border-transparent outline-none transition-all"
            />
          </div>

          <div className="flex items-end gap-3">
            <button
              onClick={handleApplyFilters}
              className="flex-1 rounded-xl bg-[#0f172a] px-4 py-2.5 text-white text-sm font-semibold hover:bg-[#1e293b] transition-all shadow-sm"
            >
              Apply
            </button>
            <button
              onClick={handleReset}
              className="flex-1 rounded-xl bg-[#f1f5f9] px-4 py-2.5 text-[#475569] text-sm font-semibold hover:bg-[#e2e8f0] hover:text-[#0f172a] transition-all"
            >
              Reset
            </button>
          </div>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="rounded-xl bg-[#fef2f2] p-4 border border-[#fca5a5] flex items-center gap-3">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-medium text-[#991b1b]">{error}</p>
        </div>
      )}

      {actionError && (
        <div className="rounded-xl bg-[#fef2f2] p-4 border border-[#fca5a5] flex items-center gap-3">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-medium text-[#991b1b]">{actionError}</p>
        </div>
      )}

      {/* Bookings Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-[#e2e8f0] overflow-hidden">
        {bookings.length === 0 && !loading ? (
          <div className="p-12 text-center flex flex-col items-center">
            <div className="w-16 h-16 bg-[#f8fafc] rounded-full flex items-center justify-center mb-4">
              <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            </div>
            <h3 className="text-[#0f172a] font-semibold mb-1">No bookings found</h3>
            <p className="text-sm text-[#64748b]">Adjust your filters to see more results.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#e2e8f0] bg-[#f8fafc]">
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider">Facility</th>
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider">Date & Time</th>
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider">Booked For</th>
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider">Purpose</th>
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider text-center">Attendees</th>
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider">Status</th>
                  <th className="px-6 py-4 text-xs font-bold text-[#64748b] uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e2e8f0]/60 bg-white">
                {bookings.map((booking) => (
                  <tr key={booking.id} className="hover:bg-[#f8fafc] transition-colors group">
                    <td className="px-6 py-4">
                      <div>
                        <p className="text-sm font-semibold text-[#0f172a]">
                          {booking.facility?.name || "Unknown"}
                        </p>
                        <p className="text-xs text-[#64748b] mt-0.5">
                          {booking.facility?.location || ""}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm">
                        <p className="font-semibold text-[#0f172a]">
                          {formatDate(booking.booking_date)}
                        </p>
                        <p className="text-xs text-[#64748b] mt-0.5">
                          {formatTime(booking.start_time)} - {formatTime(booking.end_time)}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm font-medium text-[#0f172a]">
                        {booking.booked_for?.displayName || booking.booked_for?.email || "Unknown"}
                      </p>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm text-[#475569] max-w-[200px] truncate">
                        {booking.purpose}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="inline-flex items-center justify-center bg-[#f1f5f9] text-[#475569] px-2 py-1 rounded text-xs font-semibold">
                        {booking.attendees}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${getStatusColor(booking.status)}`}>
                        {booking.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {booking.status !== "CANCELLED" && booking.status !== "REJECTED" && (
                        <button
                          onClick={() => handleCancelBooking(booking.id, booking.version)}
                          disabled={actionLoading === booking.id}
                          className="px-3 py-1.5 bg-white border border-[#fca5a5] text-[#dc2626] rounded-lg text-xs font-semibold hover:bg-[#fef2f2] transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                        >
                          {actionLoading === booking.id ? "Cancelling..." : "Cancel"}
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
        <div className="bg-[#eff6ff] rounded-xl border border-[#dbeafe] p-4 flex items-center justify-between">
          <p className="text-sm text-[#1e40af] font-medium">
            <span className="font-bold">{bookings.length}</span> booking{bookings.length !== 1 ? "s" : ""} matching your criteria
          </p>
        </div>
      )}
    </div>
  );
}
