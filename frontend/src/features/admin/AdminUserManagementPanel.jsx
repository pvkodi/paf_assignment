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
      {/* Header with Refresh */}
      <div className="bg-white rounded-lg shadow-md p-6 border border-slate-200">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">
              User Management
            </h1>
            <p className="text-slate-600 mt-1 text-sm">
              Manage registrations and user data
            </p>
          </div>
          <button
            onClick={() =>
              activeTab === "registrations" ? fetchRequests() : fetchUsers()
            }
            className="px-4 py-2 text-slate-900 bg-slate-100 hover:bg-slate-200 font-semibold rounded-md transition-all duration-200 active:scale-95"
          >
            Refresh
          </button>
        </div>
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
            <div className="space-y-6">
              {/* Status Filters */}
              <div className="flex gap-2 flex-wrap">
                {["PENDING", "APPROVED", "REJECTED"].map((status) => (
                  <button
                    key={status}
                    onClick={() => setRequestStatus(status)}
                    className={`px-4 py-2 rounded-lg font-medium transition-all ${
                      requestStatus === status
                        ? "bg-indigo-600 text-white shadow-md"
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

              {/* Error State */}
              {error && (
                <div className="rounded-lg bg-red-50 border border-red-200 p-4">
                  <p className="text-sm font-medium text-red-900">{error}</p>
                </div>
              )}

              {/* Loading State */}
              {loading && (
                <div className="flex items-center justify-center h-64">
                  <div className="text-slate-500">
                    Loading registration requests...
                  </div>
                </div>
              )}

              {/* Empty State */}
              {!loading && requests.length === 0 && (
                <div className="text-center py-12 rounded-lg border-2 border-dashed border-slate-300 bg-slate-50">
                  <p className="text-slate-600 font-medium">
                    No {requestStatus.toLowerCase()} requests
                  </p>
                </div>
              )}

              {/* Table View */}
              {!loading && requests.length > 0 && (
                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-slate-50 border-b border-slate-200">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Name
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Email
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Role
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Submitted
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Status
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Action
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {requests.map((request) => (
                        <React.Fragment key={request.id}>
                          <tr className="hover:bg-slate-50 transition">
                            <td className="px-6 py-4">
                              <p className="text-sm font-semibold text-slate-900">
                                {request.displayName}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <p className="text-sm text-slate-600">
                                {request.email}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <span className="px-3 py-1 bg-slate-100 text-slate-700 rounded text-xs font-medium">
                                {request.roleRequested === "USER"
                                  ? "Student"
                                  : request.roleRequested.replace(/_/g, " ")}
                              </span>
                            </td>
                            <td className="px-6 py-4">
                              <p className="text-sm text-slate-600">
                                {new Date(
                                  request.createdAt,
                                ).toLocaleDateString()}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <span
                                className={`px-3 py-1 rounded text-xs font-medium ${
                                  request.status === "PENDING"
                                    ? "bg-yellow-100 text-yellow-700"
                                    : request.status === "APPROVED"
                                      ? "bg-green-100 text-green-700"
                                      : "bg-red-100 text-red-700"
                                }`}
                              >
                                {request.status === "PENDING" && "⏳ Pending"}
                                {request.status === "APPROVED" && "✅ Approved"}
                                {request.status === "REJECTED" && "❌ Rejected"}
                              </span>
                            </td>
                            <td className="px-6 py-4">
                              <button
                                onClick={() =>
                                  setExpandedId(
                                    expandedId === request.id
                                      ? null
                                      : request.id,
                                  )
                                }
                                className="text-indigo-600 hover:text-indigo-900 font-medium text-sm"
                              >
                                {expandedId === request.id ? "Hide" : "View"}
                              </button>
                            </td>
                          </tr>

                          {/* Expanded Row */}
                          {expandedId === request.id && (
                            <tr className="bg-slate-50">
                              <td colSpan="6" className="px-6 py-4">
                                <div className="space-y-4">
                                  <div className="grid grid-cols-2 gap-4 mb-4">
                                    <div>
                                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                                        {request.roleRequested === "USER"
                                          ? "Registration Number"
                                          : "Employee Number"}
                                      </p>
                                      <p className="text-sm text-slate-900">
                                        {request.roleRequested === "USER"
                                          ? request.registrationNumber
                                          : request.employeeNumber}
                                      </p>
                                    </div>
                                    {request.reviewedAt && (
                                      <div>
                                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                                          Reviewed
                                        </p>
                                        <p className="text-sm text-slate-900">
                                          {new Date(
                                            request.reviewedAt,
                                          ).toLocaleString()}
                                        </p>
                                      </div>
                                    )}
                                  </div>

                                  {request.rejectionReason && (
                                    <div>
                                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                                        Rejection Reason
                                      </p>
                                      <p className="text-sm text-slate-700 bg-white p-3 rounded border border-slate-200">
                                        {request.rejectionReason}
                                      </p>
                                    </div>
                                  )}

                                  {request.status === "PENDING" && (
                                    <div className="space-y-3 pt-3 border-t border-slate-300">
                                      <div>
                                        <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                                          Add Note (Optional)
                                        </label>
                                        <textarea
                                          value={
                                            actionData[request.id]?.note ||
                                            actionData[request.id]?.reason ||
                                            ""
                                          }
                                          onChange={(e) => {
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
                                          rows="2"
                                          className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm resize-none"
                                        />
                                      </div>

                                      <div className="flex gap-3">
                                        <button
                                          onClick={() =>
                                            handleApprove(request.id)
                                          }
                                          disabled={processingId === request.id}
                                          className="flex-1 px-4 py-2 text-sm bg-green-50 text-green-600 border-2 border-green-600 hover:bg-green-100 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                          {processingId === request.id
                                            ? "Processing..."
                                            : "✅ Approve"}
                                        </button>
                                        <button
                                          onClick={() =>
                                            handleReject(request.id)
                                          }
                                          disabled={processingId === request.id}
                                          className="flex-1 px-4 py-2 text-sm bg-red-50 text-red-600 border-2 border-red-600 hover:bg-red-100 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                          {processingId === request.id
                                            ? "Processing..."
                                            : "❌ Reject"}
                                        </button>
                                      </div>
                                    </div>
                                  )}
                                </div>
                              </td>
                            </tr>
                          )}
                        </React.Fragment>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* User Data Tab */}
          {activeTab === "userData" && (
            <div className="space-y-6">
              {/* Search and Filter Bar */}
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-4 space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                    Search
                  </label>
                  <input
                    type="text"
                    placeholder="Search by email or display name..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full px-4 py-2 border border-slate-300 bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                    Filter by Roles
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {availableRoles.map((role) => (
                      <label
                        key={role}
                        className={`flex items-center gap-2 px-3 py-2 rounded-lg border-2 cursor-pointer transition ${
                          selectedRoles.includes(role)
                            ? "border-indigo-600 bg-indigo-50"
                            : "border-slate-300 hover:border-slate-400"
                        }`}
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
                      Clear filters
                    </button>
                  )}
                </div>
              </div>

              {/* Error State */}
              {error && (
                <div className="rounded-lg bg-red-50 border border-red-200 p-4">
                  <p className="text-sm font-medium text-red-900">{error}</p>
                </div>
              )}

              {/* Loading State */}
              {loading && (
                <div className="flex items-center justify-center h-64">
                  <div className="text-slate-500">Loading users...</div>
                </div>
              )}

              {/* Empty State */}
              {!loading && users.length === 0 && (
                <div className="text-center py-12 rounded-lg border-2 border-dashed border-slate-300 bg-slate-50">
                  <p className="text-slate-600 font-medium">No users found</p>
                </div>
              )}

              {/* Table View */}
              {!loading && users.length > 0 && (
                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-slate-50 border-b border-slate-200">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Name
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Email
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Roles
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          No-Shows
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Status
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wide">
                          Action
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {users.map((user) => (
                        <React.Fragment key={user.id}>
                          <tr className="hover:bg-slate-50 transition">
                            <td className="px-6 py-4">
                              <p className="text-sm font-semibold text-slate-900">
                                {user.displayName}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <p className="text-sm text-slate-600">
                                {user.email}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <div className="flex gap-1 flex-wrap">
                                {user.roles &&
                                  user.roles.map((role) => (
                                    <span
                                      key={role}
                                      className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-medium"
                                    >
                                      {role}
                                    </span>
                                  ))}
                              </div>
                            </td>
                            <td className="px-6 py-4">
                              <p className="text-sm text-slate-900 font-medium">
                                {user.noShowCount}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <span
                                className={`px-3 py-1 rounded text-xs font-medium ${
                                  user.suspendedUntil &&
                                  new Date(user.suspendedUntil) > new Date()
                                    ? "bg-red-100 text-red-700"
                                    : "bg-green-100 text-green-700"
                                }`}
                              >
                                {user.suspendedUntil &&
                                new Date(user.suspendedUntil) > new Date() ? (
                                  <>🚫 Suspended</>
                                ) : (
                                  <>✅ Active</>
                                )}
                              </span>
                            </td>
                            <td className="px-6 py-4 text-center">
                              {editingUserId === user.id ? (
                                <button
                                  onClick={() => setExpandedId(null)}
                                  className="text-indigo-600 hover:text-indigo-900 font-medium text-sm"
                                >
                                  Close
                                </button>
                              ) : (
                                <button
                                  onClick={() => {
                                    handleEditUser(user);
                                    setExpandedId(user.id);
                                  }}
                                  className="text-indigo-600 hover:text-indigo-900 font-medium text-sm"
                                >
                                  Edit
                                </button>
                              )}
                            </td>
                          </tr>

                          {/* Expanded Row - Edit Form */}
                          {expandedId === user.id &&
                            editingUserId === user.id && (
                              <tr className="bg-slate-50 border-t-2 border-t-slate-300">
                                <td colSpan="6" className="px-6 py-6">
                                  <div className="max-w-2xl">
                                    <h3 className="text-lg font-semibold text-slate-900 mb-4">
                                      Edit User
                                    </h3>
                                    <div className="grid grid-cols-2 gap-4 mb-6">
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
                                          Suspend Until
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
                                    </div>

                                    <p className="text-xs text-slate-500 mb-4">
                                      Leave "Suspend Until" empty to unsuspend
                                      the user
                                    </p>

                                    <div className="flex gap-3">
                                      <button
                                        onClick={() => handleSaveUser(user.id)}
                                        disabled={processingId === user.id}
                                        className="flex-1 px-4 py-2 text-sm bg-green-50 text-green-600 border-2 border-green-600 hover:bg-green-100 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                      >
                                        {processingId === user.id
                                          ? "Saving..."
                                          : "✅ Save Changes"}
                                      </button>
                                      <button
                                        onClick={() => {
                                          handleCancelEdit();
                                          setExpandedId(null);
                                        }}
                                        disabled={processingId === user.id}
                                        className="flex-1 px-4 py-2 text-sm bg-slate-100 text-slate-600 border-2 border-slate-300 hover:bg-slate-200 font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                      >
                                        Cancel
                                      </button>
                                    </div>
                                  </div>
                                </td>
                              </tr>
                            )}
                        </React.Fragment>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
