import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";
import BookingDetails from "./BookingDetails";
import CheckInComponent from "./CheckInComponent";
import {
  getStatusColorClasses,
  formatBookingDate,
  canCancelBooking,
  canCheckInBooking,
} from "../../utils/bookingUtils";

/**
 * BookingsList Component - Professional Senior-Level Design
 * Displays all bookings for the current user with their status, details, and approval workflow.
 * Optimized for clarity, information hierarchy, and user experience.
 */
export default function BookingsList() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedBookingId, setSelectedBookingId] = useState(null);
  const [checkInBookingId, setCheckInBookingId] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get("/v1/bookings");
      setBookings(Array.isArray(response.data) ? response.data : []);
      setError(null);
    } catch (err) {
      console.error("Failed to fetch bookings:", err);
      setError(err.response?.data?.message || "Failed to load bookings");
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) {
      return;
    }

    try {
      setActionLoading(bookingId);
      setActionError(null);
      await apiClient.post(`/v1/bookings/${bookingId}/cancel`);
      await fetchBookings();
    } catch (err) {
      console.error("Failed to cancel booking:", err);
      setActionError(err.response?.data?.message || "Failed to cancel booking");
    } finally {
      setActionLoading(null);
    }
  };

  const filteredBookings =
    filter === "ALL" ? bookings : bookings.filter((b) => b.status === filter);

  const getStatusIcon = (status) => {
    switch (status) {
      case "APPROVED":
        return <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>;
      case "PENDING":
        return <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>;
      case "REJECTED":
        return <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>;
      case "CANCELLED":
        return <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"></path></svg>;
      default:
        return <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>;
    }
  };

  const getStatusBgColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "bg-white border-[#e2e8f0]";
      case "PENDING":
        return "bg-white border-[#e2e8f0]";
      case "REJECTED":
        return "bg-white border-[#e2e8f0]";
      case "CANCELLED":
        return "bg-[#f8fafc] border-[#e2e8f0] opacity-80";
      default:
        return "bg-white border-[#e2e8f0]";
    }
  };

  const getStatusIconColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "text-[#10b981] bg-[#d1fae5]";
      case "PENDING":
        return "text-[#f59e0b] bg-[#fef3c7]";
      case "REJECTED":
        return "text-[#ef4444] bg-[#fee2e2]";
      case "CANCELLED":
        return "text-[#64748b] bg-[#f1f5f9]";
      default:
        return "text-[#64748b] bg-[#f1f5f9]";
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-64">
        <div className="w-8 h-8 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mb-4"></div>
        <div className="text-[#64748b] font-medium">Loading bookings...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with Summary */}
      <div className="border-b border-[#e2e8f0] pb-6">
        <div className="flex items-baseline justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-[#0f172a] tracking-tight">My Bookings</h1>
            <p className="text-[#64748b] mt-2 font-medium">
              {filteredBookings.length} {filteredBookings.length === 1 ? "booking" : "bookings"} found
            </p>
          </div>
          <div className="text-right">
            <p className="text-sm font-semibold text-[#94a3b8] uppercase tracking-wider">
              {filter === "ALL" ? "All Bookings" : `${filter} Bookings`}
            </p>
          </div>
        </div>
      </div>

      {/* Status Filter Tabs */}
      <div className="flex gap-2 flex-wrap">
        {["ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"].map((status) => {
          const isActive = filter === status;
          const count = status === "ALL" ? bookings.length : bookings.filter((b) => b.status === status).length;

          return (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`px-4 py-2 rounded-xl font-semibold text-sm transition-all duration-200 flex items-center gap-2 ${
                isActive
                  ? "bg-[#0f172a] text-white shadow-sm"
                  : "bg-white text-[#475569] border border-[#e2e8f0] hover:border-[#cbd5e1] hover:bg-[#f8fafc]"
              }`}
            >
              {status}
              <span className={`px-1.5 py-0.5 rounded-md text-[10px] ${isActive ? 'bg-white/20 text-white' : 'bg-[#f1f5f9] text-[#64748b]'}`}>
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Error Messages */}
      {error && (
        <div className="rounded-xl bg-[#fef2f2] border border-[#fca5a5] p-4 flex items-center gap-3">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-semibold text-[#991b1b]">{error}</p>
        </div>
      )}

      {actionError && (
        <div className="rounded-xl bg-[#fef2f2] border border-[#fca5a5] p-4 flex items-center gap-3">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-semibold text-[#991b1b]">{actionError}</p>
        </div>
      )}

      {/* Bookings List */}
      {filteredBookings.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-[#e2e8f0] shadow-sm">
          <div className="w-16 h-16 bg-[#f8fafc] rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <p className="text-[#0f172a] font-bold text-lg">No bookings found</p>
          <p className="text-[#64748b] text-sm mt-1">
            {filter === "ALL" ? "Create a new booking to get started" : `No ${filter.toLowerCase()} bookings at the moment`}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredBookings.map((booking) => (
            <div
              key={booking.id}
              className={`rounded-2xl border transition-all duration-200 hover:shadow-md overflow-hidden ${getStatusBgColor(booking.status)}`}
            >
              <div className="p-6">
                {/* Card Header: Facility Name, Date/Time, Status Badge */}
                <div className="flex items-start justify-between gap-4 mb-5">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start gap-3">
                      <div className={`mt-1 p-2 rounded-xl flex-shrink-0 ${getStatusIconColor(booking.status)}`}>
                        {getStatusIcon(booking.status)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="text-xl font-bold text-[#0f172a] truncate tracking-tight">
                          {booking.facility?.name || "Unknown Facility"}
                        </h3>
                        <div className="mt-2 flex items-center gap-4 flex-wrap text-sm">
                          <div className="text-[#475569] font-medium flex items-center gap-1.5">
                            <svg className="w-4 h-4 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                            {formatBookingDate(booking.bookingDate)}
                          </div>
                          <div className="text-[#475569] flex items-center gap-1.5">
                            <svg className="w-4 h-4 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                            <span className="font-mono bg-[#f1f5f9] px-2 py-1 rounded-md text-xs font-semibold">
                              {booking.startTime} – {booking.endTime}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className="flex-shrink-0">
                    <span
                      className={`inline-flex items-center px-3 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                        booking.status === "APPROVED" ? "bg-[#e8f5e9] text-[#1b5e20] border-[#c8e6c9]" :
                        booking.status === "PENDING" ? "bg-[#fffbeb] text-[#b45309] border-[#fde68a]" :
                        booking.status === "REJECTED" ? "bg-[#fef2f2] text-[#991b1b] border-[#fca5a5]" :
                        "bg-[#f1f5f9] text-[#475569] border-[#e2e8f0]"
                      }`}
                    >
                      {booking.status}
                    </span>
                  </div>
                </div>

                {/* Card Details: 3-Column Information Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-5 pb-5 border-t border-[#e2e8f0] pt-5">
                  {/* Column 1: Facility Details */}
                  <div>
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1">Facility Details</p>
                    <p className="text-sm text-[#0f172a] font-semibold">
                      {booking.facility?.type?.replace(/_/g, " ") || "N/A"}
                    </p>
                    <p className="text-xs text-[#64748b] mt-0.5">
                      Capacity: {booking.facility?.capacity || "N/A"}
                    </p>
                  </div>

                  {/* Column 2: Location */}
                  <div>
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1">Location</p>
                    <p className="text-sm text-[#0f172a] font-semibold">
                      {booking.facility?.building || "N/A"}
                    </p>
                    <p className="text-xs text-[#64748b] mt-0.5">
                      Floor {booking.facility?.floor || "N/A"} • {booking.facility?.location || "N/A"}
                    </p>
                  </div>

                  {/* Column 3: Attendees & Purpose */}
                  <div>
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1">Attendees & Purpose</p>
                    <p className="text-sm text-[#0f172a] font-semibold">
                      {booking.attendees} {booking.attendees === 1 ? "person" : "people"}
                    </p>
                    <p className="text-xs text-[#64748b] mt-0.5 truncate">{booking.purpose}</p>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-3 flex-wrap">
                  <button
                    onClick={() =>
                      setSelectedBookingId(
                        selectedBookingId === booking.id ? null : booking.id,
                      )
                    }
                    className="px-5 py-2.5 bg-[#f1f5f9] hover:bg-[#e2e8f0] text-[#0f172a] rounded-xl font-semibold text-sm transition-colors flex items-center gap-2"
                  >
                    {selectedBookingId === booking.id ? "Hide Details" : "View Details"}
                    <svg className={`w-4 h-4 transition-transform ${selectedBookingId === booking.id ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                  </button>

                  {canCheckInBooking(booking) && (
                    <button
                      onClick={() => setCheckInBookingId(booking.id)}
                      className="px-5 py-2.5 bg-[#10b981] hover:bg-[#059669] text-white rounded-xl font-semibold text-sm transition-colors shadow-sm"
                    >
                      Check In Now
                    </button>
                  )}

                  {canCancelBooking(booking) && (
                    <button
                      onClick={() => handleCancelBooking(booking.id)}
                      disabled={actionLoading === booking.id}
                      className="px-5 py-2.5 bg-white border border-[#fca5a5] hover:bg-[#fef2f2] text-[#ef4444] rounded-xl font-semibold text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {actionLoading === booking.id ? "Cancelling..." : "Cancel Booking"}
                    </button>
                  )}
                </div>
              </div>

              {/* Expanded Details Section */}
              {selectedBookingId === booking.id && (
                <div className="border-t border-[#e2e8f0] bg-[#f8fafc]">
                  <BookingDetails
                    bookingId={booking.id}
                    onClose={() => setSelectedBookingId(null)}
                    onUpdate={fetchBookings}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Check In Modal */}
      {checkInBookingId && (
        <div className="fixed inset-0 bg-[#0f172a]/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md animate-in fade-in zoom-in duration-200">
            <CheckInComponent
              bookingId={checkInBookingId}
              onCheckInSuccess={() => {
                setCheckInBookingId(null);
                fetchBookings();
              }}
              onClose={() => setCheckInBookingId(null)}
            />
          </div>
        </div>
      )}
    </div>
  );
}
