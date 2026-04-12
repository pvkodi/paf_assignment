import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";

/**
 * Login Page Component
 * Handles both Google OAuth and email/password login
 * Implements FR-001: OAuth authentication via Google
 * Implements email/password authentication
 */

export function LoginPage() {
  const navigate = useNavigate();
  const {
    login,
    loginWithEmailPassword,
    registerWithEmailPassword,
    loading,
    error: authError,
  } = useAuth();

  // Local state
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [authMode, setAuthMode] = useState("oauth"); // "oauth", "login", "register"

  // Email/Password form state
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
  const REDIRECT_URI = `${window.location.origin}/auth/callback`;

  /**
   * Initiates Google OAuth flow
   */
  const handleGoogleLogin = () => {
    try {
      setError(null);
      setIsProcessing(true);

      if (
        !GOOGLE_CLIENT_ID ||
        GOOGLE_CLIENT_ID.trim() === "" ||
        GOOGLE_CLIENT_ID === "YOUR_GOOGLE_CLIENT_ID" ||
        GOOGLE_CLIENT_ID === "replace-me"
      ) {
        setError(
          "Google OAuth is not configured. Set VITE_GOOGLE_CLIENT_ID in frontend/.env and restart the frontend.",
        );
        setIsProcessing(false);
        return;
      }

      // Generate state parameter for CSRF protection
      const state = Math.random().toString(36).substring(7);
      sessionStorage.setItem("oauth_state", state);

      // Build Google OAuth URL
      const params = new URLSearchParams({
        client_id: GOOGLE_CLIENT_ID,
        redirect_uri: REDIRECT_URI,
        response_type: "code",
        scope: "openid email profile",
        access_type: "offline",
        state: state,
      });

      const googleAuthUrl = `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;

      // Redirect to Google login
      window.location.href = googleAuthUrl;
    } catch (err) {
      console.error("OAuth initiation error:", err);
      setError("Failed to initiate sign-in. Please try again.");
      setIsProcessing(false);
    }
  };

  /**
   * Handle email/password login
   */
  const handleEmailPasswordLogin = async (e) => {
    e.preventDefault();
    setError(null);

    if (!email || !password) {
      setError("Please enter both email and password");
      return;
    }

    try {
      setIsProcessing(true);
      await loginWithEmailPassword(email, password);
      navigate("/dashboard");
    } catch (err) {
      setError(
        err.message || err.code === "INVALID_CREDENTIALS"
          ? "Invalid email or password"
          : "Login failed. Please try again.",
      );
      setIsProcessing(false);
    }
  };

  /**
   * Handle user registration
   */
  const handleRegister = async (e) => {
    e.preventDefault();
    setError(null);

    // Validate inputs
    if (!email || !displayName || !password || !confirmPassword) {
      setError("Please fill in all fields");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    if (password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }

    try {
      setIsProcessing(true);
      await registerWithEmailPassword(
        email,
        displayName,
        password,
        confirmPassword,
      );
      navigate("/dashboard");
    } catch (err) {
      setError(
        err.message ||
          (err.code === "VALIDATION_ERROR"
            ? "Email already registered or invalid input"
            : "Registration failed. Please try again."),
      );
      setIsProcessing(false);
    }
  };

  /**
   * Reset form state when switching modes
   */
  const switchMode = (mode) => {
    setAuthMode(mode);
    setError(null);
    setEmail("");
    setPassword("");
    setDisplayName("");
    setConfirmPassword("");
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-xl p-8">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">
              Smart Campus Hub
            </h1>
            <p className="text-gray-600">Sign in to your account</p>
          </div>

          {/* Error Messages */}
          {(error || authError) && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-700">{error || authError}</p>
            </div>
          )}

          {/* OAuth Mode */}
          {authMode === "oauth" && (
            <div>
              <div className="space-y-4 mb-6">
                <button
                  onClick={handleGoogleLogin}
                  disabled={isProcessing || loading}
                  className="w-full py-3 px-4 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isProcessing || loading ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      Signing in...
                    </>
                  ) : (
                    <>
                      <svg
                        className="w-5 h-5"
                        viewBox="0 0 24 24"
                        fill="currentColor"
                      >
                        <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                        <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                        <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                        <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                      </svg>
                      Sign in with Google
                    </>
                  )}
                </button>
              </div>

              {/* Divider */}
              <div className="relative mb-6">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-gray-300"></div>
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="px-2 bg-white text-gray-500">Or</span>
                </div>
              </div>

              {/* Email/Password toggle */}
              <div className="flex gap-3">
                <button
                  onClick={() => switchMode("login")}
                  className="flex-1 py-2 px-4 text-center text-gray-700 font-medium rounded-lg hover:bg-gray-100 transition"
                >
                  Sign in
                </button>
                <button
                  onClick={() => switchMode("register")}
                  className="flex-1 py-2 px-4 text-center text-gray-700 font-medium rounded-lg hover:bg-gray-100 transition"
                >
                  Create Account
                </button>
              </div>
            </div>
          )}

          {/* Login Mode */}
          {authMode === "login" && (
            <div>
              <form onSubmit={handleEmailPasswordLogin} className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@institution.edu"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  />
                </div>

                <button
                  type="submit"
                  disabled={isProcessing || loading}
                  className="w-full py-3 px-4 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isProcessing || loading ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      Signing in...
                    </>
                  ) : (
                    "Sign in with Email"
                  )}
                </button>
              </form>

              {/*Back to OAuth */}
              <button
                onClick={() => switchMode("oauth")}
                className="w-full mt-4 py-2 px-4 text-center text-gray-600 font-medium rounded-lg hover:bg-gray-100 transition"
              >
                ← Back
              </button>
            </div>
          )}

          {/* Register Mode */}
          {authMode === "register" && (
            <div>
              <form onSubmit={handleRegister} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@institution.edu"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Display Name
                  </label>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Your Name"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="At least 8 characters"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    Must be at least 8 characters
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Confirm Password
                  </label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  />
                </div>

                <button
                  type="submit"
                  disabled={isProcessing || loading}
                  className="w-full py-3 px-4 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isProcessing || loading ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      Creating account...
                    </>
                  ) : (
                    "Create Account"
                  )}
                </button>
              </form>

              {/* Back button */}
              <button
                onClick={() => switchMode("oauth")}
                className="w-full mt-4 py-2 px-4 text-center text-gray-600 font-medium rounded-lg hover:bg-gray-100 transition"
              >
                ← Back
              </button>
            </div>
          )}

          {/* Footer Info */}
          <p className="text-xs text-gray-500 text-center mt-8">
            By signing in, you agree to our Terms of Service and Privacy Policy
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
