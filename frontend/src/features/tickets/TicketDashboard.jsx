import React, { useCallback, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient } from "../../services/apiClient";
import { AuthContext } from "../../contexts/AuthContext";

/**
 * Ticket Dashboard Component
 * Displays list of tickets with filtering, search, and status indicators
 * Implements US4 requirement: Ticket list/dashboard with filters and search
 */

// Utility function to shorten UUID
const shortenTicketId = (fullId) => {
  if (!fullId) return "";
  return fullId.substring(0, 8);
};

// Utility function to copy to clipboard
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    console.log("Copied to clipboard:", text);
  });
};

export function TicketDashboard() {
  const navigate = useNavigate();
  const { hasRole } = useContext(AuthContext);
  const [tickets, setTickets] = useState([]);
  const [filteredTickets, setFilteredTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [priorityFilter, setPriorityFilter] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);

  const STATUS_OPTIONS = [
    "OPEN",
    "IN_PROGRESS",
    "ON_HOLD",
    "RESOLVED",
    "CLOSED",
    "REJECTED",
  ];
  const PRIORITY_OPTIONS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

  // Fetch tickets on component mount
  useEffect(() => {
    fetchTickets();
  }, []);

  // Apply filters whenever search or filter values change
  const fetchTickets = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/tickets");
      
      // Handle response format from backend (returns List directly)
      let ticketsList = [];
      if (Array.isArray(response.data)) {
        ticketsList = response.data;
      } else if (response.data && Array.isArray(response.data.content)) {
        // If paginated format
        ticketsList = response.data.content;
      }
      
      setTickets(ticketsList || []);
    } catch (err) {
      console.error("Failed to fetch tickets:", err);
      setError("Failed to load tickets. Please try again.");
      setTickets([]); // Ensure tickets is always an array
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = useCallback(() => {
    let filtered = tickets;

    // Apply search filter
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (ticket) =>
          ticket.title?.toLowerCase().includes(term) ||
          ticket.description?.toLowerCase().includes(term) ||
          ticket.id?.toString().includes(term),
      );
    }

    // Apply status filter
    if (statusFilter) {
      filtered = filtered.filter((ticket) => ticket.status === statusFilter);
    }

    // Apply priority filter
    if (priorityFilter) {
      filtered = filtered.filter((ticket) => ticket.priority === priorityFilter);
    }

    setFilteredTickets(filtered);
  }, [tickets, searchTerm, statusFilter, priorityFilter]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

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

  const getPriorityColor = (priority) => {
    const colors = {
      LOW: "text-green-600",
      MEDIUM: "text-blue-600",
      HIGH: "text-orange-600",
      CRITICAL: "text-red-600",
    };
    return colors[priority] || "text-gray-600";
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    return new Date(dateString).toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Tickets</h2>
          <p className="text-gray-600 mt-1">
            View and manage maintenance tickets
          </p>
        </div>
        {(hasRole("USER") || hasRole("STUDENT") || hasRole("LECTURER") || hasRole("ADMIN")) && (
          <button
            onClick={() => setShowCreateForm(!showCreateForm)}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
          >
            {showCreateForm ? "Cancel" : "Create Ticket"}
          </button>
        )}
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-red-700">{error}</p>
        </div>
      )}

      {/* Create Form */}
      {showCreateForm && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <TicketCreationForm
            onTicketCreated={() => {
              setShowCreateForm(false);
              fetchTickets();
            }}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <input
            type="text"
            placeholder="Search by ID, title, or description..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">All Statuses</option>
            {STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>

          {/* Priority Filter */}
          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">All Priorities</option>
            {PRIORITY_OPTIONS.map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </select>

          {/* Reset Filters */}
          {(searchTerm || statusFilter || priorityFilter) && (
            <button
              onClick={() => {
                setSearchTerm("");
                setStatusFilter("");
                setPriorityFilter("");
              }}
              className="px-3 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Reset Filters
            </button>
          )}
        </div>

        {/* Filter Info */}
        <div className="flex items-center justify-between text-sm text-gray-600">
          <span>
            Showing {filteredTickets.length} of {tickets.length} tickets
          </span>
        </div>
      </div>

      {/* Tickets Table */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        {filteredTickets.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <p className="text-lg font-medium">No tickets found</p>
            <p className="text-sm mt-1">
              {tickets.length === 0
                ? "No tickets yet. Create one to get started."
                : "Try adjusting your filters"}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Title
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Priority
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Created
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Assigned To
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {filteredTickets.map((ticket) => (
                  <tr
                    key={ticket.id}
                    className="hover:bg-gray-50 transition cursor-pointer"
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      <div className="flex items-center gap-2">
                        <span className="text-indigo-600 font-semibold">#{shortenTicketId(ticket.id)}</span>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            copyToClipboard(ticket.id);
                          }}
                          title="Copy full ticket ID"
                          className="p-0.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                          </svg>
                        </button>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-700 max-w-xs truncate">
                      {ticket.title}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(
                          ticket.status,
                        )}`}
                      >
                        {ticket.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`font-medium ${getPriorityColor(ticket.priority)}`}>
                        {ticket.priority}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {formatDate(ticket.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {ticket.assignedTechnicianName || "—"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <button
                        onClick={() =>
                          navigate(`/tickets/${ticket.id}`)}
                        className="text-indigo-600 hover:text-indigo-900 font-medium transition"
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Ticket Creation Form Component
 * Handles creation of new tickets with validation
 */
function TicketCreationForm({ onTicketCreated, onCancel }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [facilities, setFacilities] = useState([]);
  const [formData, setFormData] = useState({
    facilityId: "",
    category: "ELECTRICAL",
    priority: "MEDIUM",
    title: "",
    description: "",
  });
  const [files, setFiles] = useState([]);
  const [fileError, setFileError] = useState(null);

  // Match backend TicketCategory enum values
  const CATEGORIES = ["ELECTRICAL", "PLUMBING", "HVAC", "IT_NETWORKING", "STRUCTURAL", "CLEANING", "SAFETY", "OTHER"];
  const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

  // Fetch facilities
  useEffect(() => {
    fetchFacilities();
  }, []);

  const fetchFacilities = async () => {
    try {
      const response = await apiClient.get("/facilities");
      
      // Handle Spring Data Page format from backend
      let facilitiesList = [];
      if (response.data && Array.isArray(response.data.content)) {
        // Backend returns Page<FacilityResponseDTO> with content array
        facilitiesList = response.data.content;
      } else if (Array.isArray(response.data)) {
        // Fallback: direct array format
        facilitiesList = response.data;
      } else if (response.data && Array.isArray(response.data.items)) {
        // Fallback: items array format
        facilitiesList = response.data.items;
      }
      
      setFacilities(facilitiesList || []);
    } catch (err) {
      console.error("Failed to fetch facilities:", err);
      setFacilities([]); // Ensure facilities is always an array
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleFileChange = (e) => {
    const selectedFiles = e.target.files ? Array.from(e.target.files) : [];
    if (selectedFiles.length === 0) {
      setFiles([]);
      setFileError(null);
      return;
    }

    // Check max 3 attachments
    if (selectedFiles.length > 3) {
      setFileError("Maximum 3 attachments allowed. You selected " + selectedFiles.length);
      setFiles([]);
      return;
    }

    // Validate each file
    const validMimes = ["image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"];
    const validFiles = [];
    let hasErrors = false;

    for (const selectedFile of selectedFiles) {
      // Validate file size (5MB max)
      if (selectedFile.size > 5 * 1024 * 1024) {
        setFileError(`File "${selectedFile.name}" exceeds 5MB limit`);
        hasErrors = true;
        break;
      }

      // Validate MIME type
      if (!validMimes.includes(selectedFile.type)) {
        setFileError(`File "${selectedFile.name}": MIME type not allowed. Only images (JPEG, PNG, GIF, WebP) and PDF are allowed`);
        hasErrors = true;
        break;
      }

      validFiles.push(selectedFile);
    }

    if (hasErrors) {
      setFiles([]);
      return;
    }

    setFiles(validFiles);
    setFileError(null);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    const droppedFiles = e.dataTransfer.files ? Array.from(e.dataTransfer.files) : [];
    if (droppedFiles.length > 0) {
      handleFileChange({ target: { files: droppedFiles } });
    }
  };

  const validateForm = () => {
    if (!formData.facilityId) {
      setError("Please select a facility");
      return false;
    }
    if (!formData.title || formData.title.length < 20) {
      setError("Title must be at least 20 characters");
      return false;
    }
    if (formData.title.length > 200) {
      setError("Title must not exceed 200 characters");
      return false;
    }
    if (!formData.description || formData.description.length < 50) {
      setError("Description must be at least 50 characters");
      return false;
    }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);

      // Step 1: Create ticket with JSON request
      const ticketRequest = {
        facilityId: formData.facilityId,
        category: formData.category,
        priority: formData.priority,
        title: formData.title,
        description: formData.description,
      };

      const ticketResponse = await apiClient.post("/tickets", ticketRequest);
      const newTicketId = ticketResponse.data.id;

      // Step 2: Upload files separately if provided
      if (files && files.length > 0) {
        for (const file of files) {
          try {
            const fileFormData = new FormData();
            fileFormData.append("file", file);

            await apiClient.post(`/tickets/${newTicketId}/attachments`, fileFormData, {
              headers: {
                "Content-Type": "multipart/form-data",
              },
            });
          } catch (fileErr) {
            console.warn(`File upload failed for "${file.name}" but ticket was created:`, fileErr);
            // Don't fail the whole operation if file upload fails
            // The ticket was created successfully
          }
        }
      }

      // Reset form and notify parent
      setFormData({
        facilityId: "",
        category: "ELECTRICAL",
        priority: "MEDIUM",
        title: "",
        description: "",
      });
      setFiles([]);
      onTicketCreated();
    } catch (err) {
      console.error("Failed to create ticket:", err);
      console.error("Error response:", err.response?.data);
      
      // Construct detailed error message from various possible response formats
      let errorMessage = "Failed to create ticket. Please try again.";
      
      if (err.response?.data?.message) {
        errorMessage = err.response.data.message;
      } else if (err.response?.data?.violations) {
        // Validation errors from backend
        errorMessage = err.response.data.violations
          .map((v) => `${v.propertyPath}: ${v.message}`)
          .join("; ");
      } else if (err.response?.data?.errors) {
        errorMessage = err.response.data.errors
          .map((e) => `${e.field}: ${e.message}`)
          .join("; ");
      } else if (err.message) {
        errorMessage = err.message;
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h3 className="text-lg font-semibold text-gray-900">Create New Ticket</h3>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        {/* Facility Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Facility
          </label>
          <select
            name="facilityId"
            value={formData.facilityId}
            onChange={handleInputChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">Select a facility</option>
            {facilities.map((facility) => (
              <option key={facility.id} value={facility.id}>
                {facility.name}
              </option>
            ))}
          </select>
        </div>

        {/* Category Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Category
          </label>
          <select
            name="category"
            value={formData.category}
            onChange={handleInputChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
        </div>

        {/* Priority Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Priority
          </label>
          <select
            name="priority"
            value={formData.priority}
            onChange={handleInputChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {PRIORITIES.map((pri) => (
              <option key={pri} value={pri}>
                {pri}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Title */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Title ({formData.title.length}/200)
        </label>
        <input
          type="text"
          name="title"
          value={formData.title}
          onChange={handleInputChange}
          placeholder="Brief description of the issue (20-200 chars)"
          maxLength="200"
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        {formData.title.length < 20 && (
          <p className="text-xs text-red-600 mt-1">
            Minimum 20 characters required
          </p>
        )}
      </div>

      {/* Description */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Description ({formData.description.length} chars)
        </label>
        <textarea
          name="description"
          value={formData.description}
          onChange={handleInputChange}
          placeholder="Detailed description of the issue (50+ chars)"
          rows="4"
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        {formData.description.length < 50 && (
          <p className="text-xs text-red-600 mt-1">
            Minimum 50 characters required
          </p>
        )}
      </div>

      {/* File Upload */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Attachments (Optional, Max 3, 5MB each)
        </label>
        <div
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-indigo-500 transition cursor-pointer"
        >
          <input
            type="file"
            onChange={handleFileChange}
            accept="image/jpeg,image/png,image/gif,image/webp,application/pdf"
            multiple
            className="hidden"
            id="file-input"
          />
          <label htmlFor="file-input" className="cursor-pointer">
            <p className="text-gray-600">
              {files.length > 0 ? `${files.length} file(s) selected` : "Drag & drop or click to select files"}
            </p>
            <p className="text-xs text-gray-500 mt-1">
              Max 3 files, 5MB each | PNG, JPG, GIF, WebP, PDF
            </p>
          </label>
        </div>
        {fileError && (
          <p className="text-xs text-red-600 mt-2">{fileError}</p>
        )}
        {files.length > 0 && (
          <div className="mt-3 space-y-2">
            <p className="text-sm font-medium text-gray-700">Selected files:</p>
            {files.map((f, idx) => (
              <div key={idx} className="flex justify-between items-center bg-blue-50 p-2 rounded text-sm">
                <span className="text-gray-700">{f.name} ({(f.size / 1024).toFixed(1)} KB)</span>
                <button
                  type="button"
                  onClick={() => {
                    setFiles(files.filter((_, i) => i !== idx));
                    setFileError(null);
                  }}
                  className="text-red-500 hover:text-red-700 text-xs font-semibold"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Buttons */}
      <div className="flex gap-3 justify-end pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "Creating..." : "Create Ticket"}
        </button>
      </div>
    </form>
  );
}

export default TicketDashboard;
