import React, { useCallback, useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Manual Escalation Dialog Component
 * Allows TECHNICIAN, FACILITY_MANAGER, ADMIN to manually escalate tickets
 * with a documented reason and assignment to the next level staff
 */
export function ManualEscalationDialog({ ticketId, facilityId, onEscalationSuccess, onCancel }) {
  const [reason, setReason] = useState("");
  const [assigneeId, setAssigneeId] = useState("");
  const [staff, setStaff] = useState([]);
  const [filteredStaff, setFilteredStaff] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Fetch available staff for escalation
  const fetchAvailableStaff = useCallback(async () => {
    try {
      setLoading(true);
      const response = await apiClient.get(`/facilities/${facilityId}/technicians`);
      setStaff(response.data || []);
      setFilteredStaff(response.data || []);
    } catch (err) {
      console.error("Failed to fetch staff:", err);
      setError("Failed to load available staff. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [facilityId]);

  useEffect(() => {
    if (facilityId) {
      fetchAvailableStaff();
    }
  }, [facilityId, fetchAvailableStaff]);

  // Filter staff based on search term
  useEffect(() => {
    if (!searchTerm.trim()) {
      setFilteredStaff(staff);
      return;
    }
    const term = searchTerm.toLowerCase();
    const filtered = staff.filter(
      (s) =>
        s.name?.toLowerCase().includes(term) ||
        s.email?.toLowerCase().includes(term)
    );
    setFilteredStaff(filtered);
  }, [searchTerm, staff]);

  const handleEscalate = async (e) => {
    e.preventDefault();

    if (!reason.trim()) {
      setError("Please provide a reason for escalation");
      return;
    }

    if (!assigneeId) {
      setError("Please assign this ticket to a staff member");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const response = await apiClient.post(`/tickets/${ticketId}/escalate`, {
        reason: reason.trim(),
        assigneeId: assigneeId,
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
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Escalate & Assign Ticket
        </h3>

        <p className="text-sm text-gray-600 mb-5">
          Escalate this ticket to the next level and assign it to a staff member to ensure proper handling.
        </p>

        <form onSubmit={handleEscalate}>
          {/* Reason for Escalation */}
          <div className="mb-5">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Reason for Escalation *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g., Electrical hazard detected, Safety risk, Complex issue identified"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-sm"
              rows="3"
              disabled={submitting}
            />
          </div>

          {/* Assign to Staff Member */}
          <div className="mb-5">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Assign To *
            </label>
            
            {/* Search Box */}
            <input
              type="text"
              placeholder="Search by name or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-sm mb-3"
              disabled={loading || submitting}
            />

            {/* Staff Selection */}
            {loading ? (
              <div className="flex items-center justify-center py-6">
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-orange-600"></div>
              </div>
            ) : filteredStaff.length === 0 ? (
              <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg text-center text-gray-500 text-sm">
                {staff.length === 0
                  ? "No staff members available"
                  : "No staff members match your search"}
              </div>
            ) : (
              <div className="border border-gray-300 rounded-lg max-h-48 overflow-y-auto">
                {filteredStaff.map((member) => (
                  <label
                    key={member.id}
                    className="flex items-center p-3 hover:bg-orange-50 transition cursor-pointer border-b border-gray-200 last:border-b-0"
                  >
                    <input
                      type="radio"
                      name="assignee"
                      value={member.id}
                      checked={assigneeId === member.id}
                      onChange={(e) => setAssigneeId(e.target.value)}
                      className="mr-3"
                    />
                    <div className="flex-1">
                      <p className="font-medium text-gray-900 text-sm">
                        {member.name}
                      </p>
                      <p className="text-xs text-gray-600">{member.email}</p>
                    </div>
                  </label>
                ))}
              </div>
            )}
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
              disabled={submitting}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || !reason.trim() || !assigneeId}
              className="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition disabled:opacity-50 flex items-center gap-2"
            >
              {submitting ? (
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
                  Escalate & Assign
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
