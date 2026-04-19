import React, { useState } from "react";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";

/**
 * Ticket Status Update Component
 * Allows technicians to update ticket status with state machine validation
 */

export function TicketStatusUpdate({ ticketId, currentStatus, onStatusUpdated }) {
  const { user } = useAuth();
  const [newStatus, setNewStatus] = useState(currentStatus);
  const [rejectReason, setRejectReason] = useState("");
  const [workNote, setWorkNote] = useState("");
  const [solutionSummary, setSolutionSummary] = useState("");
  const [showRejectOption, setShowRejectOption] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const isAdmin = user?.roles?.includes("ADMIN");

  const getNextStatuses = (status) => {
    const strictTransitions = {
      OPEN: ["IN_PROGRESS"],
      IN_PROGRESS: ["RESOLVED"],
      RESOLVED: ["CLOSED"],
      CLOSED: [],
      REJECTED: [],
    };

    const allForwardStates = {
      OPEN: ["IN_PROGRESS", "RESOLVED", "CLOSED"],
      IN_PROGRESS: ["RESOLVED", "CLOSED"],
      RESOLVED: ["CLOSED"],
      CLOSED: [],
      REJECTED: [],
    };

    return isAdmin ? allForwardStates[status] || [] : strictTransitions[status] || [];
  };

  const allowedNextStatuses = getNextStatuses(currentStatus);
  const canReject = isAdmin && currentStatus !== "CLOSED" && currentStatus !== "REJECTED";

  const handleStatusChange = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    if (newStatus === currentStatus && !showRejectOption) {
      setError("Please select a different status");
      return;
    }

    if (!showRejectOption && !allowedNextStatuses.includes(newStatus)) {
      setError("This status transition is not allowed");
      return;
    }

    if (showRejectOption) {
      if (!rejectReason.trim()) { setError("Please provide a reason for rejection"); return; }
      if (rejectReason.length < 10) { setError("Rejection reason must be at least 10 characters"); return; }
    }

    if (newStatus === "RESOLVED" && !showRejectOption) {
      if (!solutionSummary.trim()) { setError("Please provide a solution summary for the student"); return; }
      if (solutionSummary.length < 20) { setError("Solution summary must be at least 20 characters"); return; }
      if (solutionSummary.length > 2000) { setError("Solution summary must not exceed 2000 characters"); return; }
    }

    try {
      setLoading(true);
      const payload = { status: newStatus };
      if (showRejectOption) {
        payload.rejectionReason = rejectReason;
      }
      
      await apiClient.put(`/tickets/${ticketId}/status`, payload);

      try {
        if (newStatus === "RESOLVED" && !showRejectOption) {
          await apiClient.post(`/tickets/${ticketId}/comments`, {
            content: `**SOLUTION:** ${solutionSummary}`,
            visibility: "PUBLIC",
          });
        }

        if (workNote.trim() && !showRejectOption) {
          await apiClient.post(`/tickets/${ticketId}/comments`, {
            content: `Status updated to ${newStatus}: ${workNote}`,
            visibility: "PUBLIC",
          });
        }
      } catch (commentErr) {
        console.error("Failed to create status change comment:", commentErr);
      }

      setSuccess(true);
      setShowRejectOption(false);
      setRejectReason("");
      setWorkNote("");
      setSolutionSummary("");
      setTimeout(() => {
        setSuccess(false);
        onStatusUpdated();
      }, 2000);
    } catch (err) {
      console.error("Failed to update status:", err);
      setError(err.response?.data?.message || "Failed to update status. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      OPEN: "bg-[#f1f5f9] text-[#475569] border-[#e2e8f0]",
      IN_PROGRESS: "bg-[#e0e7ff] text-[#4338ca] border-[#c7d2fe]",
      RESOLVED: "bg-[#dcfce7] text-[#15803d] border-[#bbf7d0]",
      CLOSED: "bg-[#f1f5f9] text-[#475569] border-[#e2e8f0]",
      REJECTED: "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]",
    };
    return colors[status] || "bg-[#f8fafc] text-[#64748b] border-[#e2e8f0]";
  };

  return (
    <div className="space-y-4">
      <h4 className="text-sm font-semibold text-[#0f172a] uppercase tracking-wider mb-3">Update Status</h4>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-700">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {error}
        </div>
      )}

      {success && (
        <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2 text-sm text-green-700">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
          Status updated successfully!
        </div>
      )}

      <div className="space-y-4">
        <div>
          <label className="block text-xs font-medium text-[#64748b] mb-1">Current Status</label>
          <div className={`inline-flex items-center px-3 py-1 rounded-md text-xs font-semibold border ${getStatusColor(currentStatus)}`}>
            {currentStatus}
          </div>
        </div>

        {allowedNextStatuses.length > 0 || canReject ? (
          <form onSubmit={handleStatusChange} className="space-y-4">
            {!showRejectOption ? (
              <>
                {allowedNextStatuses.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#64748b] mb-1">Change To</label>
                    <select
                      value={newStatus}
                      onChange={(e) => setNewStatus(e.target.value)}
                      className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    >
                      <option value={currentStatus}>{currentStatus} (current)</option>
                      {allowedNextStatuses.map((status) => (
                        <option key={status} value={status}>{status}</option>
                      ))}
                    </select>
                  </div>
                )}

                {allowedNextStatuses.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#64748b] mb-1">Work Note <span className="font-normal text-[#94a3b8]">(Optional)</span></label>
                    <textarea
                      value={workNote}
                      onChange={(e) => setWorkNote(e.target.value)}
                      placeholder="Add a note (visible to everyone)"
                      className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      rows="2"
                    />
                    <p className="text-[10px] text-[#94a3b8] mt-1 text-right">{workNote.length}/500 chars</p>
                  </div>
                )}

                {newStatus === "RESOLVED" && allowedNextStatuses.length > 0 && (
                  <div className="p-4 bg-[#f8fafc] border border-[#e2e8f0] rounded-xl space-y-3">
                    <div>
                      <label className="block text-sm font-semibold text-[#0f172a] mb-2">Solution Summary <span className="text-red-500">*</span></label>
                      <textarea
                        value={solutionSummary}
                        onChange={(e) => setSolutionSummary(e.target.value)}
                        placeholder="Explain the solution to the student..."
                        className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                        rows="3"
                      />
                      <p className="text-xs text-[#64748b] mt-2">Will be visible to the student | Min 20 chars</p>
                    </div>
                  </div>
                )}

                {allowedNextStatuses.length > 0 && (
                  <button
                    type="submit"
                    disabled={loading || newStatus === currentStatus}
                    className="w-full px-4 py-2 text-sm font-medium bg-[#0f172a] text-white rounded-lg hover:bg-[#1e293b] transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {loading && <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>}
                    {loading ? "Updating..." : "Update Status"}
                  </button>
                )}

                {canReject && (
                  <button
                    type="button"
                    onClick={() => { setShowRejectOption(true); setNewStatus("REJECTED"); }}
                    className="w-full px-4 py-2 text-sm font-medium text-red-600 border border-red-200 bg-red-50 rounded-lg hover:bg-red-100 transition-colors"
                  >
                    Reject Ticket
                  </button>
                )}
              </>
            ) : (
              <div className="p-4 bg-red-50 border border-red-200 rounded-xl space-y-4">
                <div>
                  <label className="block text-sm font-semibold text-red-900 mb-2">Rejection Reason <span className="text-red-500">*</span></label>
                  <textarea
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                    placeholder="Explain why this ticket is being rejected..."
                    className="w-full px-3 py-2 text-sm border border-red-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 bg-white"
                    rows="3"
                  />
                  <p className="text-xs text-red-600 mt-2">Min 10 characters required</p>
                </div>

                <div className="flex gap-3">
                  <button
                    type="submit"
                    disabled={loading || rejectReason.length < 10}
                    className="flex-1 px-4 py-2 text-sm font-medium bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
                  >
                    {loading ? "Rejecting..." : "Confirm Rejection"}
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowRejectOption(false); setNewStatus(currentStatus); setRejectReason(""); }}
                    className="flex-1 px-4 py-2 text-sm font-medium border border-gray-300 bg-white text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </form>
        ) : (
          <div className="p-4 bg-[#f8fafc] border border-[#e2e8f0] border-dashed rounded-xl text-center text-[#64748b]">
            <p className="text-sm font-medium">No status transitions available</p>
            <p className="text-xs mt-1">This ticket is in a final state</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default TicketStatusUpdate;
