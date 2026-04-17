import React, { useState, useContext, useEffect } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import { toast } from "react-toastify";

/**
 * Admin User Management Panel Component
 * Provides admin interface for:
 * - Viewing and managing user registration requests
 * - Approving/rejecting registrations
 * - Viewing user data (future)
 */
export default function AdminUserManagementPanel() {
  const { user, hasRole } = useContext(AuthContext);
  const [activeTab, setActiveTab] = useState("registrations"); // "registrations" or "userData"
  const [requestStatus, setRequestStatus] = useState("PENDING"); // PENDING, APPROVED, REJECTED
  const [requests, setRequests] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedId, setExpandedId] = useState(null);
  const [processingId, setProcessingId] = useState(null);
  const [actionData, setActionData] = useState({}); // Store notes/reasons for actions
  const [editingUserId, setEditingUserId] = useState(null);
  const [editFormData, setEditFormData] = useState({});
  // User filtering state
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedRoles, setSelectedRoles] = useState([]);

  // Map display roles to backend role values
  const roleMapping = {
    STUDENT: "USER", // UI shows "STUDENT" but backend uses "USER"
    LECTURER: "LECTURER",
    TECHNICIAN: "TECHNICIAN",
    FACILITY_MANAGER: "FACILITY_MANAGER",
    ADMIN: "ADMIN",
  };
  const availableRoles = [
    "ADMIN",
    "STUDENT",
    "LECTURER",
    "TECHNICIAN",
    "FACILITY_MANAGER",
  ];

  // Check admin access
  useEffect(() => {
    if (!hasRole("ADMIN")) {
      setError("You do not have permission to access this panel");
      setLoading(false);
    } else {
      if (activeTab === "registrations") {
        fetchRequests();
      } else {
        fetchUsers();
      }
    }
  }, [requestStatus, activeTab, searchQuery, selectedRoles]);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.get(
        `/v1/admin/user-management/registration-requests?status=${requestStatus}&page=0&size=20`,
      );

      setRequests(response.data.content || response.data || []);
    } catch (err) {
      console.error("Failed to fetch registration requests:", err);
      setError(
        err.response?.data?.message || "Failed to load registration requests",
      );
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);

      // Build query string with search and role filters
      const params = new URLSearchParams();
      params.append("page", "0");
      params.append("size", "50");

      if (searchQuery.trim()) {
        params.append("query", searchQuery.trim());
      }

      // Map display roles to backend role values and send to API
      selectedRoles.forEach((role) => {
        const backendRole = roleMapping[role];
        params.append("role", backendRole);
      });

      const response = await apiClient.get(
        `/v1/admin/user-management/users?${params.toString()}`,
      );

      setUsers(response.data.content || response.data || []);
    } catch (err) {
      console.error("Failed to fetch users:", err);
      setError(err.response?.data?.message || "Failed to load users");
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const handleEditUser = (user) => {
    setEditingUserId(user.id);
    setEditFormData({
      displayName: user.displayName,
      email: user.email,
      noShowCount: user.noShowCount,
      suspendedUntil: user.suspendedUntil
        ? new Date(user.suspendedUntil).toISOString().slice(0, 16)
        : "",
    });
  };

  const handleSaveUser = async (userId) => {
    try {
      setProcessingId(userId);

      const payload = {
        displayName: editFormData.displayName,
        email: editFormData.email,
        noShowCount: parseInt(editFormData.noShowCount),
        suspendedUntil: editFormData.suspendedUntil
          ? new Date(editFormData.suspendedUntil).toISOString()
          : null,
      };

      await apiClient.put(`/v1/admin/user-management/users/${userId}`, payload);

      toast.success("User updated successfully");
      setEditingUserId(null);
      setEditFormData({});
      await fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to update user");
    } finally {
      setProcessingId(null);
    }
  };

  const handleCancelEdit = () => {
    setEditingUserId(null);
    setEditFormData({});
  };

  const handleApprove = async (requestId) => {
    try {
      setProcessingId(requestId);
      const note = actionData[requestId]?.note || "";

      await apiClient.post(
        `/v1/admin/user-management/registration-requests/${requestId}/approve`,
        { note },
      );

      toast.success("Registration request approved successfully");
      setActionData((prev) => {
        const updated = { ...prev };
        delete updated[requestId];
        return updated;
      });
      await fetchRequests();
    } catch (err) {
      toast.error(
        err.response?.data?.message || "Failed to approve registration request",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (requestId) => {
    const reason = actionData[requestId]?.reason;

    if (!reason || reason.trim() === "") {
      toast.error("Please provide a rejection reason");
      return;
    }

    if (!window.confirm("Are you sure you want to reject this registration?")) {
      return;
    }

    try {
      setProcessingId(requestId);

      await apiClient.post(
        `/v1/admin/user-management/registration-requests/${requestId}/reject`,
        { reason },
      );

      toast.success("Registration request rejected successfully");
      setActionData((prev) => {
        const updated = { ...prev };
        delete updated[requestId];
        return updated;
      });
      await fetchRequests();
    } catch (err) {
      toast.error(
        err.response?.data?.message || "Failed to reject registration request",
      );
    } finally {
      setProcessingId(null);
    }
  };

  if (!hasRole("ADMIN")) {
    return (
      <div className="bg-white rounded-lg p-6 border border-slate-200">
        <p className="text-slate-600">
          You do not have permission to access the admin panel. Only ADMIN role
          users can access this area.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h1 className="text-3xl font-bold text-slate-900">User Management</h1>
        <p className="text-slate-600 mt-2">
          Manage user registrations and user data
        </p>
      </div>

      {/* Tab Navigation */}
      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex border-b border-slate-200">
          <button
            onClick={() => setActiveTab("registrations")}
            className={`flex-1 py-4 px-6 font-semibold transition-all ${
              activeTab === "registrations"
                ? "text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50"
                : "text-slate-600 hover:text-slate-900 hover:bg-slate-50"
            }`}
          >
            Registration Requests
          </button>
          <button
            onClick={() => setActiveTab("userData")}
            className={`flex-1 py-4 px-6 font-semibold transition-all ${
              activeTab === "userData"
                ? "text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50"
                : "text-slate-600 hover:text-slate-900 hover:bg-slate-50"
            }`}
          >
            User Data
          </button>
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {/* Registration Requests Tab */}
          {activeTab === "registrations" && (
            <div>
              {/* Status Tabs */}
              <div className="flex gap-2 mb-6">
                {["PENDING", "APPROVED", "REJECTED"].map((status) => (
                  <button
                    key={status}
                    onClick={() => setRequestStatus(status)}
                    className={`px-4 py-2 rounded-lg font-medium transition-all ${
                      requestStatus === status
                        ? "bg-indigo-600 text-white"
                        : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                    }`}
                  >
                    {status === "PENDING" && "⏳"}
                    {status === "APPROVED" && "✅"}
                    {status === "REJECTED" && "❌"}
                    {` ${status}`}
                  </button>
                ))}
              </div>

              {/* Loading State */}
              {loading && (
                <div className="flex items-center justify-center h-64">
                  <div className="text-slate-500">
                    Loading registration requests...
                  </div>
                </div>
              )}

              {/* Error State */}
              {error && (
                <div className="rounded-lg bg-red-50 border border-red-200 p-4 mb-4">
                  <p className="text-sm font-medium text-red-900">{error}</p>
                </div>
              )}

              {/* Empty State */}
              {!loading && requests.length === 0 && (
                <div className="text-center py-12">
                  <div className="text-slate-400 mb-2">
                    <svg
                      className="w-12 h-12 mx-auto mb-2 opacity-50"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      />
                    </svg>
                  </div>
                  <p className="text-slate-600 font-medium">
                    No {requestStatus.toLowerCase()} requests
                  </p>
                </div>
              )}

              {/* Requests List */}
              {!loading && requests.length > 0 && (
                <div className="space-y-4">
                  {requests.map((request) => (
                    <div
                      key={request.id}
                      className="rounded-lg border border-slate-200 bg-white overflow-hidden"
                    >
                      {/* Request Summary */}
                      <div className="p-6">
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1">
                            <h3 className="text-lg font-semibold text-slate-900">
                              {request.displayName}
                            </h3>
                            <p className="text-sm text-slate-600 mt-1">
                              {request.email}
                            </p>
                            <div className="mt-3 flex flex-wrap gap-3 text-sm">
                              <span className="px-3 py-1 bg-slate-100 rounded text-slate-700 font-medium">
                                {request.roleRequested === "USER"
                                  ? "Student"
                                  : request.roleRequested.replace("_", " ")}
                              </span>
                              <span className="px-3 py-1 bg-blue-100 rounded text-blue-700 font-medium">
                                {request.roleRequested === "USER"
                                  ? `Reg #: ${request.registrationNumber}`
                                  : `Emp #: ${request.employeeNumber}`}
                              </span>
                            </div>
                          </div>
                          <button
                            onClick={() =>
                              setExpandedId(
                                expandedId === request.id ? null : request.id,
                              )
                            }
                            className="px-3 py-1 text-slate-600 hover:text-slate-900 font-semibold text-sm transition-colors"
                          >
                            {expandedId === request.id ? "Less" : "More"}
                          </button>
                        </div>

                        {/* Request Summary Info */}
                        <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
                          <div>
                            <p className="text-xs font-semibold text-slate-500 uppercase">
                              Submitted
                            </p>
                            <p className="text-slate-900 mt-1">
                              {new Date(request.createdAt).toLocaleString()}
                            </p>
                          </div>
                          <div>
                            <p className="text-xs font-semibold text-slate-500 uppercase">
                              Status
                            </p>
                            <p className="text-slate-900 mt-1">
                              {request.status === "PENDING" && "⏳ Pending"}
                              {request.status === "APPROVED" && "✅ Approved"}
                              {request.status === "REJECTED" && "❌ Rejected"}
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* Expanded Details */}
                      {expandedId === request.id && (
                        <div className="border-t border-slate-200 bg-slate-50 p-6 space-y-4">
                          {request.reviewedAt && (
                            <div>
                              <p className="text-xs font-semibold text-slate-500 uppercase mb-1">
                                Reviewed
                              </p>
                              <p className="text-sm text-slate-700">
                                {new Date(request.reviewedAt).toLocaleString()}
                              </p>
                            </div>
                          )}

                          {request.rejectionReason && (
                            <div>
                              <p className="text-xs font-semibold text-slate-500 uppercase mb-1">
                                Rejection Reason
                              </p>
                              <p className="text-sm text-slate-700">
                                {request.rejectionReason}
                              </p>
                            </div>
                          )}
                        </div>
                      )}

                      {/* Action Buttons (for PENDING requests) */}
                      {request.status === "PENDING" && (
                        <div className="border-t border-slate-200 p-6 bg-white space-y-4">
                          {/* Approval Note/Rejection Reason */}
                          <div>
                            <label className="block text-sm font-semibold text-slate-900 mb-2">
                              {requestStatus === "PENDING"
                                ? "Add note (optional)"
                                : ""}
                            </label>
                            <textarea
                              value={
                                actionData[request.id]?.note ||
                                actionData[request.id]?.reason ||
                                ""
                              }
                              onChange={(e) => {
                                const isRejection = false; // Determine if this is for rejection
                                setActionData((prev) => ({
                                  ...prev,
                                  [request.id]: {
                                    ...prev[request.id],
                                    note: e.target.value,
                                    reason: e.target.value,
                                  },
                                }));
                              }}
                              placeholder="Add a note for the applicant..."
                              rows="3"
                              className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm resize-none"
                            />
                          </div>

                          {/* Buttons */}
                          <div className="flex gap-3">
                            <button
                              onClick={() => handleApprove(request.id)}
                              disabled={processingId === request.id}
                              className="flex-1 px-4 py-2 text-sm text-green-600 border-2 border-green-600 hover:bg-green-50 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {processingId === request.id
                                ? "Processing..."
                                : "Approve"}
                            </button>
                            <button
                              onClick={() => handleReject(request.id)}
                              disabled={processingId === request.id}
                              className="flex-1 px-4 py-2 text-sm text-red-600 border-2 border-red-600 hover:bg-red-50 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {processingId === request.id
                                ? "Processing..."
                                : "Reject"}
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* User Data Tab */}
          {activeTab === "userData" && (
            <div>
              {/* Search and Filter */}
              <div className="mb-6 space-y-4">
                {/* Search Bar */}
                <div>
                  <label className="block text-sm font-semibold text-slate-900 mb-2">
                    Search by Email or Username
                  </label>
                  <input
                    type="text"
                    placeholder="Enter email or display name..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                  />
                </div>

                {/* Role Filter */}
                <div>
                  <label className="block text-sm font-semibold text-slate-900 mb-2">
                    Filter by Roles
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {availableRoles.map((role) => (
                      <label
                        key={role}
                        className="flex items-center gap-2 px-3 py-2 rounded-lg border-2 border-slate-300 cursor-pointer hover:bg-slate-50 transition"
                      >
                        <input
                          type="checkbox"
                          checked={selectedRoles.includes(role)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedRoles([...selectedRoles, role]);
                            } else {
                              setSelectedRoles(
                                selectedRoles.filter((r) => r !== role),
                              );
                            }
                          }}
                          className="w-4 h-4 cursor-pointer"
                        />
                        <span className="text-sm font-medium text-slate-700">
                          {role}
                        </span>
                      </label>
                    ))}
                  </div>
                  {selectedRoles.length > 0 && (
                    <button
                      onClick={() => setSelectedRoles([])}
                      className="mt-2 text-xs text-slate-500 hover:text-slate-700 underline"
                    >
                      Clear all roles
                    </button>
                  )}
                </div>
              </div>

              {/* Loading State */}
              {loading && (
                <div className="flex items-center justify-center h-64">
                  <div className="text-slate-500">Loading users...</div>
                </div>
              )}

              {/* Error State */}
              {error && (
                <div className="rounded-lg bg-red-50 border border-red-200 p-4 mb-4">
                  <p className="text-sm font-medium text-red-900">{error}</p>
                </div>
              )}

              {/* Empty State */}
              {!loading && users.length === 0 && (
                <div className="text-center py-12">
                  <div className="text-slate-400 mb-2">
                    <svg
                      className="w-12 h-12 mx-auto mb-2 opacity-50"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M12 4.354a4 4 0 110 5.292M19 14H5m14 0a2 2 0 012 2v3H3v-3a2 2 0 012-2m0-5a4 4 0 11-8 0 4 4 0 018 0z"
                      />
                    </svg>
                  </div>
                  <p className="text-slate-600 font-medium">No users found</p>
                </div>
              )}

              {/* Users List */}
              {!loading && users.length > 0 && (
                <div className="space-y-4">
                  {users.map((user) => (
                    <div
                      key={user.id}
                      className="rounded-lg border border-slate-200 bg-white overflow-hidden"
                    >
                      {/* User Summary */}
                      {editingUserId !== user.id ? (
                        <div className="p-6">
                          <div className="flex items-start justify-between gap-4">
                            <div className="flex-1">
                              <h3 className="text-lg font-semibold text-slate-900">
                                {user.displayName}
                              </h3>
                              <p className="text-sm text-slate-600 mt-1">
                                {user.email}
                              </p>
                              <div className="mt-3 flex flex-wrap gap-2">
                                {user.roles &&
                                  user.roles.map((role) => (
                                    <span
                                      key={role}
                                      className="px-3 py-1 bg-blue-100 rounded text-blue-700 text-xs font-medium"
                                    >
                                      {role}
                                    </span>
                                  ))}
                              </div>
                            </div>
                            <button
                              onClick={() => handleEditUser(user)}
                              className="px-4 py-2 text-sm text-indigo-600 border-2 border-indigo-600 hover:bg-indigo-50 font-medium rounded-lg transition"
                            >
                              Edit
                            </button>
                          </div>

                          {/* User Details */}
                          <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
                            <div>
                              <p className="text-xs font-semibold text-slate-500 uppercase">
                                No-Shows
                              </p>
                              <p className="text-slate-900 mt-1">
                                {user.noShowCount}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs font-semibold text-slate-500 uppercase">
                                Status
                              </p>
                              <p className="text-slate-900 mt-1">
                                {user.suspendedUntil &&
                                new Date(user.suspendedUntil) > new Date() ? (
                                  <span className="text-red-600 font-semibold">
                                    🚫 Suspended
                                  </span>
                                ) : (
                                  <span className="text-green-600 font-semibold">
                                    ✅ Active
                                  </span>
                                )}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs font-semibold text-slate-500 uppercase">
                                Created
                              </p>
                              <p className="text-slate-900 mt-1">
                                {new Date(user.createdAt).toLocaleDateString()}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs font-semibold text-slate-500 uppercase">
                                Updated
                              </p>
                              <p className="text-slate-900 mt-1">
                                {new Date(user.updatedAt).toLocaleDateString()}
                              </p>
                            </div>
                          </div>
                        </div>
                      ) : (
                        /* Edit Form */
                        <div className="p-6 bg-slate-50 border-t border-slate-200">
                          <h3 className="text-lg font-semibold text-slate-900 mb-4">
                            Edit User
                          </h3>
                          <div className="space-y-4">
                            <div>
                              <label className="block text-sm font-semibold text-slate-900 mb-2">
                                Display Name
                              </label>
                              <input
                                type="text"
                                value={editFormData.displayName}
                                onChange={(e) =>
                                  setEditFormData({
                                    ...editFormData,
                                    displayName: e.target.value,
                                  })
                                }
                                className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                              />
                            </div>

                            <div>
                              <label className="block text-sm font-semibold text-slate-900 mb-2">
                                Email
                              </label>
                              <input
                                type="email"
                                value={editFormData.email}
                                onChange={(e) =>
                                  setEditFormData({
                                    ...editFormData,
                                    email: e.target.value,
                                  })
                                }
                                className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                              />
                            </div>

                            <div>
                              <label className="block text-sm font-semibold text-slate-900 mb-2">
                                No-Show Count
                              </label>
                              <input
                                type="number"
                                value={editFormData.noShowCount}
                                onChange={(e) =>
                                  setEditFormData({
                                    ...editFormData,
                                    noShowCount: e.target.value,
                                  })
                                }
                                min="0"
                                className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                              />
                            </div>

                            <div>
                              <label className="block text-sm font-semibold text-slate-900 mb-2">
                                Suspended Until (leave empty to unsuspend)
                              </label>
                              <input
                                type="datetime-local"
                                value={editFormData.suspendedUntil}
                                onChange={(e) =>
                                  setEditFormData({
                                    ...editFormData,
                                    suspendedUntil: e.target.value,
                                  })
                                }
                                className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                              />
                            </div>

                            {/* Buttons */}
                            <div className="flex gap-3 pt-4">
                              <button
                                onClick={() => handleSaveUser(user.id)}
                                disabled={processingId === user.id}
                                className="flex-1 px-4 py-2 text-sm text-green-600 border-2 border-green-600 hover:bg-green-50 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                {processingId === user.id
                                  ? "Saving..."
                                  : "Save Changes"}
                              </button>
                              <button
                                onClick={handleCancelEdit}
                                disabled={processingId === user.id}
                                className="flex-1 px-4 py-2 text-sm text-slate-600 border-2 border-slate-300 hover:bg-slate-50 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                Cancel
                              </button>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
