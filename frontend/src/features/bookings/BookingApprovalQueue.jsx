import React, { useState, useEffect, useContext } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import QRCodePdfModal from "./QRCodePdfModal";
import { toast } from "react-toastify";
import { formatBookingDate } from "../../utils/bookingUtils";

/**
 * BookingApprovalQueue Component - Modern Senior-Level Design
 * Shows bookings pending approval for LECTURER, FACILITY_MANAGER, and ADMIN roles.
 * Clean, minimalist card design with professional information hierarchy.
 */
export default function BookingApprovalQueue() {
  const { user, hasRole } = useContext(AuthContext);
  const [pendingBookings, setPendingBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [approvalNotes, setApprovalNotes] = useState({});
  const [expandedId, setExpandedId] = useState(null);
  const [showQrModal, setShowQrModal] = useState(false);
  const [lastApprovedBooking, setLastApprovedBooking] = useState(null);

  // DEBUG: Log component mount and user info
  useEffect(() => {
    console.log("🔍 BookingApprovalQueue MOUNTED");
    console.log("👤 User:", user);
    console.log("📋 User email:", user?.email);
    console.log("🔐 User roles:", user?.roles);
    console.log(
      "✅ hasRole function available:",
      typeof hasRole === "function",
    );
  }, []);

  const canApprove =
    hasRole("LECTURER") || hasRole("FACILITY_MANAGER") || hasRole("ADMIN");

  // DEBUG: Log role checks
  useEffect(() => {
    console.log("🎭 Role Check Results:");
    console.log("  - hasRole('LECTURER'):", hasRole("LECTURER"));
    console.log(
      "  - hasRole('FACILITY_MANAGER'):",
      hasRole("FACILITY_MANAGER"),
    );
    console.log("  - hasRole('ADMIN'):", hasRole("ADMIN"));
    console.log("  - canApprove:", canApprove);
  }, [canApprove]);

  useEffect(() => {
    if (canApprove) {
      fetchPendingBookings();
    } else {
      setLoading(false);
    }
  }, [canApprove]);

  const fetchPendingBookings = async () => {
    try {
      console.log(
        "📡 Fetching pending approvals from /v1/bookings/pending-approvals",
      );
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/v1/bookings/pending-approvals");
      setPendingBookings(Array.isArray(response.data) ? response.data : []);
      console.log(
        "✅ State updated with",
        Array.isArray(response.data) ? response.data.length : 0,
        "bookings",
      );
    } catch (err) {
      console.error("❌ Failed to fetch pending approvals");
      console.error("Error object:", err);
      console.error("Response status:", err.response?.status);
      console.error("Response data:", err.response?.data);
      console.error("Error message:", err.message);
      setError(
        err.response?.data?.message || "Failed to load pending approvals",
      );
      setPendingBookings([]);
      toast.error("Failed to load pending approvals");
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (bookingId) => {
    try {
      setActionLoading(bookingId);
      setError(null);

      const note = (approvalNotes[bookingId] || "").trim();
      const params = new URLSearchParams();
      if (note.length > 0) {
        params.append("note", note);
      }

      const url = `/v1/bookings/${bookingId}/approve${params.toString() ? "?" + params.toString() : ""}`;
      await apiClient.post(url);
      toast.success("Booking approved successfully");

      // Find the approved booking to show in modal
      const approvedBooking = pendingBookings.find((b) => b.id === bookingId);
      if (approvedBooking) {
        setLastApprovedBooking(approvedBooking);
        setShowQrModal(true);
      }

      // Refresh list
      await fetchPendingBookings();
      setApprovalNotes((prev) => {
        const updated = { ...prev };
        delete updated[bookingId];
        return updated;
      });
    } catch (err) {
      const errorMsg =
        err.response?.data?.message || "Failed to approve booking";
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (bookingId) => {
    if (!window.confirm("Are you sure you want to reject this booking?")) {
      return;
    }

    try {
      setActionLoading(bookingId);
      setError(null);

      const note = (approvalNotes[bookingId] || "").trim();
      const params = new URLSearchParams();
      if (note.length > 0) {
        params.append("note", note);
      }

      const url = `/v1/bookings/${bookingId}/reject${params.toString() ? "?" + params.toString() : ""}`;
      await apiClient.post(url);
      toast.success("Booking rejected successfully");

      await fetchPendingBookings();
      setApprovalNotes((prev) => {
        const updated = { ...prev };
        delete updated[bookingId];
        return updated;
      });
    } catch (err) {
      const errorMsg =
        err.response?.data?.message || "Failed to reject booking";
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setActionLoading(null);
    }
  };

  if (!canApprove) {
    console.warn(
      "🚫 User lacks approval permissions - rendering no-permission message",
    );
    return (
      <div className="bg-white rounded-2xl p-8 border border-[#e2e8f0] shadow-sm text-center">
        <div className="w-16 h-16 bg-[#f8fafc] rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
        </div>
        <p className="text-[#0f172a] font-semibold text-lg">
          No Permission
        </p>
        <p className="text-[#64748b] mt-2">
          You do not have permission to approve bookings. Only LECTURER,
          FACILITY_MANAGER, and ADMIN roles can approve.
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-64">
        <div className="w-8 h-8 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mb-4"></div>
        <div className="text-[#64748b] font-medium">Loading pending approvals...</div>
      </div>
    );
  }

  console.log(
    "🎨 Rendering BookingApprovalQueue - pendingBookings:",
    pendingBookings,
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-2xl shadow-sm p-6 border border-[#e2e8f0]">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-[#0f172a] tracking-tight">
            Booking Approvals
          </h2>
          <button
            onClick={fetchPendingBookings}
            className="px-4 py-2 text-[#0f172a] bg-[#f1f5f9] hover:bg-[#e2e8f0] font-semibold rounded-xl transition-all duration-200 active:scale-95 shadow-sm text-sm"
          >
            Refresh List
          </button>
        </div>

        {error && (
          <div className="rounded-xl bg-[#fef2f2] p-4 mb-4 border border-[#fca5a5]">
            <p className="text-sm font-medium text-[#991b1b]">{error}</p>
          </div>
        )}

        <p className="text-sm text-[#64748b] font-medium flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#f59e0b]"></span>
          {pendingBookings.length} booking{pendingBookings.length !== 1 ? "s" : ""} awaiting your approval
        </p>
      </div>

      {/* No Pending Bookings */}
      {pendingBookings.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-[#e2e8f0] shadow-sm">
          <div className="w-16 h-16 bg-[#f0fdf4] rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-[#16a34a]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="text-[#0f172a] font-semibold text-lg">All caught up!</p>
          <p className="text-[#64748b] text-sm mt-1">
            No bookings are currently pending your approval.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {pendingBookings.map((booking) => (
            <div
              key={booking.id}
              className="rounded-2xl border border-[#e2e8f0] bg-white transition-all duration-200 hover:shadow-md overflow-hidden"
            >
              {/* Card Header - Summary Information */}
              <div className="p-6">
                <div className="flex items-start justify-between gap-4 mb-5">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-bold text-[#0f172a] truncate">
                      {booking.facility?.name || "Unknown Facility"}
                    </h3>
                    <div className="mt-2 flex items-center gap-4 flex-wrap text-sm">
                      <div className="text-[#475569] font-medium">
                        {formatBookingDate(booking.booking_date)}
                      </div>
                      <div className="text-[#475569]">
                        <span className="font-mono bg-[#f8fafc] border border-[#e2e8f0] px-2 py-1 rounded-md text-xs font-semibold">
                          {booking.start_time} – {booking.end_time}
                        </span>
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={() =>
                      setExpandedId(
                        expandedId === booking.id ? null : booking.id,
                      )
                    }
                    className="px-3 py-1.5 text-[#475569] bg-[#f8fafc] hover:bg-[#e2e8f0] border border-[#e2e8f0] rounded-lg font-semibold text-xs transition-colors flex items-center gap-1"
                  >
                    {expandedId === booking.id ? "Hide Details" : "View Details"}
                    <svg className={`w-3 h-3 transition-transform ${expandedId === booking.id ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                  </button>
                </div>

                {/* Quick Info Grid */}
                <div className="grid grid-cols-3 gap-4 pt-4 border-t border-[#e2e8f0]/60">
                  <div>
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider">
                      Facility
                    </p>
                    <p className="text-sm text-[#0f172a] font-semibold mt-1">
                      {booking.facility?.type?.replace(/_/g, " ") || "N/A"}
                    </p>
                    <p className="text-xs text-[#64748b] mt-0.5">
                      Cap: {booking.facility?.capacity || "N/A"}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider">
                      Location
                    </p>
                    <p className="text-sm text-[#0f172a] font-semibold mt-1">
                      {booking.facility?.building || "N/A"}
                    </p>
                    <p className="text-xs text-[#64748b] mt-0.5">
                      Floor {booking.facility?.floor || "N/A"}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider">
                      Requested By
                    </p>
                    <p className="text-sm text-[#0f172a] font-semibold mt-1 truncate">
                      {booking.requested_by?.displayName ||
                        booking.requested_by?.email ||
                        "Unknown"}
                    </p>
                    <p className="text-xs text-[#64748b] mt-0.5">
                      {booking.attendees} attendees
                    </p>
                  </div>
                </div>
              </div>

              {/* Expanded Details Section */}
              {expandedId === booking.id && (
                <div className="border-t border-[#e2e8f0] bg-[#f8fafc] p-6 space-y-5">
                  {booking.purpose && (
                    <div>
                      <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1.5">
                        Purpose
                      </p>
                      <p className="text-sm text-[#475569] leading-relaxed bg-white p-3 rounded-xl border border-[#e2e8f0]">
                        {booking.purpose}
                      </p>
                    </div>
                  )}

                  {booking.facility?.location && (
                    <div>
                      <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1.5">
                        Exact Location
                      </p>
                      <p className="text-sm text-[#475569]">
                        {booking.facility.location}
                      </p>
                    </div>
                  )}

                  {booking.approvalSteps &&
                    booking.approvalSteps.length > 0 && (
                      <div>
                        <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-2">
                          Approval Workflow
                        </p>
                        <div className="space-y-2">
                          {booking.approvalSteps.map((step, idx) => (
                            <div
                              key={idx}
                              className="flex items-center gap-3 p-3 bg-white rounded-xl border border-[#e2e8f0] shadow-sm text-sm"
                            >
                              <span className="flex items-center justify-center w-6 h-6 rounded-full bg-[#f1f5f9] text-[10px] font-bold text-[#475569] flex-shrink-0">
                                {step.stepOrder}
                              </span>
                              <span className="flex-1 font-semibold text-[#0f172a]">
                                {step.approverRole?.replace(/_/g, " ")}
                              </span>
                              <span
                                className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border flex-shrink-0 ${
                                  step.decision === "APPROVED"
                                    ? "bg-[#e8f5e9] text-[#1b5e20] border-[#c8e6c9]"
                                    : step.decision === "REJECTED"
                                      ? "bg-[#fef2f2] text-[#991b1b] border-[#fca5a5]"
                                      : "bg-[#fffbeb] text-[#b45309] border-[#fde68a]"
                                }`}
                              >
                                {step.decision}
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                </div>
              )}

              {/* Notes Input with Action Buttons */}
              <div className="border-t border-[#e2e8f0] p-6 bg-[#f8fafc]/50 flex flex-col sm:flex-row gap-6 items-start">
                {/* Notes Textarea - Left Side */}
                <div className="flex-1 w-full">
                  <label className="block text-xs font-bold text-[#64748b] uppercase tracking-wider mb-2">
                    Reviewer Note <span className="font-normal normal-case tracking-normal">(optional)</span>
                  </label>
                  <textarea
                    value={approvalNotes[booking.id] || ""}
                    onChange={(e) =>
                      setApprovalNotes((prev) => ({
                        ...prev,
                        [booking.id]: e.target.value,
                      }))
                    }
                    placeholder="Add comments for the requester..."
                    rows="2"
                    className="w-full px-4 py-3 bg-white border border-[#e2e8f0] rounded-xl focus:outline-none focus:ring-2 focus:ring-[#6366f1] focus:border-transparent text-sm resize-none shadow-sm transition-all"
                  />
                </div>

                {/* Action Buttons - Right Side */}
                <div className="flex flex-row sm:flex-col gap-3 w-full sm:w-auto sm:pt-6">
                  <button
                    onClick={() => handleApprove(booking.id)}
                    disabled={actionLoading === booking.id}
                    className="flex-1 sm:flex-none px-6 py-2.5 text-sm text-white bg-[#0f172a] hover:bg-[#1e293b] font-semibold rounded-xl transition-all shadow-sm active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                  >
                    {actionLoading === booking.id ? "Approving..." : "Approve Booking"}
                  </button>
                  <button
                    onClick={() => handleReject(booking.id)}
                    disabled={actionLoading === booking.id}
                    className="flex-1 sm:flex-none px-6 py-2.5 text-sm text-[#ef4444] bg-white border border-[#fca5a5] hover:bg-[#fef2f2] font-semibold rounded-xl transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                  >
                    {actionLoading === booking.id ? "Rejecting..." : "Reject"}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* QR Code PDF Modal */}
      <QRCodePdfModal
        booking={lastApprovedBooking}
        isOpen={showQrModal}
        onClose={() => setShowQrModal(false)}
      />
    </div>
  );
}
