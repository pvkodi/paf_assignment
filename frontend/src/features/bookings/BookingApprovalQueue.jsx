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
      <div className="bg-white rounded-3xl p-8 border border-slate-100 shadow-sm text-center">
        <div className="w-16 h-16 bg-red-50 text-red-400 flex items-center justify-center rounded-full mx-auto mb-4 border border-red-100">
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m0 0v2m0-2h2m-2 0H8m4-6V4M4 4h16v16H4z"/></svg>
        </div>
        <h2 className="text-xl font-bold text-slate-800 mb-2">Access Restricted</h2>
        <p className="text-slate-500 font-medium max-w-md mx-auto">
          You do not have permission to approve bookings. Only LECTURERS,
          FACILITY MANAGERS, and ADMINS can approve requests.
        </p>
        <p className="text-xs text-slate-400 mt-6 font-mono px-4 py-2 bg-slate-50 rounded-lg inline-block border border-slate-100">
          ROLE: {user?.roles?.join(", ") || "None"}
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 bg-[#f8f9fa] rounded-3xl">
        <div className="text-slate-400 text-sm font-bold animate-pulse">Loading queue...</div>
      </div>
    );
  }

  console.log(
    "🎨 Rendering BookingApprovalQueue - pendingBookings:",
    pendingBookings,
  );

  return (
    <div className="space-y-8 bg-[#f8f9fa] min-h-screen p-6 md:p-8 rounded-3xl">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-2">
        <div>
          <h1 className="text-4xl font-bold tracking-tight text-slate-900">
            Booking Approvals
          </h1>
          <p className="text-slate-500 mt-2 text-sm font-medium">
            📋 {pendingBookings.length} booking{pendingBookings.length !== 1 ? "s" : ""} awaiting your response
          </p>
        </div>
        <button
          onClick={fetchPendingBookings}
          className="px-5 py-2.5 text-sm font-bold text-slate-600 bg-white border border-slate-200 hover:text-slate-900 hover:bg-slate-50 shadow-[0_2px_10px_rgb(0,0,0,0.02)] rounded-full transition-all duration-300"
        >
          Refresh Queue
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="rounded-2xl bg-red-50/50 border border-red-100 p-5 backdrop-blur-sm">
          <p className="text-sm font-bold text-red-800">{error}</p>
        </div>
      )}

      {/* No Pending Bookings */}
      {pendingBookings.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-3xl border border-slate-100 shadow-sm">
          <div className="w-20 h-20 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-green-100">
            <svg
              className="w-8 h-8 text-green-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p className="text-slate-800 font-bold text-lg mb-1">Queue is Clear</p>
          <p className="text-slate-500 text-sm font-medium">
            You have no pending bookings to review.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {pendingBookings.map((booking) => (
            <div
              key={booking.id}
              className="group bg-white rounded-[1.5rem] border border-slate-100 p-6 shadow-[0_4px_24px_rgb(0,0,0,0.02)] hover:shadow-[0_8px_32px_rgb(0,0,0,0.06)] transition-all duration-300 transform hover:-translate-y-1 overflow-hidden"
            >
              <div className="flex flex-col md:flex-row gap-6">
                {/* Left Side Info Blocks */}
                <div className="flex-1">
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-orange-50 text-[#49BBBB] border-orange-200 mb-3">
                        <span className="animate-pulse w-1.5 h-1.5 rounded-full bg-[#49BBBB]"></span>
                        Action Required
                      </div>
                      <h3 className="text-2xl font-bold text-slate-900 group-hover:text-slate-800 transition-colors">
                        {booking.facility?.name || "Unknown Facility"}
                      </h3>
                      <p className="text-slate-500 text-sm font-medium flex items-center gap-2 mt-1">
                        <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                        {booking.facility?.building || "N/A"}, Fl {booking.facility?.floor || "-"}
                      </p>
                    </div>
                  </div>

                  {/* Time & Date Block */}
                  <div className="bg-slate-50 rounded-2xl p-4 mb-4 border border-slate-100">
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Date</p>
                        <p className="text-sm font-bold text-slate-800">{formatBookingDate(booking.booking_date)}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Time</p>
                        <p className="text-sm font-bold text-slate-800">{booking.start_time} - {booking.end_time}</p>
                      </div>
                    </div>
                  </div>

                  {/* Purpose & Attendees */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-white border border-slate-100 rounded-xl p-3 shadow-sm">
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Requested By</p>
                      <p className="text-xs font-bold text-slate-800 truncate">
                        {booking.requested_by?.displayName || booking.requested_by?.email || "Unknown"}
                      </p>
                    </div>
                    <div className="bg-white border border-slate-100 rounded-xl p-3 shadow-sm flex items-center justify-between">
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Attendees</p>
                      <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center text-xs font-bold text-slate-700">
                        {booking.attendees}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Right Side Review Actions */}
                <div className="w-full md:w-72 bg-slate-50 rounded-2xl p-5 border border-slate-100 flex flex-col">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-2">
                      Reviewer Note (Optional)
                    </label>
                    <textarea
                      value={approvalNotes[booking.id] || ""}
                      onChange={(e) =>
                        setApprovalNotes((prev) => ({
                          ...prev,
                          [booking.id]: e.target.value,
                        }))
                      }
                      placeholder="Add an optional comment..."
                      rows="3"
                      className="w-full px-4 py-3 bg-white border border-slate-200 rounded-xl font-medium text-slate-800 transition-all focus:outline-none focus:ring-4 focus:ring-slate-100 text-sm resize-none mb-4"
                    />
                  </div>

                  <div className="mt-auto flex flex-col gap-3">
                    <button
                      onClick={() => handleApprove(booking.id)}
                      disabled={actionLoading === booking.id}
                      className="w-full py-3 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {actionLoading === booking.id ? "Approving..." : "Approve Request"}
                    </button>
                    <button
                      onClick={() => handleReject(booking.id)}
                      disabled={actionLoading === booking.id}
                      className="w-full py-3 bg-white hover:bg-red-50 border border-slate-200 hover:border-red-200 text-slate-600 hover:text-red-500 font-bold rounded-xl shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {actionLoading === booking.id ? "Wait..." : "Reject"}
                    </button>
                  </div>
                </div>
              </div>

              {/* Toggle Extra Info */}
              <div className="mt-4 pt-4 border-t border-slate-100 relative">
                <button
                  onClick={() => setExpandedId(expandedId === booking.id ? null : booking.id)}
                  className="mx-auto block text-xs font-bold text-slate-400 hover:text-slate-700 uppercase tracking-wider transition-colors"
                >
                  {expandedId === booking.id ? "- Hide Details -" : "+ Show Details +"}
                </button>
                
                <div className={`mt-4 overflow-hidden transition-all duration-300 origin-top transform ${expandedId === booking.id ? 'opacity-100 scale-y-100 h-auto' : 'opacity-0 scale-y-0 h-0'}`}>
                  {booking.purpose && (
                    <div className="bg-slate-50 rounded-xl p-4 mb-4 border border-slate-100">
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Booking Purpose</p>
                      <p className="text-sm font-medium text-slate-700 leading-relaxed">{booking.purpose}</p>
                    </div>
                  )}

                  {booking.approvalSteps && booking.approvalSteps.length > 0 && (
                    <div>
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Previous Approvals</p>
                      <div className="space-y-2">
                        {booking.approvalSteps.map((step, idx) => (
                          <div key={idx} className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100 text-sm">
                            <span className="flex items-center justify-center w-6 h-6 rounded-full bg-slate-200 text-[10px] font-bold text-slate-700 flex-shrink-0">
                              {step.stepOrder}
                            </span>
                            <span className="flex-1 font-bold text-slate-800 text-xs uppercase tracking-wide">
                              {step.approverRole?.replace(/_/g, " ")}
                            </span>
                            <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase tracking-widest flex-shrink-0 ${
                              step.decision === "APPROVED" ? "bg-green-100 text-green-700" :
                              step.decision === "REJECTED" ? "bg-red-100 text-red-700" :
                              "bg-orange-100 text-[#49BBBB]"
                            }`}>
                              {step.decision}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
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
