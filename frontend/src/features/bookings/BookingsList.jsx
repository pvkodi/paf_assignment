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
 * BookingsList Component
 * Displays all bookings for the current user with their status, details, and approval workflow.
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

  if (loading) {
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
        <h1 className="text-3xl font-bold text-slate-900 mb-2">My Bookings</h1>
        <p className="text-slate-600">
          Manage your facility bookings and track approval status
        </p>
      </div>

      {/* Filters */}
      <div className="flex gap-2 flex-wrap">
        {["ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"].map(
          (status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`px-4 py-2 rounded-md font-medium transition ${
                filter === status
                  ? "bg-indigo-600 text-white"
                  : "bg-slate-200 text-slate-700 hover:bg-slate-300"
              }`}
            >
              {status}
            </button>
          ),
        )}
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

      {/* Bookings List */}
      <div className="space-y-4">
        {filteredBookings.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-6">
            <p className="text-slate-600 text-center">
              No bookings found for this category.
            </p>
          </div>
        ) : (
          filteredBookings.map((booking) => (
            <div
              key={booking.id}
              className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow"
            >
              {/* Header with Title and Status */}
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-slate-900">
                    {booking.facility?.name || "Unknown Facility"}
                  </h3>
                  <p className="text-slate-600 text-sm mt-1">
                    {formatBookingDate(booking.bookingDate)} · {booking.startTime} -{" "}
                    {booking.endTime}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-medium border ${getStatusColorClasses(
                      booking.status,
                    )}`}
                  >
                    {booking.status}
                  </span>
                </div>
              </div>

              {/* Booking Info Grid */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
                <div>
                  <span className="font-medium text-slate-700">Attendees</span>
                  <p className="text-slate-600">{booking.attendees}</p>
                </div>
                <div>
                  <span className="font-medium text-slate-700">Purpose</span>
                  <p className="text-slate-600 truncate">{booking.purpose}</p>
                </div>
                <div>
                  <span className="font-medium text-slate-700">Location</span>
                  <p className="text-slate-600">{booking.facility?.location}</p>
                </div>
                <div>
                  <span className="font-medium text-slate-700">Requested By</span>
                  <p className="text-slate-600">{booking.requestedBy?.name}</p>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="flex gap-2 mb-4">
                <button
                  onClick={() =>
                    setSelectedBookingId(
                      selectedBookingId === booking.id ? null : booking.id,
                    )
                  }
                  className="px-3 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 transition-colors"
                >
                  {selectedBookingId === booking.id ? "Hide Details" : "View Details"}
                </button>

                {canCheckInBooking(booking) && (
                  <button
                    onClick={() => setCheckInBookingId(booking.id)}
                    className="px-3 py-2 bg-green-600 text-white rounded text-sm font-medium hover:bg-green-700 transition-colors"
                  >
                    ✓ Check In
                  </button>
                )}
                {canCancelBooking(booking) && (
                  <button
                    onClick={() => handleCancelBooking(booking.id)}
                    disabled={actionLoading === booking.id}
                    className="px-3 py-2 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700 disabled:bg-red-400 transition-colors"
                  >
                    {actionLoading === booking.id ? "Cancelling..." : "✕ Cancel"}
                  </button>
                )}
              </div>

              {/* Booking Details Modal */}
              {selectedBookingId === booking.id && (
                <BookingDetails
                  bookingId={booking.id}
                  onClose={() => setSelectedBookingId(null)}
                  onUpdate={fetchBookings}
                />
              )}

              {/* Creation Info */}
              <div className="pt-4 border-t border-slate-200">
                <p className="text-xs text-slate-500">
                  Created: {new Date(booking.createdAt).toLocaleString()}
                </p>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Check In Modal */}
      {checkInBookingId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full">
            <h2 className="text-xl font-semibold mb-4">Check In Booking</h2>
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