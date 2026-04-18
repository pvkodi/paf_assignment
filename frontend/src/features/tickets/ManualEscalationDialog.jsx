import React, { useState } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Manual Escalation Dialog Component
 * Allows TECHNICIAN, FACILITY_MANAGER, ADMIN to manually escalate tickets
 * with a documented reason for the escalation history audit trail
 */
export function ManualEscalationDialog({ ticketId, onEscalationSuccess, onCancel }) {
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleEscalate = async (e) => {
    e.preventDefault();

    if (!reason.trim()) {
      setError("Please provide a reason for escalation");
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.post(`/tickets/${ticketId}/escalate`, {
        reason: reason.trim(),
      });

      console.log("Escalation successful:", response.data);
      onEscalationSuccess(response.data);
    } catch (err) {
      console.error("Escalation failed:", err);
      setError(
        err.response?.data?.message ||
          "Failed to escalate ticket. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Manually Escalate Ticket
        </h3>

        <p className="text-sm text-gray-600 mb-4">
          Escalating this ticket will move it to the next severity level and notify
          higher-level staff. Please provide a reason for this escalation.
        </p>

        <form onSubmit={handleEscalate}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Reason for Escalation *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g., Electrical hazard detected, Safety risk, Complex issue identified"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-sm"
              rows="4"
              disabled={loading}
            />
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={onCancel}
              disabled={loading}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || !reason.trim()}
              className="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition disabled:opacity-50 flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  Escalating...
                </>
              ) : (
                <>
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M13 10V3L4 14h7v7l9-11h-7z"
                    />
                  </svg>
                  Escalate Now
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
