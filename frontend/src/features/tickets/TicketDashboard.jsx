import React, { useCallback, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient } from "../../services/apiClient";
import { AuthContext } from "../../contexts/AuthContext";

/**
 * Ticket Dashboard Component - Modern Minimalist Design
 * Redesigned to match a premium, clean dashboard aesthetic.
 */

const shortenTicketId = (fullId) => {
  if (!fullId) return "";
  return fullId.substring(0, 8);
};

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
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [showCreateForm, setShowCreateForm] = useState(false);

  const STATUS_TABS = ["ALL", "OPEN", "IN_PROGRESS", "ON_HOLD", "RESOLVED", "CLOSED", "REJECTED"];

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/tickets");
      let ticketsList = [];
      if (Array.isArray(response.data)) {
        ticketsList = response.data;
      } else if (response.data && Array.isArray(response.data.content)) {
        ticketsList = response.data.content;
      }
      setTickets(ticketsList || []);
    } catch (err) {
      console.error("Failed to fetch tickets:", err);
      setError("Failed to load tickets. Please try again.");
      setTickets([]);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = useCallback(() => {
    let filtered = tickets;
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (ticket) =>
          ticket.title?.toLowerCase().includes(term) ||
          ticket.description?.toLowerCase().includes(term) ||
          ticket.id?.toString().includes(term),
      );
    }
    if (statusFilter !== "ALL") {
      filtered = filtered.filter((ticket) => ticket.status === statusFilter);
    }
    setFilteredTickets(filtered);
  }, [tickets, searchTerm, statusFilter]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  // Group tickets by status to display them in sections like the reference
  const groupedTickets = {
    TODO: filteredTickets.filter(t => t.status === 'OPEN'),
    IN_PROGRESS: filteredTickets.filter(t => t.status === 'IN_PROGRESS'),
    ON_HOLD: filteredTickets.filter(t => t.status === 'ON_HOLD'),
    RESOLVED_CLOSED: filteredTickets.filter(t => ['RESOLVED', 'CLOSED'].includes(t.status)),
    OTHER: filteredTickets.filter(t => t.status === 'REJECTED')
  };

  const getStatusPill = (status) => {
    switch (status) {
      case "OPEN":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#f1f5f9] text-[#475569]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#475569]"></span>
            Open
          </span>
        );
      case "IN_PROGRESS":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#e0e7ff] text-[#4338ca]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#4338ca]"></span>
            In Progress
          </span>
        );
      case "ON_HOLD":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#fff3e0] text-[#ef6c00]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ef6c00]"></span>
            On Hold
          </span>
        );
      case "RESOLVED":
      case "CLOSED":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#e8f5e9] text-[#2e7d32]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#2e7d32]"></span>
            {status === "CLOSED" ? "Closed" : "Resolved"}
          </span>
        );
      case "REJECTED":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#ffebee] text-[#c62828]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#c62828]"></span>
            Rejected
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-700">
            {status}
          </span>
        );
    }
  };

  const getPriorityPill = (priority) => {
    switch (priority) {
      case "LOW":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#f1f5f9] text-[#64748b]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#64748b] mr-1.5"></span> Low
          </span>
        );
      case "MEDIUM":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#fff7ed] text-[#ea580c]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ea580c] mr-1.5"></span> Medium
          </span>
        );
      case "HIGH":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#fef2f2] text-[#ef4444]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ef4444] mr-1.5"></span> High
          </span>
        );
      case "CRITICAL":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#7f1d1d] text-white">
            <span className="w-1.5 h-1.5 rounded-full bg-white mr-1.5"></span> Critical
          </span>
        );
      default:
        return <span className="text-gray-500 text-xs">{priority}</span>;
    }
  };

  const getTypePill = (type) => {
    return (
      <span className="inline-flex items-center px-2 py-1 rounded border border-[#fce7f3] bg-[#fdf2f8] text-[#db2777] text-xs font-medium">
        <svg className="w-3 h-3 mr-1 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path></svg>
        {type?.replace(/_/g, " ") || "Ticket"}
      </span>
    );
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    return new Date(dateString).toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric"
    });
  };

  const renderSection = (title, count, items) => {
    if (items.length === 0) return null;
    return (
      <div className="mb-6">
        <div className="flex items-center gap-2 mb-3 px-2">
          <button className="text-[#94a3b8] hover:text-[#0f172a] transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
          </button>
          <h2 className="text-sm font-semibold text-[#0f172a]">{title}</h2>
          <span className="flex items-center justify-center bg-[#f1f5f9] text-[#64748b] text-[10px] font-bold h-5 w-5 rounded-full">
            {count}
          </span>
          <div className="ml-auto">
            <button className="text-[#94a3b8] hover:text-[#0f172a] transition-colors">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
            </button>
          </div>
        </div>

        <div className="bg-white border border-[#e2e8f0] rounded-xl overflow-hidden shadow-sm">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-[#f8fafc] text-[#64748b] font-medium border-b border-[#e2e8f0]">
              <tr>
                <th className="px-4 py-3 w-10 text-center">
                  <input type="checkbox" className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                </th>
                <th className="px-4 py-3 font-medium">Ticket Name</th>
                <th className="px-4 py-3 font-medium">Description</th>
                <th className="px-4 py-3 font-medium">Created Date</th>
                <th className="px-4 py-3 font-medium">Category</th>
                <th className="px-4 py-3 font-medium">Assignee</th>
                <th className="px-4 py-3 font-medium">Priority</th>
                <th className="px-4 py-3 w-10 text-center"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e2e8f0]">
              {items.map((ticket) => (
                <tr 
                  key={ticket.id} 
                  className="hover:bg-[#f8fafc] transition-colors group cursor-pointer"
                  onClick={() => navigate(`/tickets/${ticket.id}`)}
                >
                  <td className="px-4 py-4 text-center" onClick={(e) => e.stopPropagation()}>
                    <input type="checkbox" className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-md bg-[#f1f5f9] border border-[#e2e8f0] flex items-center justify-center text-[#64748b]">
                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path></svg>
                      </div>
                      <span className="font-medium text-[#0f172a]">{ticket.title}</span>
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <span className="text-[#64748b] truncate max-w-[200px] inline-block">{ticket.description || "No description"}</span>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-1.5 text-[#475569]">
                      <svg className="w-4 h-4 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                      {formatDate(ticket.createdAt)}
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    {getTypePill(ticket.category)}
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-2">
                      {ticket.assignedTechnicianName ? (
                        <>
                          <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-[10px] font-bold">
                            {ticket.assignedTechnicianName.charAt(0)}
                          </div>
                          <span className="text-sm text-[#475569]">{ticket.assignedTechnicianName}</span>
                        </>
                      ) : (
                        <span className="text-sm text-[#94a3b8] italic">Unassigned</span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    {getPriorityPill(ticket.priority)}
                  </td>
                  <td className="px-4 py-4 text-center">
                    <button 
                      className="text-[#94a3b8] hover:text-[#0f172a] opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => {
                        e.stopPropagation();
                        copyToClipboard(ticket.id);
                      }}
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 12h.01M12 12h.01M19 12h.01M6 12a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0z"></path></svg>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#94a3b8] font-medium flex items-center gap-2">
          <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Loading tickets...
        </div>
      </div>
    );
  }

  return (
    <div className="w-full font-sans">
      {/* Top Header Section */}
      <div className="mb-8 flex justify-between items-end">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-[#4f46e5] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            T
          </div>
          <div>
            <h1 className="text-2xl font-bold text-[#0f172a] tracking-tight">Support Tickets</h1>
            <p className="text-sm text-[#64748b]">Manage and track your maintenance requests</p>
          </div>
        </div>
        
        <div className="flex gap-3">
          {(hasRole("USER") || hasRole("STUDENT") || hasRole("LECTURER") || hasRole("ADMIN")) && (
            <button
              onClick={() => setShowCreateForm(!showCreateForm)}
              className="px-4 py-2 bg-[#0f172a] text-white text-sm font-medium rounded-lg hover:bg-[#1e293b] transition-colors flex items-center gap-2"
            >
              {showCreateForm ? (
                <>Cancel</>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
                  New Ticket
                </>
              )}
            </button>
          )}
        </div>
      </div>

      {/* Create Ticket Modal / Form Section */}
      {showCreateForm && (
        <div className="mb-8 bg-white rounded-xl border border-[#e2e8f0] p-6 shadow-sm animate-in fade-in slide-in-from-top-4 duration-200">
          <TicketCreationForm
            onTicketCreated={() => {
              setShowCreateForm(false);
              fetchTickets();
            }}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Tabs and Controls */}
      <div className="flex items-center justify-between mb-6 border-b border-[#e2e8f0] pb-px">
        <div className="flex items-center gap-6 overflow-x-auto">
          {STATUS_TABS.map((status) => {
            const isActive = statusFilter === status;
            return (
              <button
                key={status}
                onClick={() => setStatusFilter(status)}
                className={`pb-3 text-sm font-medium border-b-2 whitespace-nowrap transition-colors ${
                  isActive
                    ? "text-[#0f172a] border-[#0f172a]"
                    : "text-[#64748b] border-transparent hover:text-[#475569]"
                }`}
              >
                {status === "ALL" ? "All Tickets" : status.split('_').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')}
              </button>
            );
          })}
        </div>
        
        <div className="flex items-center gap-3 pb-2">
          <div className="relative">
            <svg className="w-4 h-4 text-[#94a3b8] absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            <input
              type="text"
              placeholder="Search..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9 pr-4 py-1.5 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 w-48"
            />
          </div>
          <button className="flex items-center gap-2 px-3 py-1.5 text-sm border border-[#e2e8f0] rounded-lg text-[#475569] hover:bg-[#f8fafc] transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path></svg>
            Filter
          </button>
        </div>
      </div>

      {/* Errors */}
      {error && (
        <div className="mb-6 rounded-lg bg-red-50 border border-red-200 p-4 flex items-center gap-3">
          <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-medium text-red-800">{error}</p>
        </div>
      )}

      {/* Lists */}
      {filteredTickets.length === 0 ? (
        <div className="text-center py-16 bg-white border border-[#e2e8f0] rounded-xl border-dashed">
          <div className="w-16 h-16 bg-[#f1f5f9] rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-[#0f172a] mb-1">No tickets found</h3>
          <p className="text-[#64748b] text-sm">
            {statusFilter === "ALL" && !searchTerm ? "No tickets yet. Create one to get started." : "Try adjusting your filters or search term."}
          </p>
        </div>
      ) : statusFilter === "ALL" ? (
        <div className="space-y-2">
          {renderSection("To-do", groupedTickets.TODO.length, groupedTickets.TODO)}
          {renderSection("On Progress", groupedTickets.IN_PROGRESS.length, groupedTickets.IN_PROGRESS)}
          {renderSection("On Hold", groupedTickets.ON_HOLD.length, groupedTickets.ON_HOLD)}
          {renderSection("Resolved / Closed", groupedTickets.RESOLVED_CLOSED.length, groupedTickets.RESOLVED_CLOSED)}
          {renderSection("Others", groupedTickets.OTHER.length, groupedTickets.OTHER)}
        </div>
      ) : (
        <div className="space-y-2">
          {renderSection(
            statusFilter.split('_').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' '), 
            filteredTickets.length, 
            filteredTickets
          )}
        </div>
      )}
    </div>
  );
}

/**
 * Ticket Creation Form Component
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

  const CATEGORIES = ["ELECTRICAL", "PLUMBING", "HVAC", "IT_NETWORKING", "STRUCTURAL", "CLEANING", "SAFETY", "OTHER"];
  const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

  useEffect(() => {
    fetchFacilities();
  }, []);

  const fetchFacilities = async () => {
    try {
      const response = await apiClient.get("/facilities");
      let facilitiesList = [];
      if (response.data && Array.isArray(response.data.content)) {
        facilitiesList = response.data.content;
      } else if (Array.isArray(response.data)) {
        facilitiesList = response.data;
      } else if (response.data && Array.isArray(response.data.items)) {
        facilitiesList = response.data.items;
      }
      setFacilities(facilitiesList || []);
    } catch (err) {
      console.error("Failed to fetch facilities:", err);
      setFacilities([]);
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
    if (selectedFiles.length > 3) {
      setFileError("Maximum 3 attachments allowed. You selected " + selectedFiles.length);
      setFiles([]);
      return;
    }
    const validMimes = ["image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"];
    const validFiles = [];
    let hasErrors = false;
    for (const selectedFile of selectedFiles) {
      if (selectedFile.size > 5 * 1024 * 1024) {
        setFileError(`File "${selectedFile.name}" exceeds 5MB limit`);
        hasErrors = true;
        break;
      }
      if (!validMimes.includes(selectedFile.type)) {
        setFileError(`File "${selectedFile.name}": MIME type not allowed.`);
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

  const validateForm = () => {
    if (!formData.facilityId) { setError("Please select a facility"); return false; }
    if (!formData.title || formData.title.length < 20) { setError("Title must be at least 20 characters"); return false; }
    if (formData.title.length > 200) { setError("Title must not exceed 200 characters"); return false; }
    if (!formData.description || formData.description.length < 50) { setError("Description must be at least 50 characters"); return false; }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!validateForm()) return;

    try {
      setLoading(true);
      const ticketRequest = {
        facilityId: formData.facilityId,
        category: formData.category,
        priority: formData.priority,
        title: formData.title,
        description: formData.description,
      };

      const ticketResponse = await apiClient.post("/tickets", ticketRequest);
      const newTicketId = ticketResponse.data.id;

      if (files && files.length > 0) {
        for (const file of files) {
          try {
            const fileFormData = new FormData();
            fileFormData.append("file", file);
            await apiClient.post(`/tickets/${newTicketId}/attachments`, fileFormData, {
              headers: { "Content-Type": "multipart/form-data" },
            });
          } catch (fileErr) {
            console.warn(`File upload failed for "${file.name}"`, fileErr);
          }
        }
      }

      setFormData({
        facilityId: "", category: "ELECTRICAL", priority: "MEDIUM", title: "", description: "",
      });
      setFiles([]);
      onTicketCreated();
    } catch (err) {
      console.error("Failed to create ticket:", err);
      let errorMessage = "Failed to create ticket. Please try again.";
      if (err.response?.data?.message) {
        errorMessage = err.response.data.message;
      } else if (err.response?.data?.violations) {
        errorMessage = err.response.data.violations.map((v) => `${v.propertyPath}: ${v.message}`).join("; ");
      } else if (err.response?.data?.errors) {
        errorMessage = err.response.data.errors.map((e) => `${e.field}: ${e.message}`).join("; ");
      } else if (err.message) {
        errorMessage = err.message;
      }
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="flex justify-between items-center border-b border-[#e2e8f0] pb-4">
        <h3 className="text-lg font-semibold text-[#0f172a]">Create New Ticket</h3>
        <button type="button" onClick={onCancel} className="text-[#64748b] hover:text-[#0f172a]">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-sm text-red-700">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div>
          <label className="block text-sm font-medium text-[#0f172a] mb-1">Facility</label>
          <select
            name="facilityId"
            value={formData.facilityId}
            onChange={handleInputChange}
            required
            className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">Select a facility</option>
            {facilities.map((facility) => (
              <option key={facility.id} value={facility.id}>{facility.name}</option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#0f172a] mb-1">Category</label>
            <select
              name="category"
              value={formData.category}
              onChange={handleInputChange}
              className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
            >
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
              onChange={handleInputChange}
              className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              {PRIORITIES.map((pri) => (
                <option key={pri} value={pri}>{pri}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-[#0f172a] mb-1">
          Title <span className="text-[#94a3b8] font-normal text-xs">({formData.title.length}/200)</span>
        </label>
        <input
          type="text"
          name="title"
          value={formData.title}
          onChange={handleInputChange}
          placeholder="Add title here"
          maxLength="200"
          className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-[#0f172a] mb-1">
          Description
        </label>
        <textarea
          name="description"
          value={formData.description}
          onChange={handleInputChange}
          placeholder="Detailed description of the issue (10+ chars)"
          rows="3"
          className="w-full px-3 py-2 text-sm border border-[#e2e8f0] rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        {formData.description.length > 0 && formData.description.length < 10 && (
          <p className="text-xs text-red-600 mt-1">Minimum 10 characters required</p>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium text-[#0f172a] mb-2">Attachments <span className="text-[#94a3b8] font-normal text-xs">(Optional, Max 3, 5MB each)</span></label>
        <div className="border border-dashed border-[#cbd5e1] bg-[#f8fafc] rounded-lg p-4 text-center hover:bg-[#f1f5f9] transition cursor-pointer">
          <input
            type="file"
            onChange={handleFileChange}
            accept="image/jpeg,image/png,image/gif,image/webp,application/pdf"
            multiple
            className="hidden"
            id="file-input"
          />
          <label htmlFor="file-input" className="cursor-pointer flex flex-col items-center">
            <svg className="w-6 h-6 text-[#94a3b8] mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
            <p className="text-sm text-[#475569] font-medium">
              {files.length > 0 ? `${files.length} file(s) selected` : "Click or drag files to upload"}
            </p>
            <p className="text-[10px] text-[#94a3b8] mt-1">PNG, JPG, GIF, WebP, PDF up to 5MB</p>
          </label>
        </div>
        {fileError && <p className="text-xs text-red-600 mt-2">{fileError}</p>}
        {files.length > 0 && (
          <div className="mt-3 space-y-2">
            {files.map((f, idx) => (
              <div key={idx} className="flex justify-between items-center bg-[#f1f5f9] px-3 py-2 rounded-md text-sm border border-[#e2e8f0]">
                <div className="flex items-center gap-2">
                  <svg className="w-4 h-4 text-[#64748b]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path></svg>
                  <span className="text-[#334155] font-medium truncate max-w-[200px]">{f.name}</span>
                  <span className="text-[#94a3b8] text-xs">({(f.size / 1024).toFixed(1)} KB)</span>
                </div>
                <button
                  type="button"
                  onClick={() => { setFiles(files.filter((_, i) => i !== idx)); setFileError(null); }}
                  className="text-[#ef4444] hover:text-[#b91c1c] p-1"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-sm font-medium border border-[#e2e8f0] rounded-lg text-[#475569] hover:bg-[#f8fafc] transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2 text-sm font-medium bg-[#0f172a] text-white rounded-lg hover:bg-[#1e293b] transition-colors disabled:opacity-50 flex items-center gap-2"
        >
          {loading && <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>}
          {loading ? "Creating..." : "Create Ticket"}
        </button>
      </div>
    </form>
  );
}

export default TicketDashboard;
