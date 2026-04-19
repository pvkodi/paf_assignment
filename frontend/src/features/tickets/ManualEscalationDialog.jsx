import React, { useCallback, useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Manual Escalation Dialog Component
 * Allows TECHNICIAN, FACILITY_MANAGER, ADMIN to manually escalate tickets
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
    if (facilityId) fetchAvailableStaff();
  }, [facilityId, fetchAvailableStaff]);

  useEffect(() => {
    if (!searchTerm.trim()) {
      setFilteredStaff(staff);
      return;
    }
    const term = searchTerm.toLowerCase();
    const filtered = staff.filter(
      (s) => s.name?.toLowerCase().includes(term) || s.email?.toLowerCase().includes(term)
    );
    setFilteredStaff(filtered);
  }, [searchTerm, staff]);

  const handleEscalate = async (e) => {
    e.preventDefault();
    if (!reason.trim()) { setError("Please provide a reason for escalation"); return; }
    if (!assigneeId) { setError("Please assign this ticket to a staff member"); return; }

    try {
      setSubmitting(true);
      setError(null);
      const response = await apiClient.post(`/tickets/${ticketId}/escalate`, {
        reason: reason.trim(),
        assigneeId: assigneeId,
      });
      onEscalationSuccess(response.data);
    } catch (err) {
      console.error("Escalation failed:", err);
      setError(err.response?.data?.message || "Failed to escalate ticket. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-[#0f172a]/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="px-6 py-4 border-b border-[#e2e8f0] flex justify-between items-center bg-orange-50">
          <div>
            <h2 className="text-lg font-semibold text-[#c2410c]">Escalate & Assign Ticket</h2>
            <p className="text-sm text-[#ea580c]">Escalate this ticket to the next level</p>
          </div>
          <button onClick={onCancel} className="text-[#fdba74] hover:text-[#c2410c] transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>

        <form onSubmit={handleEscalate} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-[#0f172a] mb-1">
              Reason for Escalation <span className="text-red-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g., Electrical hazard detected, Safety risk..."
              className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500"
              rows="3"
              disabled={submitting}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#0f172a] mb-2">
              Assign To <span className="text-red-500">*</span>
            </label>
            <div className="relative mb-3">
              <svg className="w-4 h-4 text-[#94a3b8] absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
              <input
                type="text"
                placeholder="Search staff..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500"
                disabled={loading || submitting}
              />
            </div>

            {loading ? (
              <div className="flex items-center justify-center py-6">
                <svg className="animate-spin h-5 w-5 text-orange-500" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </div>
            ) : filteredStaff.length === 0 ? (
              <div className="p-3 bg-[#f8fafc] border border-[#e2e8f0] border-dashed rounded-lg text-center text-[#64748b] text-sm">
                {staff.length === 0 ? "No staff available" : "No staff match your search"}
              </div>
            ) : (
              <div className="border border-[#e2e8f0] rounded-lg max-h-48 overflow-y-auto divide-y divide-[#e2e8f0]">
                {filteredStaff.map((member) => (
                  <label key={member.id} className="flex items-center p-3 hover:bg-orange-50 transition cursor-pointer">
                    <input
                      type="radio"
                      name="assignee"
                      value={member.id}
                      checked={assigneeId === member.id}
                      onChange={(e) => setAssigneeId(e.target.value)}
                      className="mr-3 text-orange-600 focus:ring-orange-500"
                    />
                    <div className="flex-1">
                      <p className="font-medium text-[#0f172a] text-sm">{member.name}</p>
                      <p className="text-xs text-[#64748b]">{member.email}</p>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-700">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-[#e2e8f0]">
            <button
              type="button"
              onClick={onCancel}
              disabled={submitting}
              className="px-4 py-2 text-sm font-medium border border-[#e2e8f0] rounded-lg text-[#475569] hover:bg-[#f8fafc] transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || !reason.trim() || !assigneeId}
              className="px-4 py-2 text-sm font-medium bg-[#ea580c] text-white rounded-lg hover:bg-[#c2410c] transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {submitting && <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>}
              {submitting ? "Escalating..." : "Escalate & Assign"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
