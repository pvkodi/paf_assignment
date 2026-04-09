import React from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navigation */}
      <nav className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-gray-900">
                Smart Campus Hub
              </h1>
            </div>
            <div className="flex items-center space-x-4">
              {user && (
                <>
                  <div className="flex items-center space-x-3">
                    {user.picture && (
                      <img
                        src={user.picture}
                        alt={user.name}
                        className="h-10 w-10 rounded-full"
                      />
                    )}
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {user.name}
                      </p>
                      <p className="text-xs text-gray-500">{user.email}</p>
                    </div>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition"
                  >
                    Logout
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Welcome</h2>
          <p className="text-gray-600 mb-6">
            You are successfully authenticated with Google OAuth!
          </p>

          {/* User Information Card */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-indigo-50 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-indigo-900 mb-4">
                Your Profile
              </h3>
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-indigo-700">
                    Name
                  </label>
                  <p className="text-indigo-900">
                    {user?.displayName || "N/A"}
                  </p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-indigo-700">
                    Email
                  </label>
                  <p className="text-indigo-900">{user?.email || "N/A"}</p>
                </div>
              </div>
            </div>

            <div className="bg-blue-50 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-blue-900 mb-4">
                Authentication Status
              </h3>
              <div className="space-y-3">
                <div className="flex items-center">
                  <div className="w-3 h-3 bg-green-500 rounded-full mr-2"></div>
                  <span className="text-blue-900">You are authenticated</span>
                </div>
                <div className="flex items-center">
                  <div className="w-3 h-3 bg-green-500 rounded-full mr-2"></div>
                  <span className="text-blue-900">JWT tokens are active</span>
                </div>
                <div className="flex items-center">
                  <div className="w-3 h-3 bg-green-500 rounded-full mr-2"></div>
                  <span className="text-blue-900">Roles</span>
                  {user?.roles ? (
                    <span className="ml-1 text-sm text-blue-700">
                      ({user.roles.join(", ")})
                    </span>
                  ) : (
                    <span className="ml-1 text-sm text-blue-700">
                      (No roles)
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Next Steps */}
          <div className="mt-8 bg-yellow-50 border border-yellow-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-yellow-900 mb-2">
              Next Steps
            </h3>
            <ul className="list-disc list-inside text-yellow-800 space-y-1">
              <li>You can now use the system with your Google account</li>
              <li>
                Protected endpoints will automatically include your JWT token
              </li>
              <li>Tokens will automatically refresh when expired</li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  );
}
