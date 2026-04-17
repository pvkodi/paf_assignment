import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * Ticket Edit Modal Component
 * Allows editing of ticket title, description, category, and priority
 * Only available for OPEN tickets by creator or ADMIN
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

  const CATEGORIES = [
    "ELECTRICAL",
    "PLUMBING",
    "HVAC",
    "IT_NETWORKING",
    "STRUCTURAL",
    "CLEANING",
    "SAFETY",
    "OTHER",
  ];
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
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const validateForm = () => {
    if (!formData.title.trim()) {
      setError("Title is required");
      return false;
    }
    if (formData.title.length < 20) {
      setError("Title must be at least 20 characters");
      return false;
    }
    if (formData.title.length > 200) {
      setError("Title must not exceed 200 characters");
      return false;
    }
    if (!formData.description.trim()) {
      setError("Description is required");
      return false;
    }
    if (formData.description.length < 50) {
      setError("Description must be at least 50 characters");
      return false;
    }
    if (!formData.category) {
      setError("Category is required");
      return false;
    }
    if (!formData.priority) {
      setError("Priority is required");
      return false;
    }
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
      setError(
        err.response?.data?.message || "Failed to update ticket. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-96 overflow-y-auto">
        {/* Header */}
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">Edit Ticket</h2>
          <p className="text-gray-600 mt-1">Update ticket details</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {/* Title */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Title ({formData.title.length}/200)
            </label>
            <input
              type="text"
              name="title"
              value={formData.title}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Short but descriptive title"
            />
            <p className="text-xs text-gray-500 mt-1">Min 20, max 200 characters</p>
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description ({formData.description.length} chars)
            </label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows="3"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Detailed description of the issue (min 50 characters)"
            />
            <p className="text-xs text-gray-500 mt-1">Min 50 characters</p>
          </div>

          {/* Category and Priority */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Category
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Select category</option>
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Priority
              </label>
              <select
                name="priority"
                value={formData.priority}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Select priority</option>
                {PRIORITIES.map((pri) => (
                  <option key={pri} value={pri}>
                    {pri}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Buttons */}
          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
            >
              {loading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
