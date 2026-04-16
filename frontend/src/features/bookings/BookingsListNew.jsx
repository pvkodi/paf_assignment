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
        return "✓";
      case "PENDING":
        return "◷";
      case "REJECTED":
        return "✕";
      case "CANCELLED":
        return "—";
      default:
        return "•";
    }
  };

  const getStatusBgColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-50 border-green-200";
      case "PENDING":
        return "bg-amber-50 border-amber-200";
      case "REJECTED":
        return "bg-red-50 border-red-200";
      case "CANCELLED":
        return "bg-slate-50 border-slate-200";
      default:
        return "bg-white border-slate-200";
    }
  };

  const getStatusTextColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "text-green-700";
      case "PENDING":
        return "text-amber-700";
      case "REJECTED":
        return "text-red-700";
      case "CANCELLED":
        return "text-slate-700";
      default:
        return "text-slate-700";
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-slate-500">Loading bookings...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with Summary */}
      <div className="border-b border-slate-200 pb-6">
        <div className="flex items-baseline justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">My Bookings</h1>
            <p className="text-slate-600 mt-2">
              {filteredBookings.length} {filteredBookings.length === 1 ? "booking" : "bookings"} found
            </p>
          </div>
          <div className="text-right">
            <p className="text-sm text-slate-500">
              {filter === "ALL" ? "Showing all bookings" : `Showing ${filter.toLowerCase()} bookings`}
            </p>
          </div>
        </div>
      </div>

      {/* Status Filter Tabs */}
      <div className="flex gap-3 flex-wrap">
        {["ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"].map((status) => {
          const isActive = filter === status;
          const count = status === "ALL" ? bookings.length : bookings.filter((b) => b.status === status).length;

          return (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`px-4 py-2 rounded-lg font-medium text-sm transition-all duration-200 ${
                isActive
                  ? "bg-slate-900 text-white shadow-md"
                  : "bg-white text-slate-700 border border-slate-200 hover:border-slate-300 hover:bg-slate-50"
              }`}
            >
              {status} <span className="text-xs opacity-75 ml-1">({count})</span>
            </button>
          );
        })}
      </div>

      {/* Error Messages */}
      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 p-4">
          <p className="text-sm font-medium text-red-900">{error}</p>
        </div>
      )}

      {actionError && (
        <div className="rounded-lg bg-red-50 border border-red-200 p-4">
          <p className="text-sm font-medium text-red-900">{actionError}</p>
        </div>
      )}

      {/* Bookings List */}
      {filteredBookings.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-slate-400 mb-2">
            <svg className="w-12 h-12 mx-auto mb-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <p className="text-slate-600 font-medium">No bookings found</p>
          <p className="text-slate-500 text-sm mt-1">
            {filter === "ALL" ? "Create a new booking to get started" : `No ${filter.toLowerCase()} bookings at the moment`}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredBookings.map((booking) => (
            <div
              key={booking.id}
              className={`rounded-lg border-2 transition-all duration-200 hover:shadow-md ${getStatusBgColor(booking.status)}`}
            >
              <div className="p-5">
                {/* Card Header: Facility Name, Date/Time, Status Badge */}
                <div className="flex items-start justify-between gap-4 mb-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start gap-3">
                      <div className={`mt-1 text-lg font-bold ${getStatusTextColor(booking.status)}`}>
                        {getStatusIcon(booking.status)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="text-lg font-semibold text-slate-900 truncate">
                          {booking.facility?.name || "Unknown Facility"}
                        </h3>
                        <div className="mt-1 flex items-center gap-4 flex-wrap text-sm">
                          <div className="text-slate-600">
                            <span className="font-medium">{formatBookingDate(booking.bookingDate)}</span>
                          </div>
                          <div className="text-slate-600">
                            <span className="font-mono bg-white bg-opacity-50 px-2 py-1 rounded text-xs">
                              {booking.startTime} – {booking.endTime}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className="flex-shrink-0">
                    <span
                      className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold border ${getStatusColorClasses(
                        booking.status,
                      )}`}
                    >
                      {booking.status}
                    </span>
                  </div>
                </div>

                {/* Card Details: 3-Column Information Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4 pb-4 border-t border-slate-200 border-opacity-50 pt-4">
                  {/* Column 1: Facility Details */}
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Facility Details</p>
                    <p className="text-sm text-slate-900 font-medium mt-1">
                      {booking.facility?.type?.replace(/_/g, " ") || "N/A"}
                    </p>
                    <p className="text-xs text-slate-600 mt-1">
                      Capacity: {booking.facility?.capacity || "N/A"}
                    </p>
                  </div>

                  {/* Column 2: Location */}
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Location</p>
                    <p className="text-sm text-slate-900 font-medium mt-1">
                      {booking.facility?.building || "N/A"}
                    </p>
                    <p className="text-xs text-slate-600 mt-1">
                      Floor {booking.facility?.floor || "N/A"} • {booking.facility?.location || "N/A"}
                    </p>
                  </div>

                  {/* Column 3: Attendees & Purpose */}
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Attendees & Purpose</p>
                    <p className="text-sm text-slate-900 font-medium mt-1">
                      {booking.attendees} {booking.attendees === 1 ? "person" : "people"}
                    </p>
                    <p className="text-xs text-slate-600 mt-1 truncate">{booking.purpose}</p>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-2 flex-wrap">
                  <button
                    onClick={() =>
                      setSelectedBookingId(
                        selectedBookingId === booking.id ? null : booking.id,
                      )
                    }
                    className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg font-medium text-sm transition-colors"
                  >
                    {selectedBookingId === booking.id ? "Hide Details" : "View Details"}
                  </button>

                  {canCheckInBooking(booking) && (
                    <button
                      onClick={() => setCheckInBookingId(booking.id)}
                      className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium text-sm transition-colors"
                    >
                      Check In
                    </button>
                  )}

                  {canCancelBooking(booking) && (
                    <button
                      onClick={() => handleCancelBooking(booking.id)}
                      disabled={actionLoading === booking.id}
                      className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {actionLoading === booking.id ? "Cancelling..." : "Cancel"}
                    </button>
                  )}
                </div>
              </div>

              {/* Expanded Details Section - No Duplication */}
              {selectedBookingId === booking.id && (
                <div className="border-t border-slate-200 border-opacity-50">
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
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full">
            <h2 className="text-xl font-semibold text-slate-900 mb-4">Check In Booking</h2>
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
