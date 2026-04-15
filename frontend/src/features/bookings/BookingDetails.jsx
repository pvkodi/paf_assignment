import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * BookingDetails Component
 * Displays complete booking details including facility, timeline, and approval workflow.
 * Opened as a modal or drawer from BookingsList.
 */
export default function BookingDetails({ bookingId, onClose, onUpdate }) {
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    if (bookingId) {
      fetchBookingDetails();
    }
  }, [bookingId]);

  const fetchBookingDetails = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get(`/v1/bookings/${bookingId}`);
      setBooking(response.data);
      setError(null);
    } catch (err) {
      console.error("Failed to fetch booking details:", err);
      setError(err.response?.data?.message || "Failed to load booking details");
      setBooking(null);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async () => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) {
      return;
    }

    try {
      setActionLoading(true);
      setActionError(null);
      await apiClient.post(`/v1/bookings/${bookingId}/cancel`);
      await fetchBookingDetails();
      if (onUpdate) onUpdate();
    } catch (err) {
      console.error("Failed to cancel booking:", err);
      setActionError(err.response?.data?.message || "Failed to cancel booking");
    } finally {
      setActionLoading(false);
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

  const getDecisionColor = (decision) => {
    switch (decision) {
      case "APPROVED":
        return "bg-green-100 text-green-700 border-green-300";
      case "REJECTED":
        return "bg-red-100 text-red-700 border-red-300";
      case "PENDING":
        return "bg-yellow-100 text-yellow-700 border-yellow-300";
      default:
        return "bg-slate-100 text-slate-700 border-slate-300";
    }
  };

  const formatDateTime = (date) => {
    return new Date(date).toLocaleString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatDate = (date) => {
    return new Date(date).toLocaleDateString("en-US", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-8 text-center">
        <p className="text-slate-600">Loading booking details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-8">
        <div className="rounded-md bg-red-50 p-4">
          <p className="text-sm font-medium text-red-800">{error}</p>
        </div>
        <button
          onClick={onClose}
          className="mt-4 px-4 py-2 bg-slate-200 text-slate-700 rounded-md hover:bg-slate-300 transition-colors"
        >
          Close
        </button>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-8">
        <p className="text-slate-600">Booking not found</p>
      </div>
    );
  }

  const isApproved = booking.status === "APPROVED";
  const isPending = booking.status === "PENDING";
  const isTerminal = ["REJECTED", "CANCELLED"].includes(booking.status);
  const canCancel = (isApproved || isPending) && !isTerminal;

  return (
    <div className="bg-white rounded-lg shadow-lg p-6 max-h-[90vh] overflow-y-auto">
      {/* Header */}
      <div className="flex justify-between items-start mb-6 pb-4 border-b border-slate-200">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 mb-2">
            {booking.facility?.name || "Unknown Facility"}
          </h2>
          <div className="flex items-center gap-2">
            <span className={`px-3 py-1 rounded-full text-sm font-medium border ${getStatusColor(booking.status)}`}>
              {booking.status}
            </span>
            {booking.recurrenceRule && (
              <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded font-medium">
                Recurring
              </span>
            )}
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-2xl text-slate-400 hover:text-slate-600 transition-colors"
        >
          ✕
        </button>
      </div>

      {actionError && (
        <div className="rounded-md bg-red-50 p-4 mb-4">
          <p className="text-sm font-medium text-red-800">{actionError}</p>
        </div>
      )}

      <div className="space-y-6">
        {/* Main Details */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Date & Time */}
          <div>
            <h3 className="text-sm font-semibold text-slate-700 mb-2">📅 Date & Time</h3>
            <div className="bg-slate-50 rounded-lg p-3 space-y-1">
              <p className="text-sm font-medium text-slate-900">
                {formatDate(booking.bookingDate)}
              </p>
              <p className="text-sm text-slate-600">
                {booking.startTime} - {booking.endTime}
              </p>
              <p className="text-xs text-slate-500">Timezone: {booking.timezone}</p>
            </div>
          </div>

          {/* Facility Info */}
          <div>
            <h3 className="text-sm font-semibold text-slate-700 mb-2">🏢 Facility Info</h3>
            <div className="bg-slate-50 rounded-lg p-3 space-y-1 text-sm">
              <p className="text-slate-600">
                <span className="font-medium">Type:</span> {booking.facility?.type?.replace(/_/g, " ")}
              </p>
              <p className="text-slate-600">
                <span className="font-medium">Capacity:</span> {booking.facility?.capacity}
              </p>
              <p className="text-slate-600">
                <span className="font-medium">Location:</span> {booking.facility?.building} - Floor{" "}
                {booking.facility?.floor}
              </p>
            </div>
          </div>

          {/* Booking Details */}
          <div>
            <h3 className="text-sm font-semibold text-slate-700 mb-2">📝 Booking Details</h3>
            <div className="bg-slate-50 rounded-lg p-3 space-y-1 text-sm">
              <p className="text-slate-600">
                <span className="font-medium">Attendees:</span> {booking.attendees}
              </p>
              <p className="text-slate-600">
                <span className="font-medium">Purpose:</span> {booking.purpose}
              </p>
              <p className="text-slate-600">
                <span className="font-medium">Requested by:</span>{" "}
                {booking.requestedBy?.displayName || booking.requestedBy?.email}
              </p>
            </div>
          </div>

          {/* Users */}
          {booking.bookedFor?.id !== booking.requestedBy?.id && (
            <div>
              <h3 className="text-sm font-semibold text-slate-700 mb-2">👥 Booking For</h3>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 space-y-1 text-sm">
                <p className="text-blue-900 font-medium">
                  {booking.bookedFor?.displayName || booking.bookedFor?.email}
                </p>
                <p className="text-xs text-blue-700">
                  (Booked by: {booking.requestedBy?.displayName || booking.requestedBy?.email})
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Recurrence Info */}
        {booking.recurrenceRule && (
          <div>
            <h3 className="text-sm font-semibold text-slate-700 mb-2">🔄 Recurrence Rule</h3>
            <div className="bg-slate-50 rounded-lg p-3">
              <p className="text-sm font-mono text-slate-600">{booking.recurrenceRule}</p>
            </div>
          </div>
        )}

        {/* Approval Workflow */}
        {booking.approvalSteps && booking.approvalSteps.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-slate-700 mb-3">✅ Approval Workflow</h3>
            <div className="space-y-2">
              {booking.approvalSteps.map((step, index) => (
                <div
                  key={index}
                  className={`p-3 rounded-lg border ${getDecisionColor(step.decision)}`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-semibold">
                        Step {step.stepOrder}: {step.approverRole}
                      </p>
                      <p className="text-sm mt-1">{step.decision}</p>
                    </div>
                  </div>
                  {step.decidedBy && (
                    <div className="mt-2 text-xs opacity-75">
                      <p>
                        <span className="font-medium">Decided by:</span>{" "}
                        {step.decidedBy.displayName || step.decidedBy.email}
                      </p>
                      {step.decidedAt && (
                        <p>
                          <span className="font-medium">At:</span> {formatDateTime(step.decidedAt)}
                        </p>
                      )}
                      {step.note && <p><span className="font-medium">Note:</span> {step.note}</p>}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Timestamps */}
        <div className="bg-slate-50 rounded-lg p-3 space-y-1 text-xs text-slate-600">
          <p>
            <span className="font-medium">Created:</span> {formatDateTime(booking.createdAt)}
          </p>
          <p>
            <span className="font-medium">Last Updated:</span> {formatDateTime(booking.updatedAt)}
          </p>
          {booking.version && (
            <p>
              <span className="font-medium">Version:</span> {booking.version}
            </p>
          )}
        </div>

        {/* Actions */}
        <div className="flex gap-2 pt-4 border-t border-slate-200">
          {canCancel && (
            <button
              onClick={handleCancelBooking}
              disabled={actionLoading}
              className="flex-1 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:bg-red-400 disabled:cursor-not-allowed transition-colors font-medium"
            >
              {actionLoading ? "Cancelling..." : "Cancel Booking"}
            </button>
          )}

          {isApproved && (
            <button
              onClick={() => {
                /* TODO: Implement check-in */
              }}
              className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors font-medium"
            >
              Check In
            </button>
          )}

          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 bg-slate-200 text-slate-700 rounded-md hover:bg-slate-300 transition-colors font-medium"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
