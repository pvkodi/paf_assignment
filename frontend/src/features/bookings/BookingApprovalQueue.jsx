import React, { useState, useEffect, useContext } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";

/**
 * BookingApprovalQueue Component
 * Shows bookings pending approval for LECTURER, FACILITY_MANAGER, and ADMIN roles.
 */
export default function BookingApprovalQueue() {
  const { user, hasRole } = useContext(AuthContext);
  const [pendingBookings, setPendingBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [approvalNotes, setApprovalNotes] = useState({});
  const [expandedId, setExpandedId] = useState(null);

  // DEBUG: Log component mount and user info
  useEffect(() => {
    console.log("🔍 BookingApprovalQueue MOUNTED");
    console.log("👤 User:", user);
    console.log("📋 User email:", user?.email);
    console.log("🔐 User roles:", user?.roles);
    console.log("✅ hasRole function available:", typeof hasRole === 'function');
  }, []);

  const canApprove = hasRole("LECTURER") || hasRole("FACILITY_MANAGER") || hasRole("ADMIN");
  
  // DEBUG: Log role checks
  useEffect(() => {
    console.log("🎭 Role Check Results:");
    console.log("  - hasRole('LECTURER'):", hasRole("LECTURER"));
    console.log("  - hasRole('FACILITY_MANAGER'):", hasRole("FACILITY_MANAGER"));
    console.log("  - hasRole('ADMIN'):", hasRole("ADMIN"));
    console.log("  - canApprove:", canApprove);
  }, [canApprove]);

  useEffect(() => {
    console.log("⚡ useEffect triggered - canApprove:", canApprove);
    if (canApprove) {
      console.log("✅ User can approve - fetching pending bookings...");
      fetchPendingBookings();
    } else {
      console.log("❌ User cannot approve - setting loading to false");
      setLoading(false);
    }
  }, [canApprove]);

  const fetchPendingBookings = async () => {
    try {
      console.log("📡 Fetching pending approvals from /v1/bookings/pending-approvals");
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/v1/bookings/pending-approvals");
      console.log("✅ API Response received:", response);
      console.log("📊 Data:", response.data);
      console.log("📦 Is array?", Array.isArray(response.data));
      setPendingBookings(Array.isArray(response.data) ? response.data : []);
      console.log("✅ State updated with", Array.isArray(response.data) ? response.data.length : 0, "bookings");
    } catch (err) {
      console.error("❌ Failed to fetch pending approvals");
      console.error("Error object:", err);
      console.error("Response status:", err.response?.status);
      console.error("Response data:", err.response?.data);
      console.error("Error message:", err.message);
      setError(err.response?.data?.message || "Failed to load pending approvals");
      setPendingBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (bookingId) => {
    console.log("✅ APPROVE clicked for booking:", bookingId);
    try {
      setActionLoading(bookingId);
      setError(null);

      const note = (approvalNotes[bookingId] || "").trim();
      console.log("📝 Note:", note);
      const params = new URLSearchParams();
      if (note.length > 0) {
        params.append("note", note);
      }
      
      const url = `/v1/bookings/${bookingId}/approve${params.toString() ? "?" + params.toString() : ""}`;
      console.log("📡 POST request to:", url);
      await apiClient.post(url);
      console.log("✅ Approve successful");

      // Refresh list
      await fetchPendingBookings();
    } catch (err) {
      console.error("❌ Failed to approve booking:", err);
      console.error("Response:", err.response?.data);
      setError(err.response?.data?.message || "Failed to approve booking");
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (bookingId) => {
    console.log("❌ REJECT clicked for booking:", bookingId);
    if (!window.confirm("Are you sure you want to reject this booking?")) {
      console.log("❌ Reject cancelled by user");
      return;
    }

    try {
      setActionLoading(bookingId);
      setError(null);

      const note = (approvalNotes[bookingId] || "").trim();
      console.log("📝 Rejection note:", note);
      const params = new URLSearchParams();
      if (note.length > 0) {
        params.append("note", note);
      }
      
      const url = `/v1/bookings/${bookingId}/reject${params.toString() ? "?" + params.toString() : ""}`;
      console.log("📡 POST request to:", url);
      await apiClient.post(url);
      console.log("✅ Reject successful");

      // Refresh list
      await fetchPendingBookings();
    } catch (err) {
      console.error("❌ Failed to reject booking:", err);
      console.error("Response:", err.response?.data);
      setError(err.response?.data?.message || "Failed to reject booking");
    } finally {
      setActionLoading(null);
    }
  };

  const formatDate = (date) => {
    return new Date(date).toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  if (!canApprove) {
    console.warn("🚫 User lacks approval permissions - rendering no-permission message");
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <p className="text-slate-600">
          You do not have permission to approve bookings. Only LECTURER, FACILITY_MANAGER, and
          ADMIN roles can approve.
        </p>
        <p className="text-sm text-slate-500 mt-2">
          DEBUG: Your roles: {user?.roles?.join(", ") || "None"}
        </p>
      </div>
    );
  }

  if (loading) {
    console.log("⏳ Still loading...");
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <p className="text-slate-600">Loading pending approvals...</p>
      </div>
    );
  }

  console.log("🎨 Rendering BookingApprovalQueue - pendingBookings:", pendingBookings);

  return (
    <div className="space-y-6">
      {/* DEBUG INFO */}
      <div className="bg-blue-50 rounded-lg shadow-md p-4 border border-blue-200">
        <h3 className="font-semibold text-blue-900 mb-2">🔍 Debug Info</h3>
        <div className="text-xs text-blue-800 space-y-1 font-mono bg-white p-2 rounded border border-blue-100">
          <div>✅ canApprove: {String(canApprove)}</div>
          <div>👤 User email: {user?.email || "NOT SET"}</div>
          <div>🔐 User roles: {user?.roles?.join(", ") || "NONE"}</div>
          <div>📋 Pending bookings: {pendingBookings.length}</div>
          <div>⏳ Loading: {String(loading)}</div>
          <div>❌ Error: {error || "NONE"}</div>
        </div>
        <p className="text-xs text-blue-600 mt-2">Check browser console (F12) for detailed logs</p>
      </div>

      {/* Header */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-slate-900">Booking Approvals</h2>
          <button
            onClick={fetchPendingBookings}
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

        <p className="text-sm text-slate-600 font-medium">
          📋 {pendingBookings.length} booking{pendingBookings.length !== 1 ? "s" : ""} awaiting your
          approval
        </p>
      </div>

      {/* Pending Bookings */}
      {pendingBookings.length === 0 ? (
        <div className="bg-white rounded-lg shadow-md p-6 text-center">
          <p className="text-lg text-slate-600">✅ All caught up!</p>
          <p className="text-sm text-slate-500 mt-1">No bookings pending your approval</p>
        </div>
      ) : (
        <div className="space-y-4">
          {pendingBookings.map((booking) => (
            <div
              key={booking.id}
              className="bg-white rounded-lg shadow-md p-6 border-l-4 border-yellow-400"
            >
              {/* Summary */}
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-slate-900">
                    {booking.facility?.name || "Unknown Facility"}
                  </h3>
                  <p className="text-sm text-slate-600 mt-1">
                    📅 {formatDate(booking.bookingDate)}
                  </p>
                  <p className="text-sm text-slate-600">
                    👤 Requested by: {booking.requestedBy?.displayName || booking.requestedBy?.email}
                  </p>
                </div>
                <button
                  onClick={() => setExpandedId(expandedId === booking.id ? null : booking.id)}
                  className="px-3 py-1 bg-slate-100 text-slate-700 rounded-md hover:bg-slate-200 transition-colors text-sm font-medium"
                >
                  {expandedId === booking.id ? "Hide" : "Expand"}
                </button>
              </div>

              {/* Booking Details */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm mb-4 pb-4 border-b border-slate-200">
                <div>
                  <span className="text-slate-600 font-medium">Time</span>
                  <p className="text-slate-900">
                    {booking.startTime} - {booking.endTime}
                  </p>
                </div>
                <div>
                  <span className="text-slate-600 font-medium">Attendees</span>
                  <p className="text-slate-900">
                    {booking.attendees} / {booking.facility?.capacity}
                  </p>
                </div>
                <div>
                  <span className="text-slate-600 font-medium">Purpose</span>
                  <p className="text-slate-900 truncate">{booking.purpose}</p>
                </div>
                <div>
                  <span className="text-slate-600 font-medium">Status</span>
                  <p className="text-slate-900">
                    <span className="inline-block px-2 py-1 bg-yellow-100 text-yellow-700 rounded text-xs font-medium">
                      {booking.status}
                    </span>
                  </p>
                </div>
              </div>

              {/* Expanded Section */}
              {expandedId === booking.id && (
                <div className="bg-slate-50 rounded-lg p-4 mb-4 space-y-3 border border-slate-200">
                  {/* Facility Info */}
                  <div>
                    <p className="text-sm font-semibold text-slate-700 mb-1">Facility Details</p>
                    <div className="text-xs text-slate-600 space-y-1">
                      <p>
                        <span className="font-medium">Type:</span>{" "}
                        {booking.facility?.type?.replace(/_/g, " ")}
                      </p>
                      <p>
                        <span className="font-medium">Location:</span> {booking.facility?.building} -
                        Floor {booking.facility?.floor}
                      </p>
                      <p>
                        <span className="font-medium">Status:</span>{" "}
                        {booking.facility?.status || "ACTIVE"}
                      </p>
                    </div>
                  </div>

                  {/* Approval Workflow */}
                  {booking.approvalSteps && booking.approvalSteps.length > 0 && (
                    <div>
                      <p className="text-sm font-semibold text-slate-700 mb-1">Approval Steps</p>
                      <div className="space-y-1 text-xs">
                        {booking.approvalSteps.map((step, idx) => (
                          <div
                            key={idx}
                            className="flex items-center gap-2 p-2 bg-white rounded border border-slate-200"
                          >
                            <span className="w-6 h-6 flex items-center justify-center rounded-full bg-slate-200 text-xs font-bold">
                              {step.stepOrder}
                            </span>
                            <span className="flex-1 font-medium">{step.approverRole}</span>
                            <span
                              className={`px-2 py-1 rounded text-xs font-medium ${
                                step.decision === "APPROVED"
                                  ? "bg-green-100 text-green-700"
                                  : step.decision === "REJECTED"
                                    ? "bg-red-100 text-red-700"
                                    : "bg-yellow-100 text-yellow-700"
                              }`}
                            >
                              {step.decision}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Booking Purpose */}
                  {booking.purpose && (
                    <div>
                      <p className="text-sm font-semibold text-slate-700 mb-1">Purpose</p>
                      <p className="text-xs text-slate-600">{booking.purpose}</p>
                    </div>
                  )}
                </div>
              )}

              {/* Approval Note Input */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Approval Note (Optional)
                </label>
                <textarea
                  value={approvalNotes[booking.id] || ""}
                  onChange={(e) =>
                    setApprovalNotes((prev) => ({
                      ...prev,
                      [booking.id]: e.target.value,
                    }))
                  }
                  placeholder="Add any comments or notes for the requester..."
                  rows="2"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm"
                />
              </div>

              {/* Action Buttons */}
              <div className="flex gap-2">
                <button
                  onClick={() => handleApprove(booking.id)}
                  disabled={actionLoading === booking.id}
                  className="flex-1 px-4 py-2 bg-green-600 text-white font-medium rounded-md hover:bg-green-700 disabled:bg-green-400 disabled:cursor-not-allowed transition-colors"
                >
                  {actionLoading === booking.id ? "Processing..." : "✅ Approve"}
                </button>
                <button
                  onClick={() => handleReject(booking.id)}
                  disabled={actionLoading === booking.id}
                  className="flex-1 px-4 py-2 bg-red-600 text-white font-medium rounded-md hover:bg-red-700 disabled:bg-red-400 disabled:cursor-not-allowed transition-colors"
                >
                  {actionLoading === booking.id ? "Processing..." : "❌ Reject"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
