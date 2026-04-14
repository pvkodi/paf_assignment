import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * AdminBookForUserSelector Component
 * Allows admin users to select which user to book on behalf of.
 * Only appears for ADMIN and FACILITY_MANAGER roles.
 */
export default function AdminBookForUserSelector({ onUserSelect, userRole }) {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedUserId, setSelectedUserId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showDropdown, setShowDropdown] = useState(false);

  const canUseSelector = userRole && ["ADMIN", "FACILITY_MANAGER"].includes(userRole);

  useEffect(() => {
    if (canUseSelector && showDropdown) {
      fetchUsers();
    }
  }, [showDropdown, canUseSelector]);

  useEffect(() => {
    // Filter users based on search term
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      const filtered = users.filter(
        (user) =>
          user.displayName?.toLowerCase().includes(term) ||
          user.email?.toLowerCase().includes(term),
      );
      setFilteredUsers(filtered);
    } else {
      setFilteredUsers(users);
    }
  }, [searchTerm, users]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/v1/users");
      setUsers(Array.isArray(response.data) ? response.data : []);
      setFilteredUsers(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to fetch users:", err);
      setError("Failed to load users. Please try again.");
      setUsers([]);
      setFilteredUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectUser = (user) => {
    setSelectedUserId(user.id);
    setSearchTerm(user.displayName || user.email);
    setShowDropdown(false);
    if (onUserSelect) {
      onUserSelect(user.id);
    }
  };

  const handleClear = () => {
    setSelectedUserId("");
    setSearchTerm("");
    setShowDropdown(false);
    if (onUserSelect) {
      onUserSelect(null);
    }
  };

  if (!canUseSelector) {
    return null;
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2 mb-2">
        <label className="block text-sm font-medium text-slate-700">
          Book on behalf of (Optional - Admin only)
        </label>
        <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded font-medium">
          ADMIN ONLY
        </span>
      </div>

      <div className="relative">
        <div className="flex gap-2">
          <div className="flex-1 relative">
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setShowDropdown(true);
              }}
              onFocus={() => setShowDropdown(true)}
              placeholder="Search by name or email..."
              className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />

            {/* Dropdown */}
            {showDropdown && (
              <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-300 rounded-md shadow-lg z-50 max-h-64 overflow-y-auto">
                {loading ? (
                  <div className="p-3 text-sm text-slate-600">Loading users...</div>
                ) : error ? (
                  <div className="p-3 text-sm text-red-600">{error}</div>
                ) : filteredUsers.length === 0 ? (
                  <div className="p-3 text-sm text-slate-600">
                    {searchTerm ? "No users found" : "No users available"}
                  </div>
                ) : (
                  <div>
                    {filteredUsers.map((user) => (
                      <button
                        key={user.id}
                        type="button"
                        onClick={() => handleSelectUser(user)}
                        className="w-full text-left px-3 py-2 hover:bg-blue-50 flex items-center justify-between border-b border-slate-100 last:border-b-0 transition-colors"
                      >
                        <div>
                          <p className="text-sm font-medium text-slate-900">
                            {user.displayName || "Unknown"}
                          </p>
                          <p className="text-xs text-slate-600">{user.email}</p>
                        </div>
                        <span className="text-xs bg-slate-100 text-slate-700 px-2 py-1 rounded">
                          {user.roles?.[0] || "USER"}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          {selectedUserId && (
            <button
              type="button"
              onClick={handleClear}
              className="px-3 py-2 bg-slate-200 text-slate-700 rounded-md hover:bg-slate-300 transition-colors text-sm font-medium"
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {selectedUserId && (
        <div className="mt-2 p-2 bg-blue-50 border border-blue-200 rounded-md">
          <p className="text-xs text-blue-800">
            <span className="font-semibold">ℹ️ Note:</span> This booking will be created for the
            selected user. They will receive a notification and can view it in their bookings.
          </p>
        </div>
      )}
    </div>
  );
}
