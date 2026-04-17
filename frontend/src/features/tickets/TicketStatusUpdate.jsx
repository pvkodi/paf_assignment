import React, { useState } from "react";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";

/**
 * Ticket Status Update Component
 * Allows technicians to update ticket status with state machine validation
 * Workflow: OPEN → IN_PROGRESS → RESOLVED → CLOSED
 * Admin can also reject with reason at any state
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

  // Define allowed state transitions based on state machine
  // Technicians: strict workflow (OPEN → IN_PROGRESS → RESOLVED → CLOSED)
  // Admins: can skip steps and transition to any forward state
  const getNextStatuses = (status) => {
    const strictTransitions = {
      OPEN: ["IN_PROGRESS"],
      IN_PROGRESS: ["RESOLVED"],
      RESOLVED: ["CLOSED"],
      CLOSED: [],
      REJECTED: [],
    };

    // Admin override: can move to any later status in the workflow
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

    // Validate rejection
    if (showRejectOption) {
      if (!rejectReason.trim()) {
        setError("Please provide a reason for rejection");
        return;
      }
      if (rejectReason.length < 10) {
        setError("Rejection reason must be at least 10 characters");
        return;
      }
    }

    // Validate solution summary for RESOLVED status
    if (newStatus === "RESOLVED" && !showRejectOption) {
      if (!solutionSummary.trim()) {
        setError("Please provide a solution summary for the student");
        return;
      }
      if (solutionSummary.length < 20) {
        setError("Solution summary must be at least 20 characters");
        return;
      }
      if (solutionSummary.length > 2000) {
        setError("Solution summary must not exceed 2000 characters");
        return;
      }
    }

    try {
      setLoading(true);
      const payload = { status: newStatus };
      if (showRejectOption) {
        payload.rejectionReason = rejectReason;
      }
      
      await apiClient.put(`/tickets/${ticketId}/status`, payload);

      // Create comments based on status change
      try {
        // For RESOLVED status, create PUBLIC solution summary comment (student-facing)
        if (newStatus === "RESOLVED" && !showRejectOption) {
          await apiClient.post(`/tickets/${ticketId}/comments`, {
            content: `**SOLUTION:** ${solutionSummary}`,
            visibility: "PUBLIC",
          });
        }

        // Create PUBLIC work note comment for all status changes (visible to everyone)
        if (workNote.trim() && !showRejectOption) {
          await apiClient.post(`/tickets/${ticketId}/comments`, {
            content: `Status updated to ${newStatus}: ${workNote}`,
            visibility: "PUBLIC",
          });
        }
      } catch (commentErr) {
        console.error("Failed to create status change comment:", commentErr);
        // Don't fail the whole operation if comment creation fails
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
      setError(
        err.response?.data?.message ||
          "Failed to update status. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      OPEN: "bg-blue-100 text-blue-800",
      IN_PROGRESS: "bg-purple-100 text-purple-800",
      RESOLVED: "bg-green-100 text-green-800",
      CLOSED: "bg-gray-100 text-gray-800",
      REJECTED: "bg-red-100 text-red-800",
    };
    return colors[status] || "bg-gray-100 text-gray-800";
  };

  return (
    <div className="space-y-4">
      <h4 className="font-semibold text-gray-900">Update Status</h4>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {success && (
        <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
          <p className="text-sm text-green-700">Status updated successfully!</p>
        </div>
      )}

      <div className="space-y-3">
        {/* Current Status */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Current Status
          </label>
          <div
            className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(
              currentStatus,
            )}`}
          >
            {currentStatus}
          </div>
        </div>

        {/* Status Progression */}
        {allowedNextStatuses.length > 0 || canReject ? (
          <form onSubmit={handleStatusChange} className="space-y-3">
            {!showRejectOption ? (
              <>
                {allowedNextStatuses.length > 0 && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Change To
                    </label>
                    <select
                      value={newStatus}
                      onChange={(e) => setNewStatus(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value={currentStatus}>{currentStatus} (current)</option>
                      {allowedNextStatuses.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {allowedNextStatuses.length > 0 && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Work Note (Optional)
                    </label>
                    <textarea
                      value={workNote}
                      onChange={(e) => setWorkNote(e.target.value)}
                      placeholder="Add a note about this status change (visible to everyone)"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                      rows="2"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      💬 This will be visible to the student | {workNote.length}/500 characters
                    </p>
                  </div>
                )}

                {newStatus === "RESOLVED" && allowedNextStatuses.length > 0 && (
                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg space-y-3">
                    <div>
                      <label className="block text-sm font-medium text-blue-900 mb-2">
                        Solution Summary for Student <span className="text-red-500">*</span>
                      </label>
                      <textarea
                        value={solutionSummary}
                        onChange={(e) => setSolutionSummary(e.target.value)}
                        placeholder="Explain the solution to the student (e.g., 'Replaced faulty power supply. Tested and confirmed working.')"
                        className="w-full px-3 py-2 border border-blue-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                        rows="3"
                      />
                      <p className="text-xs text-blue-700 mt-2">
                        ✓ Student will see this explanation | Min 20 chars | {solutionSummary.length}/2000
                      </p>
                    </div>
                  </div>
                )}

                {allowedNextStatuses.length > 0 && (
                  <button
                    type="submit"
                    disabled={loading || newStatus === currentStatus}
                    className="w-full px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed font-medium"
                  >
                    {loading ? "Updating..." : "Update Status"}
                  </button>
                )}

                {canReject && (
                  <button
                    type="button"
                    onClick={() => {
                      setShowRejectOption(true);
                      setNewStatus("REJECTED");
                    }}
                    className="w-full px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-medium"
                  >
                    Reject Ticket
                  </button>
                )}
              </>
            ) : (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Rejection Reason <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                    placeholder="Explain why this ticket is being rejected (minimum 10 characters)"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 text-sm"
                    rows="3"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    {rejectReason.length}/10 minimum characters
                  </p>
                </div>

                <div className="flex gap-2">
                  <button
                    type="submit"
                    disabled={loading || rejectReason.length < 10}
                    className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition disabled:opacity-50 disabled:cursor-not-allowed font-medium"
                  >
                    {loading ? "Rejecting..." : "Confirm Rejection"}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowRejectOption(false);
                      setNewStatus(currentStatus);
                      setRejectReason("");
                    }}
                    className="flex-1 px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition font-medium"
                  >
                    Cancel
                  </button>
                </div>
              </>
            )}
          </form>
        ) : (
          <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg text-center text-gray-500">
            <p className="text-sm">No status transitions available</p>
            <p className="text-xs mt-1">This ticket is in a final state</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default TicketStatusUpdate;
