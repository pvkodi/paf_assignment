import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * BookingDetails Component - Modern Expanded View
 * Displays complete booking details including approval workflow and recurrence.
 * Styled to fit perfectly within the expanded row of the main BookingsList table.
 */
export default function BookingDetails({ bookingId, onClose, onUpdate }) {
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  const getDecisionColor = (decision) => {
    switch (decision) {
      case "APPROVED":
        return "bg-[#e8f5e9] text-[#1b5e20] border-[#c8e6c9]";
      case "REJECTED":
        return "bg-[#fef2f2] text-[#991b1b] border-[#fca5a5]";
      case "PENDING":
        return "bg-[#fffbeb] text-[#b45309] border-[#fde68a]";
      default:
        return "bg-[#f8fafc] text-[#475569] border-[#e2e8f0]";
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
      <div className="py-4 text-center animate-pulse flex flex-col items-center">
        <div className="w-8 h-8 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mb-2"></div>
        <p className="text-sm font-medium text-[#64748b]">Loading booking details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl bg-[#fef2f2] p-4 border border-[#fca5a5] flex items-center gap-3">
        <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        <p className="text-sm font-medium text-[#991b1b]">{error}</p>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="py-4 text-center">
        <p className="text-sm font-medium text-[#64748b]">Booking not found</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column */}
        <div className="space-y-6">
          {/* Facility Details */}
          {booking.facility && (
            <div>
              <h3 className="text-xs font-bold text-[#64748b] mb-3 uppercase tracking-wider flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                Facility Information
              </h3>
              <div className="grid grid-cols-2 gap-4 bg-white rounded-xl p-4 border border-[#e2e8f0] shadow-sm">
                <div>
                  <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Status</p>
                  <p className="text-sm text-[#0f172a] font-medium mt-1">{booking.facility?.status || "Active"}</p>
                </div>
                <div>
                  <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Capacity</p>
                  <p className="text-sm text-[#0f172a] font-medium mt-1">{booking.facility?.capacity} people</p>
                </div>
                <div>
                  <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Building</p>
                  <p className="text-sm text-[#0f172a] font-medium mt-1">{booking.facility?.building}</p>
                </div>
                <div>
                  <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Floor</p>
                  <p className="text-sm text-[#0f172a] font-medium mt-1">{booking.facility?.floor}</p>
                </div>
              </div>
            </div>
          )}

          {/* Booking Metadata */}
          <div>
            <h3 className="text-xs font-bold text-[#64748b] mb-3 uppercase tracking-wider flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              Booking Details
            </h3>
            <div className="grid grid-cols-2 gap-4 bg-white rounded-xl p-4 border border-[#e2e8f0] shadow-sm">
              <div>
                <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Attendees</p>
                <p className="text-sm text-[#0f172a] font-medium mt-1">{booking.attendees}</p>
              </div>
              <div>
                <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Timezone</p>
                <p className="text-sm text-[#0f172a] font-medium mt-1">{booking.timezone}</p>
              </div>
              <div className="col-span-2 pt-2 border-t border-[#e2e8f0]/50">
                <p className="text-xs text-[#94a3b8] font-semibold uppercase tracking-wide">Full Purpose</p>
                <p className="text-sm text-[#475569] mt-1.5 leading-relaxed">{booking.purpose}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-6">
          {/* Approval Workflow */}
          {booking.approvalSteps && booking.approvalSteps.length > 0 && (
            <div>
              <h3 className="text-xs font-bold text-[#64748b] mb-3 uppercase tracking-wider flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                Approval Workflow
              </h3>
              <div className="space-y-3">
                {booking.approvalSteps.map((step, index) => (
                  <div key={index} className={`p-4 rounded-xl border ${getDecisionColor(step.decision)}`}>
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <p className="font-semibold text-sm">
                          Step {step.stepOrder}: {step.approverRole?.replace(/_/g, " ")}
                        </p>
                        <p className="text-xs font-bold uppercase tracking-wider mt-1 opacity-80">{step.decision}</p>
                      </div>
                    </div>

                    {step.decidedBy && (
                      <div className="mt-3 pt-3 border-t border-current opacity-90 text-xs space-y-1.5">
                        <p><span className="font-semibold">Decided by:</span> {step.decidedBy.displayName || step.decidedBy.email}</p>
                        {step.decidedAt && <p><span className="font-semibold">Date:</span> {formatDateTime(step.decidedAt)}</p>}
                        {step.note && <p className="mt-2 bg-black/5 p-2 rounded"><span className="font-semibold block mb-0.5">Note:</span> {step.note}</p>}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Special Cases: Booked For & Recurrence */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {booking.bookedFor?.id !== booking.requestedBy?.id && (
              <div>
                <h3 className="text-xs font-bold text-[#64748b] mb-2 uppercase tracking-wider">Booked For</h3>
                <div className="bg-[#f0fdfa] border border-[#ccfbf1] rounded-xl p-4">
                  <p className="text-sm font-semibold text-[#0f766e]">
                    {booking.bookedFor?.displayName || booking.bookedFor?.email}
                  </p>
                  <p className="text-xs text-[#0d9488] mt-1 border-t border-[#99f6e4] pt-1 mt-2">
                    Requested by: {booking.requestedBy?.displayName || booking.requestedBy?.email}
                  </p>
                </div>
              </div>
            )}

            {booking.recurrenceRule && (
              <div>
                <h3 className="text-xs font-bold text-[#64748b] mb-2 uppercase tracking-wider">Recurrence</h3>
                <div className="bg-[#eff6ff] border border-[#dbeafe] rounded-xl p-4">
                  <p className="text-xs text-[#2563eb] font-semibold uppercase tracking-wide">Rule</p>
                  <p className="text-sm font-mono text-[#1e40af] mt-1 break-all">{booking.recurrenceRule}</p>
                  {booking.isRecurringMaster && (
                    <p className="text-[10px] text-[#3b82f6] mt-2 font-medium uppercase tracking-wider bg-[#dbeafe] inline-block px-2 py-0.5 rounded">Master Booking</p>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Audit Trail */}
          <div>
            <h3 className="text-xs font-bold text-[#64748b] mb-3 uppercase tracking-wider flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              Audit Log
            </h3>
            <div className="bg-white rounded-xl p-4 border border-[#e2e8f0] text-xs space-y-2 text-[#64748b] shadow-sm">
              <div className="flex justify-between items-center">
                <span className="font-semibold uppercase tracking-wider">Created</span>
                <span className="font-medium text-[#0f172a]">{formatDateTime(booking.createdAt)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-semibold uppercase tracking-wider">Last Updated</span>
                <span className="font-medium text-[#0f172a]">{formatDateTime(booking.updatedAt)}</span>
              </div>
              {booking.version && (
                <div className="flex justify-between items-center pt-2 border-t border-[#e2e8f0]">
                  <span className="font-semibold uppercase tracking-wider">Version</span>
                  <span className="font-mono bg-[#f1f5f9] px-2 py-0.5 rounded text-[#0f172a]">{booking.version}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
