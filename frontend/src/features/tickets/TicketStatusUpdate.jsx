import React, { useState } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Ticket Status Update Component
 * Allows technicians to update ticket status with state machine validation
 * Implements US4 requirement: Status update interface with allowed transitions
 */

export function TicketStatusUpdate({ ticketId, currentStatus, onStatusUpdated }) {
  const [newStatus, setNewStatus] = useState(currentStatus);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  // Define allowed state transitions based on state machine
  const getNextStatuses = (status) => {
    const transitions = {
      OPEN: ["IN_PROGRESS", "ON_HOLD", "REJECTED"],
      IN_PROGRESS: ["ON_HOLD", "RESOLVED", "REJECTED"],
      ON_HOLD: ["IN_PROGRESS", "REJECTED"],
      RESOLVED: ["CLOSED"],
      CLOSED: [],
      REJECTED: [],
    };
    return transitions[status] || [];
  };

  const allowedNextStatuses = getNextStatuses(currentStatus);

  const handleStatusChange = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    if (newStatus === currentStatus) {
      setError("Please select a different status");
      return;
    }

    if (!allowedNextStatuses.includes(newStatus)) {
      setError("This status transition is not allowed");
      return;
    }

    try {
      setLoading(true);
      await apiClient.put(`/tickets/${ticketId}/status`, {
        status: newStatus,
      });

      setSuccess(true);
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
      ON_HOLD: "bg-orange-100 text-orange-800",
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

        {/* Status Selector */}
        {allowedNextStatuses.length > 0 ? (
          <form onSubmit={handleStatusChange} className="space-y-3">
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

            <button
              type="submit"
              disabled={loading || newStatus === currentStatus}
              className="w-full px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed font-medium"
            >
              {loading ? "Updating..." : "Update Status"}
            </button>
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
