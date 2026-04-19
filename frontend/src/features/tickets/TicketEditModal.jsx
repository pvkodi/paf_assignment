import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Ticket Edit Modal Component - Modern Aesthetic
 * Allows editing of ticket title, description, category, and priority
 */
export function TicketEditModal({ ticket, isOpen, onClose, onTicketUpdated }) {
  const [formData, setFormData] = useState({
    title: ticket?.title || "",
    description: ticket?.description || "",
    category: ticket?.category || "",
    priority: ticket?.priority || "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const CATEGORIES = ["ELECTRICAL", "PLUMBING", "HVAC", "IT_NETWORKING", "STRUCTURAL", "CLEANING", "SAFETY", "OTHER"];
  const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

  useEffect(() => {
    if (ticket) {
      setFormData({
        title: ticket.title,
        description: ticket.description,
        category: ticket.category,
        priority: ticket.priority,
      });
      setError(null);
    }
  }, [ticket, isOpen]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const validateForm = () => {
    if (!formData.title.trim()) { setError("Title is required"); return false; }
    if (formData.title.length < 20) { setError("Title must be at least 20 characters"); return false; }
    if (formData.title.length > 200) { setError("Title must not exceed 200 characters"); return false; }
    if (!formData.description.trim()) { setError("Description is required"); return false; }
    if (formData.description.length < 50) { setError("Description must be at least 50 characters"); return false; }
    if (!formData.category) { setError("Category is required"); return false; }
    if (!formData.priority) { setError("Priority is required"); return false; }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.put(`/tickets/${ticket.id}`, formData);
      onTicketUpdated(response.data);
      onClose();
    } catch (err) {
      console.error("Failed to update ticket:", err);
      setError(err.response?.data?.message || "Failed to update ticket. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-[#0f172a]/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="px-6 py-4 border-b border-[#e2e8f0] flex justify-between items-center">
          <div>
            <h2 className="text-lg font-semibold text-[#0f172a]">Edit Ticket</h2>
            <p className="text-sm text-[#64748b]">Update ticket details</p>
          </div>
          <button onClick={onClose} className="text-[#94a3b8] hover:text-[#0f172a] transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-700">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {error}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-[#0f172a] mb-1">
              Title <span className="text-[#94a3b8] font-normal text-xs">({formData.title.length}/200)</span>
            </label>
            <input
              type="text"
              name="title"
              value={formData.title}
              onChange={handleChange}
              className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Short but descriptive title"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#0f172a] mb-1">
              Description <span className="text-[#94a3b8] font-normal text-xs">({formData.description.length} chars)</span>
            </label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows="4"
              className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Detailed description of the issue (min 50 characters)"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-[#0f172a] mb-1">Category</label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                <option value="">Select category</option>
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#0f172a] mb-1">Priority</label>
              <select
                name="priority"
                value={formData.priority}
                onChange={handleChange}
                className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                <option value="">Select priority</option>
                {PRIORITIES.map((pri) => (
                  <option key={pri} value={pri}>{pri}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-[#e2e8f0]">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 text-sm font-medium border border-[#e2e8f0] rounded-lg text-[#475569] hover:bg-[#f8fafc] transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm font-medium bg-[#0f172a] text-white rounded-lg hover:bg-[#1e293b] transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {loading && <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>}
              {loading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
