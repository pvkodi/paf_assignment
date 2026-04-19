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
 * Role-based redirection: Admin/Facility Manager -> /dashboard, Others -> /bookings
 */

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectUrl = searchParams.get("redirect");

  const {
    loginWithEmailPassword,
    registerWithEmailPassword,
    loading,
    error: authError,
    user,
    isAuthenticated,
  } = useAuth();

  // Local state
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [authMode, setAuthMode] = useState("login"); // "oauth", "login", "register"

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
   * Handle role-based redirection after login
   */
  useEffect(() => {
    if (isAuthenticated && user) {
      // If explicit redirect URL provided, use it
      if (redirectUrl) {
        navigate(redirectUrl);
        return;
      }

      // Role-based redirection
      const userRoles = user?.roles || [];
      const isAdmin = userRoles.includes("ADMIN");
      const isFacilityManager = userRoles.includes("FACILITY_MANAGER");

      if (isAdmin || isFacilityManager) {
        navigate("/dashboard");
      } else {
        // Students, Lecturers, Technicians, etc. -> Bookings page
        navigate("/bookings");
      }
    }
  }, [isAuthenticated, user, navigate, redirectUrl]);

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
      // Navigation will be handled by the useEffect that watches isAuthenticated and user
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
    <div className="flex w-full min-h-screen bg-white font-sans">
      <div className="flex w-full min-h-screen">
        {/* Left Side Banner */}
        <div className="hidden md:flex flex-col justify-between w-[55%] lg:w-[45%] max-w-[650px] bg-gradient-to-br from-[#4FC3F7] via-[#4A3AFF] to-[#8C00FF] p-12 text-white relative m-4 md:m-5 lg:m-6 rounded-[32px] overflow-hidden shadow-xl border border-white/10">
          {/* Subtle noise/blur effect */}
          <div className="absolute inset-0 bg-white/10 backdrop-blur-3xl mix-blend-overlay pointer-events-none"></div>
          <div className="absolute -top-1/4 -right-1/4 w-[150%] h-[150%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-white/20 to-transparent blur-[80px] pointer-events-none"></div>

          <div className="relative z-10">
            <svg
              className="w-10 h-10 text-white"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="12" y1="3" x2="12" y2="21" />
              <line x1="5.14" y1="5.14" x2="18.86" y2="18.86" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="5.14" y1="18.86" x2="18.86" y2="5.14" />
            </svg>
          </div>
          <div className="relative z-10 mt-auto pt-20">
            <p className="text-[13px] font-medium mb-3 opacity-90 tracking-wide text-white uppercase">
              You can easily
            </p>
            <h2 className="text-[32px] font-bold leading-[1.25] text-white tracking-tight">
              Get access your personal hub for clarity and productivity
            </h2>
          </div>
        </div>

        {/* Right Side Form */}
        <div className="flex-1 flex flex-col justify-center items-center px-8 lg:px-12 py-20 relative">
          <div className="w-full max-w-[400px] flex flex-col h-full justify-center">
            <div className="mb-8">
              <svg
                className="w-8 h-8 text-[#4A3AFF] mb-6"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="12" y1="3" x2="12" y2="21" />
                <line x1="5.14" y1="5.14" x2="18.86" y2="18.86" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="5.14" y1="18.86" x2="18.86" y2="5.14" />
              </svg>
              <h1 className="text-[32px] font-bold text-gray-900 mb-2 tracking-tight">
                {authMode === "register" ? "Create an account" : "Sign in to account"}
              </h1>
              <p className="text-[14px] text-gray-500 leading-relaxed max-w-[340px]">
                Access your tasks, notes, and projects anytime, anywhere - and keep
                everything flowing in one place.
              </p>
            </div>

          {(error || authError) && (
            <div className="mb-5 p-3.5 bg-red-50 border border-red-100 rounded-xl">
              <p className="text-sm text-red-600 font-medium">{error || authError}</p>
            </div>
          )}

          <div className="flex-1 max-h-[300px] overflow-y-auto pr-2 no-scrollbar">
            {authMode === "login" && (
              <form onSubmit={handleEmailPasswordLogin} className="space-y-4">
                <div>
                  <label className="block text-[13px] font-bold text-gray-900 mb-1.5">
                    Your email
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="farazhaider786@gmail.com"
                    className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                    disabled={isProcessing || loading}
                  />
                </div>
                
                <div>
                  <label className="block text-[13px] font-bold text-gray-900 mb-1.5">
                    Password
                  </label>
                  <div className="relative">
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••••"
                      className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 pr-10 text-sm"
                      disabled={isProcessing || loading}
                    />
                    <button type="button" className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isProcessing || loading}
                  className="w-full py-3.5 px-4 bg-[#4A3AFF] text-white font-semibold rounded-xl hover:bg-[#3d2eea] hover:shadow-[0_8px_20px_rgba(74,58,255,0.25)] hover:-translate-y-[1px] transition-all duration-200 disabled:opacity-70 disabled:cursor-not-allowed disabled:transform-none disabled:shadow-none flex items-center justify-center gap-2 mt-2 text-sm"
                >
                  {isProcessing || loading ? (
                    <div className="animate-spin rounded-[50%] h-5 w-5 border-2 border-white/20 border-t-white"></div>
                  ) : (
                    "Get Started"
                  )}
                </button>
              </form>
            )}

            {authMode === "register" && (
              <form onSubmit={handleRegister} className="space-y-4">
                <div>
                  <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Email</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@smartcampus.edu"
                    className="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                    disabled={isProcessing || loading}
                  />
                </div>
                <div>
                  <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Display Name</label>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Your Name"
                    className="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                    disabled={isProcessing || loading}
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Role</label>
                    <select
                      value={roleRequested}
                      onChange={(e) => setRoleRequested(e.target.value)}
                      className="w-full px-3 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all font-medium text-gray-900 text-sm"
                      disabled={isProcessing || loading}
                    >
                      <option value="USER">Student</option>
                      <option value="LECTURER">Lecturer</option>
                      <option value="TECHNICIAN">Technician</option>
                      <option value="FACILITY_MANAGER">Facility Manager</option>
                    </select>
                  </div>
                  {roleRequested === "USER" ? (
                    <div>
                      <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Reg Number</label>
                      <input
                        type="text"
                        value={registrationNumber}
                        onChange={(e) => setRegistrationNumber(e.target.value)}
                        placeholder="2023000"
                        className="w-full px-3 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                        disabled={isProcessing || loading}
                      />
                    </div>
                  ) : (
                    <div>
                      <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Emp Number</label>
                      <input
                        type="text"
                        value={employeeNumber}
                        onChange={(e) => setEmployeeNumber(e.target.value)}
                        placeholder="EMP001"
                        className="w-full px-3 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                        disabled={isProcessing || loading}
                      />
                    </div>
                  )}
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Password</label>
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full px-3 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                      disabled={isProcessing || loading}
                    />
                  </div>
                  <div>
                    <label className="block text-[13px] font-bold text-gray-900 mb-1.5">Confirm</label>
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full px-3 py-2.5 bg-white border border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:ring-4 focus:ring-[#4A3AFF]/10 outline-none transition-all placeholder-gray-400 font-medium text-gray-900 text-sm"
                      disabled={isProcessing || loading}
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isProcessing || loading}
                  className="w-full py-3.5 px-4 bg-[#4A3AFF] text-white font-semibold rounded-xl hover:bg-[#3d2eea] hover:shadow-[0_8px_20px_rgba(74,58,255,0.25)] hover:-translate-y-[1px] transition-all duration-200 disabled:opacity-70 disabled:cursor-not-allowed disabled:transform-none flex items-center justify-center gap-2 mt-2 text-sm"
                >
                  {isProcessing || loading ? (
                    <div className="animate-spin rounded-full h-5 w-5 border-2 border-white/20 border-t-white"></div>
                  ) : (
                    "Register with OTP"
                  )}
                </button>
              </form>
            )}
          </div>

          <div className="mt-8 mb-5 relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200"></div>
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="px-4 bg-white text-gray-400 font-medium">or continue with</span>
            </div>
          </div>

          <div className="flex gap-2.5 justify-center mb-6">
            <button
              onClick={() => {}}
              className="flex-1 max-w-[100px] py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium rounded-xl transition-colors flex items-center justify-center disabled:opacity-50"
              disabled
            >
              <span className="font-bold text-sm tracking-tighter">Bē</span>
            </button>
            <button
              onClick={handleGoogleLogin}
              disabled={isProcessing || loading}
              className="flex-1 max-w-[100px] py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium rounded-xl transition-colors flex items-center justify-center disabled:opacity-50"
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
            </button>
            <button disabled className="flex-1 max-w-[100px] py-2 bg-gray-100 text-gray-400 font-medium rounded-xl opacity-80 cursor-not-allowed flex items-center justify-center">
              <svg className="w-4 h-4 text-[#1877F2]" fill="currentColor" viewBox="0 0 24 24"><path d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.878v-6.987h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.988C18.343 21.128 22 16.991 22 12z"/></svg>
            </button>
          </div>

          <p className="text-center text-[13px] text-gray-500 font-medium mt-auto">
            {authMode === "login" ? (
              <>
                Don't have an account?{" "}
                <button
                  onClick={() => switchMode("register")}
                  className="text-[#4A3AFF] hover:text-[#3d2eea] font-bold transition-colors"
                >
                  Sign up
                </button>
              </>
            ) : (
              <>
                Already have an account?{" "}
                <button
                  onClick={() => switchMode("login")}
                  className="text-[#4A3AFF] hover:text-[#3d2eea] font-bold transition-colors"
                >
                  Sign in
                </button>
              </>
            )}
          </p>
          </div>
        </div>
      </div>

      {/* OTP Modal */}
      {showOtpModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center px-4 z-50">
          <div className="bg-white rounded-[24px] p-8 max-w-md w-full shadow-2xl">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Verify Your Email
            </h2>
            <p className="text-sm text-gray-600 mb-6">
              Enter the 6-digit code sent to
              <br />
              <span className="font-semibold text-gray-900">
                {registrationData?.email}
              </span>
            </p>

            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-100 rounded-xl">
                <p className="text-sm text-red-600 font-medium">{error}</p>
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
                className="w-full px-4 py-4 text-center text-3xl tracking-[0.5em] font-bold bg-gray-50 border-2 border-gray-200 rounded-xl focus:border-[#4A3AFF] focus:bg-white focus:outline-none transition-all"
                disabled={isProcessing}
              />
            </div>

            {otpTimeRemaining !== null && (
              <p className="text-center text-sm font-medium text-gray-500 mb-5">
                Code expires in <span className="text-[#4A3AFF] font-bold">{Math.floor(otpTimeRemaining / 60)}:
                {String(otpTimeRemaining % 60).padStart(2, "0")}</span>
              </p>
            )}

            <button
              onClick={handleVerifyOtpAndRegister}
              disabled={isProcessing || otpCode.length !== 6}
              className="w-full py-3.5 px-4 bg-[#4A3AFF] text-white font-semibold rounded-xl hover:bg-[#3d2eea] hover:-translate-y-[1px] hover:shadow-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none disabled:shadow-none"
            >
              {isProcessing ? "Verifying..." : "Complete Registration"}
            </button>

            <button
              onClick={handleResendOtp}
              disabled={isProcessing}
              className="w-full mt-3 py-3 px-4 text-center text-[#4A3AFF] font-bold hover:bg-[#4A3AFF]/5 rounded-xl transition-colors disabled:opacity-50"
            >
              Resend Code
            </button>

            <button
              onClick={() => {
                setShowOtpModal(false);
                setOtpCode("");
                setError(null);
              }}
              className="w-full mt-2 py-3 px-4 text-center text-gray-500 font-medium hover:bg-gray-50 hover:text-gray-900 rounded-xl transition-colors"
            >
              Back to Registration
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default LoginPage;
