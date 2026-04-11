import React, { useState, useEffect } from "react";
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

  useEffect(() => {
    fetchTechnicians();
  }, [facilityId]);

  useEffect(() => {
    applySearch();
  }, [technicians, searchTerm]);

  const fetchTechnicians = async () => {
    try {
      setLoading(true);
      // Fetch technicians for the facility
      const response = await apiClient.get(
        `/facilities/${facilityId}/technicians`,
      );
      setTechnicians(response.data || []);
    } catch (err) {
      console.error("Failed to fetch technicians:", err);
      setError("Failed to load technicians. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const applySearch = () => {
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
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!selectedTechnicianId) {
      setError("Please select a technician");
      return;
    }

    try {
      setSubmitting(true);
      await apiClient.post(`/tickets/${ticketId}/assign`, {
        technicianId: selectedTechnicianId,
      });

      onAssigned();
      onClose();
    } catch (err) {
      console.error("Failed to assign technician:", err);
      setError(
        err.response?.data?.message ||
          "Failed to assign technician. Please try again.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 className="text-xl font-bold text-gray-900 mb-4">
          Assign Technician
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {/* Search Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Search Technician
            </label>
            <input
              type="text"
              placeholder="Search by name or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Technician Selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select Technician
            </label>

            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600"></div>
              </div>
            ) : filteredTechnicians.length === 0 ? (
              <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg text-center text-gray-500">
                {technicians.length === 0
                  ? "No technicians available"
                  : "No technicians match your search"}
              </div>
            ) : (
              <div className="border border-gray-300 rounded-lg max-h-64 overflow-y-auto">
                {filteredTechnicians.map((technician) => (
                  <label
                    key={technician.id}
                    className="flex items-center p-3 hover:bg-indigo-50 transition cursor-pointer border-b border-gray-200 last:border-b-0"
                  >
                    <input
                      type="radio"
                      name="technician"
                      value={technician.id}
                      checked={selectedTechnicianId === technician.id}
                      onChange={(e) =>
                        setSelectedTechnicianId(e.target.value)}
                      className="mr-3"
                    />
                    <div className="flex-1">
                      <p className="font-medium text-gray-900">
                        {technician.name}
                      </p>
                      <p className="text-xs text-gray-600">{technician.email}</p>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>

          {/* Buttons */}
          <div className="flex gap-3 justify-end pt-4 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || !selectedTechnicianId}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? "Assigning..." : "Assign"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default TicketAssignmentDialog;
