import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * BookingsList Component
 * Displays all bookings for the current user with their status, details, and approval workflow.
 */
export default function BookingsList() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [filter, setFilter] = useState("ALL"); // ALL, PENDING, APPROVED, REJECTED, CANCELLED

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

  const getStatusColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100 text-green-800 border-green-300";
      case "PENDING":
        return "bg-yellow-100 text-yellow-800 border-yellow-300";
      case "REJECTED":
        return "bg-red-100 text-red-800 border-red-300";
      case "CANCELLED":
        return "bg-slate-100 text-slate-800 border-slate-300";
      default:
        return "bg-slate-100 text-slate-800 border-slate-300";
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const filteredBookings =
    filter === "ALL" ? bookings : bookings.filter((b) => b.status === filter);

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <p className="text-slate-600">Loading bookings...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-slate-900">My Bookings</h2>
          <button
            onClick={fetchBookings}
            className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 transition-colors"
          >
            Refresh
          </button>
        </div>

        {error && (
          <div className="rounded-md bg-red-50 p-4 mb-4">
            <p className="text-sm font-medium text-red-800">{error}</p>
          </div>
        )}

        {/* Filter Tabs */}
        <div className="flex flex-wrap gap-2 mb-4">
          {["ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"].map(
            (status) => (
              <button
                key={status}
                onClick={() => setFilter(status)}
                className={`px-3 py-1 rounded-md font-medium transition-colors ${
                  filter === status
                    ? "bg-blue-600 text-white"
                    : "bg-slate-200 text-slate-700 hover:bg-slate-300"
                }`}
              >
                {status}
              </button>
            ),
          )}
        </div>

        {/* Bookings Count */}
        <p className="text-sm text-slate-600">
          Showing {filteredBookings.length} of {bookings.length} bookings
        </p>
      </div>

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
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-slate-900">
                    {booking.facility?.name || "Unknown Facility"}
                  </h3>
                  <p className="text-sm text-slate-600 mt-1">
                    {formatDate(booking.bookingDate)} • {booking.startTime} -{" "}
                    {booking.endTime}
                  </p>
                </div>

                <div className="flex items-center gap-4">
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-medium border ${getStatusColor(
                      booking.status,
                    )}`}
                  >
                    {booking.status}
                  </span>

                  <button
                    onClick={() =>
                      setSelectedBooking(
                        selectedBooking?.id === booking.id ? null : booking,
                      )
                    }
                    className="px-3 py-1 bg-slate-100 text-slate-700 rounded-md hover:bg-slate-200 transition-colors text-sm"
                  >
                    {selectedBooking?.id === booking.id ? "Hide" : "Details"}
                  </button>
                </div>
              </div>

              {/* Booking Details */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <div>
                  <span className="font-medium text-slate-700">Attendees</span>
                  <p className="text-slate-600">{booking.attendees}</p>
                </div>
                <div>
                  <span className="font-medium text-slate-700">Capacity</span>
                  <p className="text-slate-600">
                    {booking.facility?.capacity || "N/A"}
                  </p>
                </div>
                <div>
                  <span className="font-medium text-slate-700">Location</span>
                  <p className="text-slate-600">
                    {booking.facility?.building} - Floor{" "}
                    {booking.facility?.floor}
                  </p>
                </div>
                <div>
                  <span className="font-medium text-slate-700">Purpose</span>
                  <p className="text-slate-600 truncate">{booking.purpose}</p>
                </div>
              </div>

              {/* Expanded Details */}
              {selectedBooking?.id === booking.id && (
                <div className="mt-4 pt-4 border-t border-slate-200 space-y-4">
                  <div>
                    <h4 className="font-semibold text-slate-900 mb-2">
                      Purpose
                    </h4>
                    <p className="text-slate-700">{booking.purpose}</p>
                  </div>

                  {booking.recurrenceRule && (
                    <div>
                      <h4 className="font-semibold text-slate-900 mb-2">
                        Recurrence
                      </h4>
                      <p className="text-slate-700 font-mono text-sm">
                        {booking.recurrenceRule}
                      </p>
                    </div>
                  )}

                  {booking.approvalSteps &&
                    booking.approvalSteps.length > 0 && (
                      <div>
                        <h4 className="font-semibold text-slate-900 mb-2">
                          Approval Workflow
                        </h4>
                        <div className="space-y-2">
                          {booking.approvalSteps.map((step, index) => (
                            <div
                              key={index}
                              className="flex items-center gap-2 text-sm"
                            >
                              <span className="font-medium text-slate-700 w-24">
                                {step.requiredRole}
                              </span>
                              <span
                                className={`px-2 py-1 rounded text-xs font-medium ${
                                  step.decision === "APPROVED"
                                    ? "bg-green-100 text-green-800"
                                    : step.decision === "REJECTED"
                                      ? "bg-red-100 text-red-800"
                                      : "bg-slate-100 text-slate-800"
                                }`}
                              >
                                {step.decision}
                              </span>
                              {step.decidedAt && (
                                <span className="text-slate-500 text-xs">
                                  {new Date(step.decidedAt).toLocaleString()}
                                </span>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                  <div className="pt-4 border-t border-slate-200">
                    <p className="text-xs text-slate-500">
                      Created: {new Date(booking.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
