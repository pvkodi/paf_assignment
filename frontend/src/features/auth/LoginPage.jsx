import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import authService from "../../services/authService";

/**
 * Login Page Component
 * Handles both Google OAuth and email/password login
 * Implements FR-001: OAuth authentication via Google
 * Implements email/password authentication
 * Supports redirect parameter for post-login navigation (e.g., QR check-in flow)
 */

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectUrl = searchParams.get("redirect") || "/dashboard";

  const {
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

  // Registration form state
  const [roleRequested, setRoleRequested] = useState("USER");
  const [registrationNumber, setRegistrationNumber] = useState("");
  const [employeeNumber, setEmployeeNumber] = useState("");

  // OTP Modal state
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [otpExpiresAt, setOtpExpiresAt] = useState(null);
  const [otpTimeRemaining, setOtpTimeRemaining] = useState(null);
  const [registrationData, setRegistrationData] = useState(null); // Store form data while waiting for OTP

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
      navigate(redirectUrl);
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
   * Step 1: Handle user registration form submission
   * Validates input and sends OTP to email
   */
  const handleRegister = async (e) => {
    e.preventDefault();
    setError(null);

    // Validate inputs
    if (!email || !displayName || !password || !confirmPassword) {
      setError("Please fill in all fields");
      return;
    }

    // Validate role-specific fields
    if (roleRequested === "USER" && !registrationNumber) {
      setError("Registration number is required for students");
      return;
    }

    if (roleRequested !== "USER" && !employeeNumber) {
      setError("Employee number is required for this role");
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

    if (!email.includes("@smartcampus.edu")) {
      setError("Only @smartcampus.edu email addresses are accepted");
      return;
    }

    // Store registration data and send OTP
    try {
      setIsProcessing(true);
      const response = await authService.sendOtp(email);

      // Store registration data for later use
      setRegistrationData({
        email,
        displayName,
        password,
        confirmPassword,
        roleRequested,
        registrationNumber,
        employeeNumber,
      });

      // Show OTP modal
      setOtpExpiresAt(new Date(response.expiresAt));
      setOtpTimeRemaining(response.expirationMinutes * 60);
      setShowOtpModal(true);
      setOtpCode("");
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to send OTP. Please try again.");
      console.error("Send OTP error:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Step 2: Verify OTP and complete registration
   */
  const handleVerifyOtpAndRegister = async () => {
    setError(null);

    if (!otpCode || otpCode.trim() === "") {
      setError("Please enter the OTP code");
      return;
    }

    if (otpCode.length !== 6 || !/^\d+$/.test(otpCode)) {
      setError("OTP must be 6 digits");
      return;
    }

    if (!registrationData) {
      setError("Registration data not found");
      return;
    }

    try {
      setIsProcessing(true);
      const response = await authService.verifyOtpAndRegister(
        registrationData.email,
        otpCode,
        registrationData.displayName,
        registrationData.password,
        registrationData.confirmPassword,
        registrationData.roleRequested,
        registrationData.registrationNumber,
        registrationData.employeeNumber,
      );

      // Store tokens and user
      authService.setAuthTokens(
        response.token,
        response.refreshToken,
        response.user,
        response.expiresAt,
      );

      // Close modal and redirect
      setShowOtpModal(false);
      navigate(redirectUrl);
    } catch (err) {
      setError(err.message || "Registration failed. Please try again.");
      console.error("Registration error:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Handle resend OTP
   */
  const handleResendOtp = async () => {
    setError(null);

    if (!registrationData?.email) {
      setError("Email not found");
      return;
    }

    try {
      setIsProcessing(true);
      const response = await authService.sendOtp(registrationData.email);
      setOtpExpiresAt(new Date(response.expiresAt));
      setOtpTimeRemaining(response.expirationMinutes * 60);
      setOtpCode("");
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to resend OTP");
    } finally {
      setIsProcessing(false);
    }
  };

  // OTP timer countdown
  useEffect(() => {
    if (!otpExpiresAt || !showOtpModal) return;

    const interval = setInterval(() => {
      const now = new Date();
      const remaining = Math.max(0, Math.floor((otpExpiresAt - now) / 1000));
      setOtpTimeRemaining(remaining);

      if (remaining === 0) {
        clearInterval(interval);
        setError("OTP has expired. Please request a new one.");
        setShowOtpModal(false);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [otpExpiresAt, showOtpModal]);

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
    setRoleRequested("USER");
    setRegistrationNumber("");
    setEmployeeNumber("");
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 px-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg border border-gray-200 p-8">
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
                Back
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
                    placeholder="your@smartcampus.edu"
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
                    Role
                  </label>
                  <select
                    value={roleRequested}
                    onChange={(e) => setRoleRequested(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                    disabled={isProcessing || loading}
                  >
                    <option value="USER">Student</option>
                    <option value="LECTURER">Lecturer</option>
                    <option value="TECHNICIAN">Technician</option>
                    <option value="FACILITY_MANAGER">Facility Manager</option>
                  </select>
                </div>

                {roleRequested === "USER" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Registration Number
                    </label>
                    <input
                      type="text"
                      value={registrationNumber}
                      onChange={(e) => setRegistrationNumber(e.target.value)}
                      placeholder="e.g., 20230001234"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                      disabled={isProcessing || loading}
                    />
                  </div>
                )}

                {roleRequested !== "USER" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Employee Number
                    </label>
                    <input
                      type="text"
                      value={employeeNumber}
                      onChange={(e) => setEmployeeNumber(e.target.value)}
                      placeholder="e.g., EMP00123"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
                      disabled={isProcessing || loading}
                    />
                  </div>
                )}

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
                      Sending OTP...
                    </>
                  ) : (
                    "Register with OTP"
                  )}
                </button>
              </form>

              {/* Back button */}
              <button
                onClick={() => switchMode("oauth")}
                className="w-full mt-4 py-2 px-4 text-center text-gray-600 font-medium rounded-lg hover:bg-gray-100 transition"
              >
                Back
              </button>
            </div>
          )}

          {/* OTP Modal */}
          {showOtpModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center px-4 z-50">
              <div className="bg-white rounded-lg p-8 max-w-md w-full">
                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                  Verify Your Email
                </h2>
                <p className="text-gray-600 mb-6">
                  Enter the 6-digit code sent to
                  <br />
                  <span className="font-semibold">
                    {registrationData?.email}
                  </span>
                </p>

                {error && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                    <p className="text-sm text-red-700">{error}</p>
                  </div>
                )}

                <div className="mb-6">
                  <input
                    type="text"
                    value={otpCode}
                    onChange={(e) => {
                      const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                      setOtpCode(val);
                    }}
                    maxLength="6"
                    placeholder="000000"
                    className="w-full px-4 py-3 text-center text-2xl tracking-widest border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none transition"
                    disabled={isProcessing}
                  />
                </div>

                {otpTimeRemaining !== null && (
                  <p className="text-center text-sm text-gray-600 mb-4">
                    Code expires in {Math.floor(otpTimeRemaining / 60)}:
                    {String(otpTimeRemaining % 60).padStart(2, "0")}
                  </p>
                )}

                <button
                  onClick={handleVerifyOtpAndRegister}
                  disabled={isProcessing || otpCode.length !== 6}
                  className="w-full py-3 px-4 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isProcessing ? "Verifying..." : "Complete Registration"}
                </button>

                <button
                  onClick={handleResendOtp}
                  disabled={isProcessing}
                  className="w-full mt-3 py-2 px-4 text-center text-indigo-600 font-medium hover:bg-indigo-50 rounded-lg transition disabled:opacity-50"
                >
                  Resend Code
                </button>

                <button
                  onClick={() => {
                    setShowOtpModal(false);
                    setOtpCode("");
                    setError(null);
                  }}
                  className="w-full mt-2 py-2 px-4 text-center text-gray-600 font-medium hover:bg-gray-100 rounded-lg transition"
                >
                  Back to Registration
                </button>
              </div>
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
