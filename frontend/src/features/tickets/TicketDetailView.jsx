import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";
import TicketAssignmentDialog from "./TicketAssignmentDialog";
import TicketStatusUpdate from "./TicketStatusUpdate";

/**
 * Ticket Detail View Component
 * Displays full ticket information with comments, attachments, and escalation history
 * Implements US4 requirement: Ticket detail view with comments, attachments, and SLA display
 */

export function TicketDetailView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAssignmentDialog, setShowAssignmentDialog] = useState(false);
  const [activeTab, setActiveTab] = useState("details");

  useEffect(() => {
    fetchTicket();
  }, [id]);

  const fetchTicket = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get(`/tickets/${id}`);
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
  };

  const isStaff = user?.roles?.some((role) =>
    ["FACILITY_MANAGER", "ADMIN"].includes(role),
  );

  const canAssign =
    isStaff &&
    user?.facilityId === ticket?.facilityId;

  const canUpdateStatus =
    isStaff && user?.facilityId === ticket?.facilityId;

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
          ← Back to Tickets
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
          ← Back to Tickets
        </button>

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                #{ticket.id} - {ticket.title}
              </h1>
              <p className="text-gray-600 mt-2">{ticket.description}</p>
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
                      <p className="text-gray-900 mt-1">{ticket.creatorName}</p>
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
                  attachments={ticket.attachments}
                  isAdmin={user?.roles?.includes("ADMIN")}
                  onAttachmentDeleted={fetchTicket}
                />
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Escalation History (Staff Only) */}
        {isStaff && (
          <div className="bg-white rounded-lg border border-gray-200 p-6 h-fit">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Escalation History
            </h3>
            {ticket.escalationHistory && ticket.escalationHistory.length > 0 ? (
              <div className="space-y-3">
                {ticket.escalationHistory.map((escalation, index) => (
                  <div
                    key={index}
                    className="pb-3 border-b border-gray-200 last:border-b-0"
                  >
                    <p className="text-sm font-medium text-gray-900">
                      Level {escalation.escalationLevel}
                    </p>
                    <p className="text-xs text-gray-600 mt-1">
                      {formatDate(escalation.escalatedAt)}
                    </p>
                    <p className="text-xs text-gray-700 mt-2">
                      {escalation.escalationReason}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-gray-500">No escalations yet</p>
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

  useEffect(() => {
    fetchComments();
  }, [ticketId]);

  const fetchComments = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get(`/tickets/${ticketId}/comments`);
      setComments(response.data || []);
    } catch (err) {
      console.error("Failed to fetch comments:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    setError(null);

    if (!newComment.trim()) {
      setError("Comment cannot be empty");
      return;
    }

    if (newComment.length < 1 || newComment.length > 5000) {
      setError("Comment must be between 1 and 5000 characters");
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
          placeholder="Add a comment... (1-5000 characters)"
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
          comments.map((comment) => (
            <div
              key={comment.id}
              className={`p-4 rounded-lg border ${
                comment.visibility === "INTERNAL"
                  ? "bg-yellow-50 border-yellow-200"
                  : "bg-gray-50 border-gray-200"
              }`}
            >
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="font-medium text-gray-900">
                    {comment.authorName}
                  </p>
                  <p className="text-xs text-gray-600">
                    {new Date(comment.createdAt).toLocaleString()}
                    {comment.isEdited && " (edited)"}
                  </p>
                </div>
                {comment.visibility === "INTERNAL" && (
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
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() =>
                        handleUpdateComment(comment.id)}
                      className="px-3 py-1 bg-indigo-600 text-white rounded text-sm hover:bg-indigo-700"
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
                  <p className="text-gray-700 text-sm">{comment.content}</p>
                  {(user?.id === comment.authorId ||
                    user?.roles?.includes("ADMIN")) && (
                    <div className="flex gap-2 mt-3">
                      <button
                        onClick={() => {
                          setEditingCommentId(comment.id);
                          setEditingContent(comment.content);
                        }}
                        className="text-xs text-indigo-600 hover:text-indigo-900"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() =>
                          handleDeleteComment(comment.id)}
                        className="text-xs text-red-600 hover:text-red-900"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

/**
 * Attachments Section Component
 */
function AttachmentsSection({ ticketId, attachments, isAdmin, onAttachmentDeleted }) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [file, setFile] = useState(null);

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

      await apiClient.post(`/tickets/${ticketId}/attachments`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      setFile(null);
      onAttachmentDeleted();
    } catch (err) {
      console.error("Failed to upload attachment:", err);
      setUploadError(
        err.response?.data?.message ||
          "Failed to upload file. Please try again.",
      );
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (attachmentId) => {
    if (!window.confirm("Are you sure you want to delete this attachment?")) {
      return;
    }

    try {
      await apiClient.delete(`/tickets/${ticketId}/attachments/${attachmentId}`);
      onAttachmentDeleted();
    } catch (err) {
      console.error("Failed to delete attachment:", err);
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
                      📄
                    </div>
                  )}
                  <p className="font-medium text-gray-900 text-sm">
                    {attachment.originalFilename}
                  </p>
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
                  {isAdmin && (
                    <button
                      onClick={() =>
                        handleDelete(attachment.id)}
                      className="px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700"
                    >
                      Delete
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
