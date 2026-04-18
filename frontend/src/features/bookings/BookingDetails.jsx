import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * BookingDetails Component - Expanded View
 * Displays complete booking details including approval workflow and recurrence.
 * Opened as an expanded section from BookingsList card.
 * Avoids repeating information already shown in the card.
 */
export default function BookingDetails({ bookingId, onClose, onUpdate, compactMode = false }) {
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
        return "bg-green-50 text-green-700 border-green-200";
      case "REJECTED":
        return "bg-red-50 text-red-700 border-red-200";
      case "PENDING":
        return "bg-orange-50 text-[#49BBBB] border-orange-200";
      default:
        return "bg-slate-50 text-slate-700 border-slate-100";
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
      <div className="p-6 flex items-center justify-center">
        <div className="text-slate-400 text-sm font-bold animate-pulse">Loading details...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-2xl bg-red-50 p-4 border border-red-100">
          <p className="text-sm font-bold text-red-600">{error}</p>
        </div>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="p-6 text-center">
        <p className="text-sm font-bold text-slate-400">Booking not found</p>
      </div>
    );
  }

  const isPending = booking.status === "PENDING";
  const isApproved = booking.status === "APPROVED";
  const isTerminal = ["REJECTED", "CANCELLED"].includes(booking.status);
  const canCancel = (isApproved || isPending) && !isTerminal;

  return (
    <div className={`${compactMode ? 'pt-2 pb-0 px-0' : 'p-6'} space-y-6`}>
      {/* Error Message */}
      {actionError && (
        <div className="rounded-2xl bg-red-50 border border-red-100 p-4">
          <p className="text-sm font-bold text-red-600">{actionError}</p>
        </div>
      )}

      {!compactMode && (
        <>
          {/* Additional Facility Details */}
          {booking.facility && (
            <div>
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Facility Info</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 bg-slate-50 rounded-2xl p-4 border border-slate-100">
                <div>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Status</p>
                  <p className="text-sm font-bold text-slate-800">{booking.facility?.status || "Active"}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Capacity</p>
                  <p className="text-sm font-bold text-slate-800">{booking.facility?.capacity} people</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Building</p>
                  <p className="text-sm font-bold text-slate-800">{booking.facility?.building}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Floor</p>
                  <p className="text-sm font-bold text-slate-800">{booking.facility?.floor}</p>
                </div>
              </div>
            </div>
          )}

          {/* Booking Metadata */}
          <div>
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Booking Info</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 rounded-2xl p-4 border border-slate-100">
              <div>
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Attendees</p>
                <p className="text-sm font-bold text-slate-800">{booking.attendees}</p>
              </div>
              <div>
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Timezone</p>
                <p className="text-sm font-bold text-slate-800">{booking.timezone}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Purpose</p>
                <p className="text-sm font-medium text-slate-700">{booking.purpose}</p>
              </div>
            </div>
          </div>
        </>
      )}

      {/* Booking For Information */}
      {booking.bookedFor?.id !== booking.requestedBy?.id && (
        <div>
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Delegation</h3>
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Booked For</p>
            <p className="text-sm font-bold text-white mb-3">
              {booking.bookedFor?.displayName || booking.bookedFor?.email}
            </p>
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Requested By</p>
            <p className="text-sm font-bold text-slate-300">
              {booking.requestedBy?.displayName || booking.requestedBy?.email}
            </p>
          </div>
        </div>
      )}

      {/* Recurrence Information */}
      {booking.recurrenceRule && (
        <div>
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Recurrence</h3>
          <div className="bg-[#49BBBB]/10 border border-[#49BBBB]/20 rounded-2xl p-4">
            <p className="text-[10px] font-bold text-[#49BBBB] uppercase tracking-wider mb-1">Rule</p>
            <p className="text-sm font-mono font-bold text-slate-900 mb-2">{booking.recurrenceRule}</p>
            {booking.isRecurringMaster && (
              <p className="text-xs font-bold text-slate-500">Master booking for series</p>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Approval Workflow */}
        {booking.approvalSteps && booking.approvalSteps.length > 0 && (
          <div>
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Approvals</h3>
            <div className="space-y-3">
              {booking.approvalSteps.map((step, index) => (
                <div key={index} className={`p-4 rounded-2xl border ${getDecisionColor(step.decision)}`}>
                  <div className="flex justify-between items-center mb-0.5">
                    <p className="text-[10px] font-bold uppercase tracking-wider opacity-80">
                      Step {step.stepOrder} • {step.approverRole?.replace(/_/g, " ")}
                    </p>
                    <p className="text-[10px] font-bold uppercase tracking-wider">{step.decision}</p>
                  </div>

                  {step.decidedBy && (
                    <div className="mt-2 pt-2 border-t border-current border-opacity-10 text-xs">
                      <p className="font-bold opacity-90 mb-0.5">
                        {step.decidedBy.displayName || step.decidedBy.email}
                      </p>
                      {step.decidedAt && (
                        <p className="font-semibold opacity-70">
                          {formatDateTime(step.decidedAt)}
                        </p>
                      )}
                      {step.note && (
                        <p className="mt-2 font-medium bg-white/50 p-2 rounded-lg">
                          "{step.note}"
                        </p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Audit Trail */}
        <div>
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Audit</h3>
          <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100 space-y-3">
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Created</p>
              <p className="text-xs font-bold text-slate-800">{formatDateTime(booking.createdAt)}</p>
            </div>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Updated</p>
              <p className="text-xs font-bold text-slate-800">{formatDateTime(booking.updatedAt)}</p>
            </div>
            {booking.version && (
              <div>
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Version</p>
                <p className="text-xs font-bold font-mono text-slate-800">{booking.version}</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      {!compactMode && canCancel && (
        <div className="pt-4 border-t border-slate-100 flex justify-end">
          <button
            onClick={handleCancelBooking}
            disabled={actionLoading}
            className="px-6 py-3 bg-white border border-slate-200 text-slate-600 hover:text-red-500 hover:bg-red-50 hover:border-red-200 rounded-xl font-bold text-sm transition-all shadow-[0_2px_8px_rgb(0,0,0,0.02)] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {actionLoading ? "Cancelling..." : "Cancel Booking"}
          </button>
        </div>
      )}
    </div>
  );
}
