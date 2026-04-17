import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * BookingDetails Component - Expanded View
 * Displays complete booking details including approval workflow and recurrence.
 * Opened as an expanded section from BookingsList card.
 * Avoids repeating information already shown in the card.
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

  if (loading) {
    return (
      <div className="p-6 text-center">
        <p className="text-slate-600">Loading booking details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg bg-red-50 p-4 border border-red-200">
          <p className="text-sm font-medium text-red-900">{error}</p>
        </div>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="p-6 text-center">
        <p className="text-slate-600">Booking not found</p>
      </div>
    );
  }

  const isPending = booking.status === "PENDING";
  const isApproved = booking.status === "APPROVED";
  const isTerminal = ["REJECTED", "CANCELLED"].includes(booking.status);
  const canCancel = (isApproved || isPending) && !isTerminal;

  return (
    <div className="p-6 space-y-6">
      {/* Error Message */}
      {actionError && (
        <div className="rounded-lg bg-red-50 border border-red-200 p-4">
          <p className="text-sm font-medium text-red-900">{actionError}</p>
        </div>
      )}

      {/* Additional Facility Details */}
      {booking.facility && (
        <div>
          <h3 className="text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wide">Facility Information</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 rounded-lg p-4">
            <div>
              <p className="text-xs text-slate-600 font-medium">Status</p>
              <p className="text-sm text-slate-900 font-medium mt-1">{booking.facility?.status || "Active"}</p>
            </div>
            <div>
              <p className="text-xs text-slate-600 font-medium">Capacity</p>
              <p className="text-sm text-slate-900 font-medium mt-1">{booking.facility?.capacity} people</p>
            </div>
            <div>
              <p className="text-xs text-slate-600 font-medium">Building</p>
              <p className="text-sm text-slate-900 font-medium mt-1">{booking.facility?.building}</p>
            </div>
            <div>
              <p className="text-xs text-slate-600 font-medium">Floor</p>
              <p className="text-sm text-slate-900 font-medium mt-1">{booking.facility?.floor}</p>
            </div>
          </div>
        </div>
      )}

      {/* Booking Metadata */}
      <div>
        <h3 className="text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wide">Booking Details</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 rounded-lg p-4">
          <div>
            <p className="text-xs text-slate-600 font-medium">Attendees</p>
            <p className="text-sm text-slate-900 font-medium mt-1">{booking.attendees}</p>
          </div>
          <div>
            <p className="text-xs text-slate-600 font-medium">Timezone</p>
            <p className="text-sm text-slate-900 font-medium mt-1">{booking.timezone}</p>
          </div>
          <div className="md:col-span-2">
            <p className="text-xs text-slate-600 font-medium">Purpose</p>
            <p className="text-sm text-slate-900 mt-1">{booking.purpose}</p>
          </div>
        </div>
      </div>

      {/* Booking For Information (if different from current user) */}
      {booking.bookedFor?.id !== booking.requestedBy?.id && (
        <div>
          <h3 className="text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wide">Booked For</h3>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm font-medium text-blue-900">
              {booking.bookedFor?.displayName || booking.bookedFor?.email}
            </p>
            <p className="text-xs text-blue-700 mt-1">
              Requested by: {booking.requestedBy?.displayName || booking.requestedBy?.email}
            </p>
          </div>
        </div>
      )}

      {/* Recurrence Information */}
      {booking.recurrenceRule && (
        <div>
          <h3 className="text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wide">Recurrence</h3>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-xs text-blue-600 font-medium">Recurrence Rule</p>
            <p className="text-sm font-mono text-blue-900 mt-1">{booking.recurrenceRule}</p>
            {booking.isRecurringMaster && (
              <p className="text-xs text-blue-700 mt-2">This is the master booking for a recurring series</p>
            )}
          </div>
        </div>
      )}

      {/* Approval Workflow */}
      {booking.approvalSteps && booking.approvalSteps.length > 0 && (
        <div>
          <h3 className="text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wide">Approval Workflow</h3>
          <div className="space-y-2">
            {booking.approvalSteps.map((step, index) => (
              <div key={index} className={`p-4 rounded-lg border-2 ${getDecisionColor(step.decision)}`}>
                <div className="flex justify-between items-start mb-2">
                  <div>
                    <p className="font-semibold text-sm">
                      Step {step.stepOrder}: {step.approverRole?.replace(/_/g, " ")}
                    </p>
                    <p className="text-xs font-medium mt-1">{step.decision}</p>
                  </div>
                </div>

                {step.decidedBy && (
                  <div className="mt-3 pt-3 border-t border-current border-opacity-20 text-xs space-y-1">
                    <p>
                      <span className="font-medium">Decided by:</span> {step.decidedBy.displayName || step.decidedBy.email}
                    </p>
                    {step.decidedAt && (
                      <p>
                        <span className="font-medium">Date:</span> {formatDateTime(step.decidedAt)}
                      </p>
                    )}
                    {step.note && (
                      <p className="mt-2">
                        <span className="font-medium">Note:</span> {step.note}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Audit Trail / Timestamps */}
      <div>
        <h3 className="text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wide">Audit Information</h3>
        <div className="bg-slate-50 rounded-lg p-4 text-xs space-y-2 text-slate-600">
          <div className="flex justify-between">
            <span className="font-medium">Created:</span>
            <span>{formatDateTime(booking.createdAt)}</span>
          </div>
          <div className="flex justify-between">
            <span className="font-medium">Last Updated:</span>
            <span>{formatDateTime(booking.updatedAt)}</span>
          </div>
          {booking.version && (
            <div className="flex justify-between pt-2 border-t border-slate-200">
              <span className="font-medium">Version:</span>
              <span className="font-mono">{booking.version}</span>
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-2 pt-4 border-t border-slate-200">
        {canCancel && (
          <button
            onClick={handleCancelBooking}
            disabled={actionLoading}
            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {actionLoading ? "Cancelling..." : "Cancel Booking"}
          </button>
        )}
      </div>
    </div>
  );
}
