import React, { useCallback, useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";
import TicketAssignmentDialog from "./TicketAssignmentDialog";
import TicketStatusUpdate from "./TicketStatusUpdate";
import { TicketEditModal } from "./TicketEditModal";
import { ManualEscalationDialog } from "./ManualEscalationDialog";

/**
 * Ticket Detail View Component
 * Displays full ticket information with comments, attachments, and escalation history
 * Implements US4 requirement: Ticket detail view with comments, attachments, and SLA display
 */

// Utility function to shorten UUID
const shortenTicketId = (fullId) => {
  if (!fullId) return "";
  return fullId.substring(0, 8);
};

// Utility function to copy to clipboard
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    // Could add a toast notification here
    console.log("Copied to clipboard:", text);
  });
};

export function TicketDetailView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAssignmentDialog, setShowAssignmentDialog] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showEscalationDialog, setShowEscalationDialog] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [activeTab, setActiveTab] = useState("details");

  const fetchTicket = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      console.log("fetchTicket called - fetching ticket data");
      const response = await apiClient.get(`/tickets/${id}`);
      console.log("Ticket data fetched:", response.data);
      console.log("Attachments count:", response.data?.attachments?.length);
      setTicket(response.data);
    } catch (err) {
      console.error("Failed to fetch ticket:", err);
      setError(
        err.response?.data?.message ||
          "Failed to load ticket details. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchTicket();
  }, [fetchTicket]);

  const handleDeleteTicket = async () => {
    if (!window.confirm("Are you sure you want to delete this ticket? This action cannot be undone.")) {
      return;
    }

    try {
      setDeleteLoading(true);
      await apiClient.delete(`/tickets/${id}`);
      navigate("/tickets");
    } catch (err) {
      console.error("Failed to delete ticket:", err);
      alert(err.response?.data?.message || "Failed to delete ticket. Please try again.");
      setDeleteLoading(false);
    }
  };

  const handleTicketUpdated = (updatedTicket) => {
    setTicket(updatedTicket);
  };

  const isStaff = user?.roles?.some((role) =>
    ["FACILITY_MANAGER", "ADMIN", "TECHNICIAN"].includes(role),
  );

  // Debug logging
  console.log("TicketDetailView - User:", user);
  console.log("TicketDetailView - User roles:", user?.roles);
  console.log("TicketDetailView - isStaff:", isStaff);

  const canAssign =
    user?.roles?.includes("ADMIN") ||
    (user?.roles?.includes("FACILITY_MANAGER") && user?.facilityId === ticket?.facilityId);

  const canUpdateStatus =
    user?.roles?.includes("ADMIN") ||
    (user?.roles?.includes("FACILITY_MANAGER") && user?.facilityId === ticket?.facilityId) ||
    (user?.roles?.includes("TECHNICIAN") && user?.id === ticket?.assignedTechnicianId);

  const canEdit =
    ticket?.status === "OPEN" &&
    (user?.id === ticket?.createdById || user?.roles?.includes("ADMIN"));

  const canDelete =
    ticket?.status === "OPEN" &&
    (user?.id === ticket?.createdById || user?.roles?.includes("ADMIN"));

  const formatDate = (dateString) => {
    if (!dateString) return "";
    return new Date(dateString).toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatEscalationLevel = (level) => {
    if (!level) return "Unknown";
    // Convert LEVEL_1 to Level 1, LEVEL_2 to Level 2, etc.
    return level.replace(/^LEVEL_/, "Level ");
  };

  const getStatusColor = (status) => {
    const colors = {
      OPEN: "bg-blue-100 text-blue-800",
      IN_PROGRESS: "bg-purple-100 text-purple-800",
      RESOLVED: "bg-green-100 text-green-800",
      CLOSED: "bg-gray-100 text-gray-800",
      REJECTED: "bg-red-100 text-red-800",
    };
    return colors[status] || "bg-gray-100 text-gray-800";
  };

  const getPriorityColor = (priority) => {
    const colors = {
      LOW: "bg-green-100 text-green-800",
      MEDIUM: "bg-blue-100 text-blue-800",
      HIGH: "bg-orange-100 text-orange-800",
      CRITICAL: "bg-red-100 text-red-800",
    };
    return colors[priority] || "bg-gray-100 text-gray-800";
  };

  const getEscalationColor = (level) => {
    const colors = {
      1: "bg-green-50 border-green-200 text-green-800",
      2: "bg-yellow-50 border-yellow-200 text-yellow-800",
      3: "bg-orange-50 border-orange-200 text-orange-800",
      4: "bg-red-50 border-red-200 text-red-800",
    };
    return colors[level] || "bg-gray-50 border-gray-200";
  };

  const getSLADeadlineStatus = (deadline) => {
    if (!deadline) return "N/A";
    const now = new Date();
    const deadlineDate = new Date(deadline);
    const minutesRemaining = (deadlineDate - now) / (1000 * 60);

    if (minutesRemaining < 0) {
      return `Breached by ${Math.abs(Math.floor(minutesRemaining))} minutes`;
    }

    if (minutesRemaining < 60) {
      return `${Math.floor(minutesRemaining)} minutes remaining`;
    }

    const hoursRemaining = Math.floor(minutesRemaining / 60);
    return `${hoursRemaining}h remaining`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (error || !ticket) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <button
          onClick={() => navigate("/tickets")}
          className="text-indigo-600 hover:text-indigo-900 mb-4"
        >
          Back to Tickets
        </button>
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-red-700">{error || "Ticket not found"}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <button
          onClick={() => navigate("/tickets")}
          className="text-indigo-600 hover:text-indigo-900 mb-4 flex items-center gap-1"
        >
          Back to Tickets
        </button>

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <h1 className="text-3xl font-bold text-gray-900">
                  #{shortenTicketId(ticket.id)}
                </h1>
                <button
                  onClick={() => copyToClipboard(ticket.id)}
                  title="Copy full ticket ID"
                  className="p-1 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded transition"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                  </svg>
                </button>
              </div>
              <h2 className="text-lg font-semibold text-gray-700 mb-2">{ticket.title}</h2>
              <p className="text-gray-600">{ticket.description}</p>
            </div>
            <div className="flex gap-2">
              {canAssign && (
                <button
                  onClick={() => setShowAssignmentDialog(true)}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition text-sm"
                >
                  Assign Technician
                </button>
              )}
              {isStaff && (
                <button
                  onClick={() => setShowEscalationDialog(true)}
                  className="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition text-sm flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  Escalate
                </button>
              )}
              {canEdit && (
                <button
                  onClick={() => setShowEditModal(true)}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition text-sm flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  Edit
                </button>
              )}
              {canDelete && (
                <button
                  onClick={handleDeleteTicket}
                  disabled={deleteLoading}
                  className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition text-sm flex items-center gap-2 disabled:opacity-50"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  {deleteLoading ? "Deleting..." : "Delete"}
                </button>
              )}
            </div>
          </div>

          {/* Status Badges */}
          <div className="flex gap-2 flex-wrap">
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(ticket.status)}`}>
              {ticket.status}
            </span>
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getPriorityColor(ticket.priority)}`}>
              {ticket.priority} Priority
            </span>
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
              {ticket.category}
            </span>
          </div>

          {/* Rejection Reason (if ticket was rejected) */}
          {ticket.status === "REJECTED" && (
            <div className="mt-4 p-4 rounded-lg border border-red-200 bg-red-50">
              <h3 className="text-sm font-semibold text-red-800 mb-2">Rejection Reason</h3>
              <p className="text-red-700 text-sm">
                {ticket.rejectionReason || "No reason provided"}
              </p>
            </div>
          )}

          {/* SLA Deadline (if available) */}
          {ticket.slaDeadline && (
            <div className={`mt-4 p-3 rounded-lg border ${getEscalationColor(ticket.escalationLevel)}`}>
              <p className="text-sm font-semibold">
                SLA Deadline: {formatDate(ticket.slaDeadline)}
              </p>
              <p className="text-xs mt-1">{getSLADeadlineStatus(ticket.slaDeadline)}</p>
              {ticket.escalationLevel && (
                <p className="text-xs mt-1">
                  Escalation Level: {ticket.escalationLevel}
                </p>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-3 gap-6">
        {/* Left Column: Details */}
        <div className="col-span-3 lg:col-span-2 space-y-6">
          {/* Tab Navigation */}
          <div className="bg-white rounded-lg border border-gray-200">
            <div className="flex border-b border-gray-200">
              {["details", "comments", "attachments"].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`flex-1 px-4 py-3 text-sm font-medium transition ${
                    activeTab === tab
                      ? "text-indigo-600 border-b-2 border-indigo-600"
                      : "text-gray-700 hover:text-gray-900"
                  }`}
                >
                  {tab.charAt(0).toUpperCase() + tab.slice(1)}
                </button>
              ))}
            </div>

            <div className="p-6">
              {/* Details Tab */}
              {activeTab === "details" && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="text-sm font-medium text-gray-600">
                        Facility
                      </label>
                      <p className="text-gray-900 mt-1">{ticket.facilityName}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">
                        Created By
                      </label>
                      <p className="text-gray-900 mt-1">{ticket.createdByName}</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="text-sm font-medium text-gray-600">
                        Created Date
                      </label>
                      <p className="text-gray-900 mt-1">
                        {formatDate(ticket.createdAt)}
                      </p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">
                        Assigned Technician
                      </label>
                      <p className="text-gray-900 mt-1">
                        {ticket.assignedTechnicianName || "Not assigned"}
                      </p>
                    </div>
                  </div>

                  {/* Status Update Section */}
                  {canUpdateStatus && (
                    <div className="mt-6 pt-6 border-t border-gray-200">
                      <TicketStatusUpdate
                        ticketId={ticket.id}
                        currentStatus={ticket.status}
                        onStatusUpdated={fetchTicket}
                      />
                    </div>
                  )}
                </div>
              )}

              {/* Comments Tab */}
              {activeTab === "comments" && (
                <CommentsSection
                  ticketId={ticket.id}
                  onCommentAdded={fetchTicket}
                  isStaff={isStaff}
                />
              )}

              {/* Attachments Tab */}
              {activeTab === "attachments" && (
                <AttachmentsSection
                  ticketId={ticket.id}
                  ticket={ticket}
                  attachments={ticket.attachments}
                  user={user}
                  isAdmin={user?.roles?.includes("ADMIN")}
                  onAttachmentDeleted={fetchTicket}
                />
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Escalation History (Staff Only) */}
        {isStaff && (
          <div className="bg-white rounded-lg border border-gray-200 p-6 h-fit shadow-sm">
            <div className="flex items-center gap-2 mb-5">
              <svg className="w-5 h-5 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M5.293 7.293a1 1 0 011.414 0A4 4 0 1010 4a1 1 0 11-2 0 2 2 0 10-4 4 1 1 0 010 2z M10 18a8 8 0 100-16 8 8 0 000 16zm0-2a6 6 0 100-12 6 6 0 000 12z" />
              </svg>
              <h3 className="text-lg font-bold text-gray-900">Escalation History</h3>
            </div>
            {ticket.escalationHistory && ticket.escalationHistory.length > 0 ? (
              <div className="space-y-3">
                {ticket.escalationHistory.map((escalation, index) => {
                  const getLevelColors = (level) => {
                    const colors = {
                      1: { bg: "bg-gradient-to-br from-green-50 to-emerald-50", border: "border-green-300", badge: "bg-green-100 text-green-700", icon: "text-green-600" },
                      2: { bg: "bg-gradient-to-br from-yellow-50 to-amber-50", border: "border-yellow-300", badge: "bg-yellow-100 text-yellow-700", icon: "text-yellow-600" },
                      3: { bg: "bg-gradient-to-br from-orange-50 to-red-50", border: "border-orange-300", badge: "bg-orange-100 text-orange-700", icon: "text-orange-600" },
                      4: { bg: "bg-gradient-to-br from-red-50 to-red-100", border: "border-red-300", badge: "bg-red-100 text-red-700", icon: "text-red-600" },
                    };
                    return colors[level] || { bg: "bg-gray-50", border: "border-gray-300", badge: "bg-gray-100 text-gray-700", icon: "text-gray-600" };
                  };

                  const toColors = getLevelColors(escalation.toLevel);

                  return (
                    <div key={index} className={`${toColors.bg} border-l-4 ${toColors.border} rounded-lg p-4 transition-all hover:shadow-md`}>
                      {/* Escalation Arrow */}
                      <div className="flex items-center gap-2 mb-3">
                        <span className={`inline-block px-2.5 py-1 text-xs font-bold rounded-full ${toColors.badge}`}>
                          {formatEscalationLevel(escalation.fromLevel)}
                        </span>
                        <svg className={`w-4 h-4 ${toColors.icon}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                        </svg>
                        <span className={`inline-block px-2.5 py-1 text-xs font-bold rounded-full ${toColors.badge}`}>
                          {formatEscalationLevel(escalation.toLevel)}
                        </span>
                      </div>

                      {/* Details Grid */}
                      <div className="grid grid-cols-2 gap-3 text-xs">
                        <div>
                          <p className="text-gray-600 font-medium mb-0.5">When</p>
                          <p className="text-gray-900 font-semibold">{formatDate(escalation.escalatedAt)}</p>
                        </div>
                        <div>
                          <p className="text-gray-600 font-medium mb-0.5">By</p>
                          <p className="text-gray-900 font-semibold">{escalation.escalatedByName}</p>
                        </div>
                      </div>

                      {/* Reason */}
                      <div className="mt-3 pt-3 border-t border-gray-200">
                        <p className="text-gray-600 font-medium text-xs mb-1">Reason</p>
                        <p className="text-gray-700 text-sm leading-relaxed">{escalation.reason}</p>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-8">
                <svg className="w-10 h-10 text-gray-300 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p className="text-sm text-gray-500 font-medium">No escalations yet</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Assignment Dialog */}
      {showAssignmentDialog && (
        <TicketAssignmentDialog
          ticketId={ticket.id}
          facilityId={ticket.facilityId}
          onAssigned={() => {
            setShowAssignmentDialog(false);
            fetchTicket();
          }}
          onClose={() => setShowAssignmentDialog(false)}
        />
      )}

      {/* Edit Modal */}
      <TicketEditModal
        ticket={ticket}
        isOpen={showEditModal}
        onClose={() => setShowEditModal(false)}
        onTicketUpdated={handleTicketUpdated}
      />

      {/* Manual Escalation Dialog */}
      {showEscalationDialog && (
        <ManualEscalationDialog
          ticketId={ticket.id}
          facilityId={ticket.facilityId}
          onEscalationSuccess={() => {
            setShowEscalationDialog(false);
            fetchTicket();
          }}
          onCancel={() => setShowEscalationDialog(false)}
        />
      )}
    </div>
  );
}

/**
 * Comments Section Component
 */
function CommentsSection({ ticketId, onCommentAdded, isStaff }) {
  const { user } = useAuth();
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingContent, setEditingContent] = useState("");

  // Helper to check user roles (handles both string and array formats)
  const hasRole = (role) => {
    if (!user?.roles) return false;
    const userRoles = Array.isArray(user.roles) ? user.roles : [user.roles];
    return userRoles.includes(role);
  };

  const fetchComments = useCallback(async () => {
    try {
      setLoading(true);
      const response = await apiClient.get(`/tickets/${ticketId}/comments`);
      setComments(response.data || []);
    } catch (err) {
      console.error("Failed to fetch comments:", err);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handleAddComment = async (e) => {
    e.preventDefault();
    setError(null);

    if (!newComment.trim()) {
      setError("Comment cannot be empty");
      return;
    }

    if (newComment.length < 5 || newComment.length > 2000) {
      setError("Comment must be between 5 and 2000 characters");
      return;
    }

    try {
      setSubmitting(true);
      await apiClient.post(`/tickets/${ticketId}/comments`, {
        content: newComment,
        visibility: visibility,
      });

      setNewComment("");
      setVisibility("PUBLIC");
      fetchComments();
      onCommentAdded();
    } catch (err) {
      console.error("Failed to add comment:", err);
      setError(
        err.response?.data?.message ||
          "Failed to add comment. Please try again.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm("Are you sure you want to delete this comment?")) {
      return;
    }

    try {
      await apiClient.delete(`/tickets/${ticketId}/comments/${commentId}`);
      fetchComments();
    } catch (err) {
      console.error("Failed to delete comment:", err);
    }
  };

  const handleUpdateComment = async (commentId) => {
    if (!editingContent.trim()) {
      setError("Comment cannot be empty");
      return;
    }

    try {
      await apiClient.put(`/tickets/${ticketId}/comments/${commentId}`, {
        content: editingContent,
      });

      setEditingCommentId(null);
      setEditingContent("");
      fetchComments();
    } catch (err) {
      console.error("Failed to update comment:", err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Add Comment Form */}
      <form onSubmit={handleAddComment} className="space-y-3">
        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Add a comment... (5-2000 characters)"
          rows="3"
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />

        <div className="flex items-center justify-between">
          <div>
            {isStaff && (
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={visibility === "INTERNAL"}
                  onChange={(e) =>
                    setVisibility(e.target.checked ? "INTERNAL" : "PUBLIC")
                  }
                  className="rounded"
                />
                <span className="text-sm text-gray-600">Internal comment</span>
              </label>
            )}
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed text-sm"
          >
            {submitting ? "Posting..." : "Post Comment"}
          </button>
        </div>
      </form>

      {/* Comments List */}
      <div className="space-y-4">
        {comments.length === 0 ? (
          <p className="text-center text-gray-500 py-8">No comments yet</p>
        ) : (
          comments.map((comment) => {
            // Check if this is a solution summary comment
            const isSolutionSummary = comment.content.startsWith("**SOLUTION:**");
            const displayContent = isSolutionSummary 
              ? comment.content.replace("**SOLUTION:** ", "").trim()
              : comment.content;

            return (
            <div
              key={comment.id}
              className={`p-4 rounded-lg border ${
                isSolutionSummary
                  ? "bg-gradient-to-r from-green-50 to-emerald-50 border-emerald-300 border-l-4 border-l-emerald-500 shadow-sm"
                  : comment.visibility === "INTERNAL"
                  ? "bg-yellow-50 border-yellow-200"
                  : "bg-gray-50 border-gray-200"
              }`}
            >
              <div className="flex items-start justify-between mb-2">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-gray-900">
                      {comment.authorName}
                    </p>
                    {isSolutionSummary && (
                      <span className="flex items-center gap-1 text-xs px-2 py-1 bg-emerald-100 text-emerald-700 rounded-full font-semibold">
                        <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                        Solution
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-600">
                    {new Date(comment.createdAt).toLocaleString()}
                    {comment.isEdited && " (edited)"}
                  </p>
                </div>
                {!isSolutionSummary && comment.visibility === "INTERNAL" && (
                  <span className="text-xs px-2 py-1 bg-yellow-200 text-yellow-800 rounded">
                    Internal
                  </span>
                )}
              </div>

              {editingCommentId === comment.id ? (
                <div className="space-y-2">
                  <textarea
                    value={editingContent}
                    onChange={(e) => setEditingContent(e.target.value)}
                    rows="2"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-sm"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() =>
                        handleUpdateComment(comment.id)}
                      className="px-3 py-1 bg-emerald-600 text-white rounded text-sm hover:bg-emerald-700"
                    >
                      Save
                    </button>
                    <button
                      onClick={() => setEditingCommentId(null)}
                      className="px-3 py-1 bg-gray-300 text-gray-700 rounded text-sm hover:bg-gray-400"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <p className={`text-sm ${isSolutionSummary ? "text-emerald-900 font-medium leading-relaxed" : "text-gray-700"}`}>
                    {displayContent}
                  </p>
                  {(() => {
                    // Compare IDs as strings for consistency
                    const userIdStr = user?.id?.toString();
                    const authorIdStr = comment.authorId?.toString();
                    const isAuthor = userIdStr && authorIdStr && userIdStr === authorIdStr;
                    const isAdmin = hasRole("ADMIN");
                    const isTechnician = hasRole("TECHNICIAN");
                    
                    // Check if comment author is admin
                    const authorRoles = comment.authorRoles || [];
                    const authorIsAdmin = authorRoles.includes("ADMIN");
                    
                    const canEdit = isAuthor; // Only author can edit
                    // Technician cannot delete admin comments
                    const canDelete = isAuthor || isAdmin || (isTechnician && !authorIsAdmin);
                    
                    if (canEdit || canDelete) {
                      return (
                        <div className="flex gap-2 mt-3">
                          {canEdit && (
                            <button
                              onClick={() => {
                                setEditingCommentId(comment.id);
                                setEditingContent(displayContent);
                              }}
                              className={`px-3 py-1 rounded text-sm transition ${
                                isSolutionSummary
                                  ? "bg-emerald-600 text-white hover:bg-emerald-700"
                                  : "bg-indigo-600 text-white hover:bg-indigo-700"
                              }`}
                            >
                              Edit
                            </button>
                          )}
                          {canDelete && (
                            <button
                              onClick={() =>
                                handleDeleteComment(comment.id)}
                              className="px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700 transition"
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      );
                    }
                    return null;
                  })()}
                </>
              )}
            </div>
            );
          })
        )}
      </div>
    </div>
  );
}

/**
 * Attachments Section Component
 */
function AttachmentsSection({ ticketId, ticket, attachments, user, isAdmin, onAttachmentDeleted }) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [deleting, setDeleting] = useState(null); // Track which attachment is being deleted
  const [file, setFile] = useState(null);
  const [attachmentType, setAttachmentType] = useState("PROBLEM"); // PROBLEM or SOLUTION

  // Helper to check user roles (handles both string and array formats)
  const hasRole = (role) => {
    if (!user?.roles) return false;
    const userRoles = Array.isArray(user.roles) ? user.roles : [user.roles];
    return userRoles.includes(role);
  };

  const handleFileChange = (e) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    if (selectedFile.size > 5 * 1024 * 1024) {
      setUploadError("File size must not exceed 5MB");
      setFile(null);
      return;
    }

    const validMimes = ["image/jpeg", "image/png", "image/gif", "application/pdf"];
    if (!validMimes.includes(selectedFile.type)) {
      setUploadError(
        "Only images (JPEG, PNG, GIF) and PDF files are allowed",
      );
      setFile(null);
      return;
    }

    setFile(selectedFile);
    setUploadError(null);
  };

  const handleUpload = async () => {
    if (!file) return;

    try {
      setUploading(true);
      const formData = new FormData();
      formData.append("file", file);
      formData.append("type", attachmentType); // Send attachment type

      await apiClient.post(`/tickets/${ticketId}/attachments`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      setFile(null);
      setAttachmentType("PROBLEM"); // Reset to default
      onAttachmentDeleted();
    } catch (err) {
      console.error("Failed to upload attachment:", err);
      const errorMessage = err.response?.data?.message || err.message;
      
      // Display user-friendly error messages
      let displayMessage = "Failed to upload file. Please try again.";
      if (errorMessage?.includes("maximum attachment limit")) {
        displayMessage = "Maximum 3 attachments allowed per ticket.";
      } else if (errorMessage?.includes("File size")) {
        displayMessage = errorMessage;
      } else if (errorMessage?.includes("MIME") || errorMessage?.includes("allowed")) {
        displayMessage = errorMessage;
      } else if (errorMessage) {
        displayMessage = errorMessage;
      }
      
      setUploadError(displayMessage);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (attachmentId) => {
    if (!window.confirm("Are you sure you want to delete this attachment?")) {
      return;
    }

    try {
      setDeleting(attachmentId);
      console.log("Starting delete for attachment:", attachmentId);
      const response = await apiClient.delete(`/tickets/${ticketId}/attachments/${attachmentId}`);
      console.log("Delete response:", response);
      console.log("Calling onAttachmentDeleted callback");
      onAttachmentDeleted();
      console.log("onAttachmentDeleted callback completed");
    } catch (err) {
      console.error("Failed to delete attachment:", err);
      console.error("Error response:", err.response);
      alert(`Failed to delete attachment: ${err.response?.data?.message || err.message}`);
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Upload Form */}
      <div className="border-b border-gray-200 pb-6">
        <h4 className="font-medium text-gray-900 mb-3">Upload New File</h4>
        <div className="space-y-3">
          {uploadError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-700">{uploadError}</p>
            </div>
          )}

          <input
            type="file"
            onChange={handleFileChange}
            accept="image/*,.pdf"
            className="block w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
          />

          {file && (
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Attachment Type
                </label>
                <select
                  value={attachmentType}
                  onChange={(e) => setAttachmentType(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                >
                  <option value="PROBLEM">Problem/Issue Photo</option>
                  <option value="SOLUTION">Solution/Repair Photo</option>
                </select>
              </div>

              <div className="flex items-center justify-between p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <span className="text-sm text-gray-700">{file.name}</span>
                <button
                  onClick={handleUpload}
                  disabled={uploading}
                  className="px-3 py-1 bg-indigo-600 text-white rounded text-sm hover:bg-indigo-700 disabled:opacity-50"
                >
                  {uploading ? "Uploading..." : "Upload"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Attachments List */}
      <div>
        <h4 className="font-medium text-gray-900 mb-3">
          Attachments ({attachments?.length || 0})
        </h4>
        {!attachments || attachments.length === 0 ? (
          <p className="text-center text-gray-500 py-8">No attachments yet</p>
        ) : (
          <div className="space-y-3">
            {attachments.map((attachment) => (
              <div
                key={attachment.id}
                className="flex items-center justify-between p-3 bg-gray-50 border border-gray-200 rounded-lg"
              >
                <div className="flex-1">
                  {attachment.thumbnailPath ? (
                    <img
                      src={`http://localhost:8080${attachment.thumbnailPath}`}
                      alt={attachment.originalFilename}
                      className="w-12 h-12 object-cover rounded mb-2"
                    />
                  ) : (
                    <div className="w-12 h-12 bg-gray-300 rounded mb-2 flex items-center justify-center text-xs">
                      FILE
                    </div>
                  )}
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-medium text-gray-900 text-sm">
                      {attachment.originalFilename}
                    </p>
                    {attachment.type && (
                      <span
                        className={`text-xs px-2 py-0.5 rounded font-medium ${
                          attachment.type === "PROBLEM"
                            ? "bg-red-100 text-red-800"
                            : "bg-green-100 text-green-800"
                        }`}
                      >
                        {attachment.type === "PROBLEM" ? " Issue" : " Solution"}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-600">
                    {attachment.fileSize} bytes | Checksum: {attachment.checksumHash?.slice(0, 8)}...
                  </p>
                </div>

                <div className="flex gap-2">
                  <a
                    href={`http://localhost:8080${attachment.filePath}`}
                    download
                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                  >
                    Download
                  </a>
                  {(isAdmin || 
                    attachment.uploadedById === user?.id || 
                    (hasRole("TECHNICIAN") && ticket?.assignedTechnicianId === user?.id)) && (
                    <button
                      onClick={() =>
                        handleDelete(attachment.id)}
                      disabled={deleting === attachment.id}
                      className={`px-3 py-1 bg-red-600 text-white rounded text-sm ${
                        deleting === attachment.id
                          ? "opacity-50 cursor-not-allowed"
                          : "hover:bg-red-700"
                      }`}
                    >
                      {deleting === attachment.id ? "Deleting..." : "Delete"}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default TicketDetailView;
