import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * AdminBookForUserSelector Component
 * Allows admin and facility manager users to select which user to book on behalf of.
 * Only appears for ADMIN and FACILITY_MANAGER roles.
 */
export default function AdminBookForUserSelector({ onUserSelect, userRole }) {
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedUserId, setSelectedUserId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showDropdown, setShowDropdown] = useState(false);
  const [searchTimeout, setSearchTimeout] = useState(null);

  const canUseSelector = userRole && ["ADMIN", "FACILITY_MANAGER"].includes(userRole);

  // Perform search when search term changes
  useEffect(() => {
    // Clear previous timeout
    if (searchTimeout) {
      clearTimeout(searchTimeout);
    }

    if (searchTerm.trim().length === 0) {
      setFilteredUsers([]);
      return;
    }

    // Debounce search requests (500ms)
    const timeout = setTimeout(() => {
      searchUsers(searchTerm);
    }, 500);

    setSearchTimeout(timeout);

    return () => {
      if (timeout) clearTimeout(timeout);
    };
  }, [searchTerm]);

  const searchUsers = async (query) => {
    if (!query.trim()) {
      setFilteredUsers([]);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/v1/users/search", {
        params: { query: query.trim() }
      });
      setFilteredUsers(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to search users:", err);
      setError("Failed to search users. Please try again.");
      setFilteredUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectUser = (user) => {
    setSelectedUserId(user.id);
    setSearchTerm(user.displayName || user.email);
    setShowDropdown(false);
    setFilteredUsers([]);
    if (onUserSelect) {
      onUserSelect(user.id);
    }
  };

  const handleClear = () => {
    setSelectedUserId("");
    setSearchTerm("");
    setShowDropdown(false);
    setFilteredUsers([]);
    if (onUserSelect) {
      onUserSelect(null);
    }
  };

  if (!canUseSelector) {
    return null;
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 mb-1">
        <label className="block text-sm font-semibold text-[#0f172a]">
          Book on behalf of <span className="text-[#64748b] font-normal">(Optional)</span>
        </label>
        <span className="text-[10px] uppercase tracking-wider bg-[#eff6ff] text-[#1e40af] px-2 py-0.5 rounded font-bold">
          {userRole}
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
                if (e.target.value.trim()) {
                  setShowDropdown(true);
                }
              }}
              onFocus={() => {
                if (searchTerm.trim()) {
                  setShowDropdown(true);
                }
              }}
              onBlur={() => {
                // Delay hiding dropdown to allow click on user to register
                setTimeout(() => setShowDropdown(false), 200);
              }}
              placeholder="Search by name or email..."
              className="w-full px-4 py-3 bg-white border border-[#e2e8f0] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#6366f1] focus:border-transparent transition-all shadow-sm placeholder:text-[#94a3b8]"
            />

            {/* Dropdown */}
            {showDropdown && searchTerm.trim() && (
              <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-[#e2e8f0] rounded-xl shadow-lg z-50 max-h-64 overflow-y-auto">
                {loading ? (
                  <div className="p-4 text-sm text-[#64748b] flex items-center gap-2">
                    <div className="w-4 h-4 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
                    Searching users...
                  </div>
                ) : error ? (
                  <div className="p-4 text-sm text-[#ef4444]">{error}</div>
                ) : filteredUsers.length === 0 ? (
                  <div className="p-4 text-sm text-[#64748b]">
                    No users found matching "{searchTerm}"
                  </div>
                ) : (
                  <div className="py-2">
                    {filteredUsers.map((user) => (
                      <button
                        key={user.id}
                        type="button"
                        onClick={() => handleSelectUser(user)}
                        className="w-full text-left px-4 py-3 hover:bg-[#f8fafc] flex items-center justify-between transition-colors"
                      >
                        <div>
                          <p className="text-sm font-semibold text-[#0f172a]">
                            {(user.displayName && user.displayName.trim()) ? user.displayName : (user.email || "Unknown")}
                          </p>
                          <p className="text-xs text-[#64748b] mt-0.5">{user.email}</p>
                        </div>
                        <span className="text-[10px] font-bold uppercase tracking-wider bg-[#f1f5f9] text-[#475569] px-2 py-1 rounded">
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
              className="px-4 py-3 bg-[#f1f5f9] text-[#475569] rounded-xl hover:bg-[#e2e8f0] hover:text-[#0f172a] transition-colors text-sm font-semibold"
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {selectedUserId && (
        <div className="mt-2 p-3 bg-[#f0fdf4] border border-[#dcfce3] rounded-xl flex items-start gap-3">
          <svg className="w-5 h-5 text-[#166534] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-xs text-[#166534] leading-relaxed">
            <span className="font-bold block mb-0.5">Booking Override Active</span>
            This booking will be created for the selected user. They will receive a notification and can view it in their bookings.
          </p>
        </div>
      )}
    </div>
  );
}
