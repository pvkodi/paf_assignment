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
      <div className="bg-white rounded-lg p-6 border border-slate-200">
        <p className="text-slate-600">
          You do not have permission to approve bookings. Only LECTURER,
          FACILITY_MANAGER, and ADMIN roles can approve.
        </p>
        <p className="text-sm text-slate-500 mt-2">
          DEBUG: Your roles: {user?.roles?.join(", ") || "None"}
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-slate-500">Loading pending approvals...</div>
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
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-slate-900">
            Booking Approvals
          </h2>
          <button
            onClick={fetchPendingBookings}
            className="px-4 py-2 text-slate-900 bg-slate-100 hover:bg-slate-200 font-semibold rounded-md transition-all duration-200 active:scale-95"
          >
            Refresh
          </button>
        </div>

        {error && (
          <div className="rounded-md bg-red-50 p-4 mb-4">
            <p className="text-sm font-medium text-red-800">{error}</p>
          </div>
        )}

        <p className="text-sm text-slate-600 font-medium">
          📋 {pendingBookings.length} booking
          {pendingBookings.length !== 1 ? "s" : ""} awaiting your approval
        </p>
      </div>

      {/* Error Message */}
      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 p-4">
          <p className="text-sm font-medium text-red-900">{error}</p>
        </div>
      )}

      {/* No Pending Bookings */}
      {pendingBookings.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-slate-400 mb-2">
            <svg
              className="w-12 h-12 mx-auto mb-2 opacity-50"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
          </div>
          <p className="text-slate-600 font-medium">All caught up</p>
          <p className="text-slate-500 text-sm mt-1">
            No bookings pending your approval
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {pendingBookings.map((booking) => (
            <div
              key={booking.id}
              className="rounded-lg border border-slate-200 bg-white transition-all duration-200 hover:border-slate-300 hover:shadow-md"
            >
              {/* Card Header - Summary Information */}
              <div className="p-6">
                <div className="flex items-start justify-between gap-4 mb-4">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-semibold text-slate-900 truncate">
                      {booking.facility?.name || "Unknown Facility"}
                    </h3>
                    <div className="mt-2 flex items-center gap-4 flex-wrap text-sm">
                      <div className="text-slate-600">
                        <span className="font-medium">
                          {formatBookingDate(booking.booking_date)}
                        </span>
                      </div>
                      <div className="text-slate-600">
                        <span className="font-mono bg-slate-100 px-2 py-1 rounded text-xs">
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
                    className="px-3 py-1 text-slate-600 hover:text-slate-900 font-semibold text-sm transition-colors"
                  >
                    {expandedId === booking.id ? "Less" : "More"}
                  </button>
                </div>

                {/* Quick Info Grid */}
                <div className="grid grid-cols-3 gap-4 pt-4 border-t border-slate-200">
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                      Facility
                    </p>
                    <p className="text-sm text-slate-900 font-medium mt-1">
                      {booking.facility?.type?.replace(/_/g, " ") || "N/A"}
                    </p>
                    <p className="text-xs text-slate-600 mt-1">
                      Cap: {booking.facility?.capacity || "N/A"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                      Location
                    </p>
                    <p className="text-sm text-slate-900 font-medium mt-1">
                      {booking.facility?.building || "N/A"}
                    </p>
                    <p className="text-xs text-slate-600 mt-1">
                      Floor {booking.facility?.floor || "N/A"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                      Requested By
                    </p>
                    <p className="text-sm text-slate-900 font-medium mt-1">
                      {booking.requested_by?.displayName ||
                        booking.requested_by?.email ||
                        "Unknown"}
                    </p>
                    <p className="text-xs text-slate-600 mt-1">
                      {booking.attendees} attendees
                    </p>
                  </div>
                </div>
              </div>

              {/* Expanded Details Section */}
              {expandedId === booking.id && (
                <div className="border-t border-slate-200 bg-slate-50 p-6 space-y-4">
                  {booking.purpose && (
                    <div>
                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                        Purpose
                      </p>
                      <p className="text-sm text-slate-700">
                        {booking.purpose}
                      </p>
                    </div>
                  )}

                  {booking.facility?.location && (
                    <div>
                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                        Exact Location
                      </p>
                      <p className="text-sm text-slate-700">
                        {booking.facility.location}
                      </p>
                    </div>
                  )}

                  {booking.approvalSteps &&
                    booking.approvalSteps.length > 0 && (
                      <div>
                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                          Approval Workflow
                        </p>
                        <div className="space-y-2">
                          {booking.approvalSteps.map((step, idx) => (
                            <div
                              key={idx}
                              className="flex items-center gap-3 p-3 bg-white rounded border border-slate-200 text-sm"
                            >
                              <span className="flex items-center justify-center w-6 h-6 rounded-full bg-slate-200 text-xs font-bold text-slate-700 flex-shrink-0">
                                {step.stepOrder}
                              </span>
                              <span className="flex-1 font-medium text-slate-900">
                                {step.approverRole}
                              </span>
                              <span
                                className={`px-2 py-1 rounded text-xs font-semibold flex-shrink-0 ${
                                  step.decision === "APPROVED"
                                    ? "bg-green-100 text-green-700"
                                    : step.decision === "REJECTED"
                                      ? "bg-red-100 text-red-700"
                                      : "bg-amber-100 text-amber-700"
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
              <div className="border-t border-slate-200 p-6 bg-white flex gap-6">
                {/* Notes Textarea - Left Side */}
                <div className="flex-1">
                  <label className="block text-sm font-semibold text-slate-900 mb-2">
                    Add a note (optional)
                  </label>
                  <textarea
                    value={approvalNotes[booking.id] || ""}
                    onChange={(e) =>
                      setApprovalNotes((prev) => ({
                        ...prev,
                        [booking.id]: e.target.value,
                      }))
                    }
                    placeholder="Add any comments for the requester..."
                    rows="2"
                    className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm resize-none"
                  />
                </div>

                {/* Action Buttons - Right Side, Stacked Vertically */}
                <div className="flex flex-col gap-2">
                  <button
                    onClick={() => handleApprove(booking.id)}
                    disabled={actionLoading === booking.id}
                    className="px-4 py-1 text-sm text-green-600 border-2 border-green-600 hover:bg-green-50 font-medium rounded-full transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                  >
                    {actionLoading === booking.id ? "Approving..." : "Approve"}
                  </button>
                  <button
                    onClick={() => handleReject(booking.id)}
                    disabled={actionLoading === booking.id}
                    className="px-4 py-1 text-sm text-slate-900 border-2 border-slate-900 hover:bg-slate-100 font-medium rounded-full transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
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
