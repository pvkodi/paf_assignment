import React, { useCallback, useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Ticket Assignment Dialog Component
 * Allows facility managers/admins to assign technicians to tickets
 * Implements US4 requirement: Technician assignment dialog with search
 */

export function TicketAssignmentDialog({ ticketId, facilityId, onAssigned, onClose }) {
  const [technicians, setTechnicians] = useState([]);
  const [filteredTechnicians, setFilteredTechnicians] = useState([]);
  const [selectedTechnicianId, setSelectedTechnicianId] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const fetchTechnicians = useCallback(async () => {
    try {
      setLoading(true);
      const response = await apiClient.get(`/facilities/${facilityId}/technicians`);
      setTechnicians(response.data || []);
    } catch (err) {
      console.error("Failed to fetch technicians:", err);
      setError("Failed to load technicians. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [facilityId]);

  const applySearch = useCallback(() => {
    if (!searchTerm.trim()) {
      setFilteredTechnicians(technicians);
      return;
    }

    const term = searchTerm.toLowerCase();
    const filtered = technicians.filter(
      (tech) =>
        tech.name?.toLowerCase().includes(term) ||
        tech.email?.toLowerCase().includes(term),
    );
    setFilteredTechnicians(filtered);
  }, [searchTerm, technicians]);

  useEffect(() => {
    fetchTechnicians();
  }, [fetchTechnicians]);

  useEffect(() => {
    applySearch();
  }, [applySearch]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!selectedTechnicianId) {
      setError("Please select a technician");
      return;
    }

    try {
      setSubmitting(true);
      const response = await apiClient.post(`/tickets/${ticketId}/assign`, {
        technicianId: selectedTechnicianId,
      });

      const technicianName = response.data?.assignedTechnicianName || "Technician";
      const successMsg = `✓ ${technicianName} assigned successfully!`;
      setSuccess(successMsg);
      
      setTimeout(() => {
        onAssigned();
        onClose();
      }, 2000);
    } catch (err) {
      console.error("Failed to assign technician:", err);
      setError(err.response?.data?.message || "Failed to assign technician. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-[#0f172a]/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="px-6 py-4 border-b border-[#e2e8f0] flex justify-between items-center">
          <div>
            <h2 className="text-lg font-semibold text-[#0f172a]">Assign Technician</h2>
            <p className="text-sm text-[#64748b]">Select staff member for ticket</p>
          </div>
          <button onClick={onClose} className="text-[#94a3b8] hover:text-[#0f172a] transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-700">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {error}
            </div>
          )}

          {success && (
            <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2 text-sm text-green-700 font-medium">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
              {success}
            </div>
          )}

          <div className="relative">
            <svg className="w-4 h-4 text-[#94a3b8] absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            <input
              type="text"
              placeholder="Search by name or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#0f172a] mb-2">
              Select Technician
            </label>

            {loading ? (
              <div className="flex items-center justify-center py-8">
                <svg className="animate-spin h-6 w-6 text-indigo-500" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </div>
            ) : filteredTechnicians.length === 0 ? (
              <div className="p-4 bg-[#f8fafc] border border-[#e2e8f0] border-dashed rounded-lg text-center text-[#64748b] text-sm">
                {technicians.length === 0
                  ? "No technicians available"
                  : "No technicians match your search"}
              </div>
            ) : (
              <div className="border border-[#e2e8f0] rounded-lg max-h-56 overflow-y-auto divide-y divide-[#e2e8f0]">
                {filteredTechnicians.map((technician) => (
                  <label
                    key={technician.id}
                    className="flex items-center p-3 hover:bg-[#f8fafc] transition cursor-pointer"
                  >
                    <input
                      type="radio"
                      name="technician"
                      value={technician.id}
                      checked={selectedTechnicianId === technician.id}
                      onChange={(e) => setSelectedTechnicianId(e.target.value)}
                      className="mr-3 text-indigo-600 focus:ring-indigo-500"
                    />
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs">
                        {technician.name ? technician.name.charAt(0) : "?"}
                      </div>
                      <div className="flex-1">
                        <p className="font-medium text-[#0f172a] text-sm">{technician.name}</p>
                        <p className="text-xs text-[#64748b]">{technician.email}</p>
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-[#e2e8f0]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium border border-[#e2e8f0] rounded-lg text-[#475569] hover:bg-[#f8fafc] transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || !selectedTechnicianId}
              className="px-4 py-2 text-sm font-medium bg-[#0f172a] text-white rounded-lg hover:bg-[#1e293b] transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {submitting && <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>}
              {submitting ? "Assigning..." : "Assign"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default TicketAssignmentDialog;
