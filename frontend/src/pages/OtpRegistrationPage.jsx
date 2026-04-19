import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import authService from "../services/authService";

/**
 * OTP Registration Page Component
 * Implements new OTP-based registration flow (no admin approval needed)
 * Only accepts emails from @smartcampus.edu domain
 *
 * Flow:
 * 1. User enters email
 * 2. System sends OTP to email (validates @smartcampus.edu domain)
 * 3. User enters OTP
 * 4. User enters registration details (password, name, role, credentials)
 * 5. System auto-registers user, user is immediately logged in
 */
export function OtpRegistrationPage() {
  const navigate = useNavigate();

  // Step 1: Email entry
  const [step, setStep] = useState("email"); // "email", "otp", "details"
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [roleRequested, setRoleRequested] = useState("USER");
  const [registrationNumber, setRegistrationNumber] = useState("");
  const [employeeNumber, setEmployeeNumber] = useState("");

  // UI state
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [otpExpiresAt, setOtpExpiresAt] = useState(null);
  const [otpTimeRemaining, setOtpTimeRemaining] = useState(null);

  // OTP timer countdown
  useEffect(() => {
    if (!otpExpiresAt || step !== "otp") return;

    const interval = setInterval(() => {
      const now = new Date();
      const remaining = Math.max(0, Math.floor((otpExpiresAt - now) / 1000));
      setOtpTimeRemaining(remaining);

      if (remaining === 0) {
        clearInterval(interval);
        setError("OTP has expired. Please request a new one.");
        setStep("email");
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [otpExpiresAt, step]);

  /**
   * Step 1: Request OTP for email
   */
  const handleSendOtp = async (e) => {
    e?.preventDefault?.();
    setError(null);

    // Validate email
    if (!email || email.trim() === "") {
      setError("Please enter your email address");
      return;
    }

    if (!email.includes("@smartcampus.edu")) {
      setError("Only @smartcampus.edu email addresses are accepted");
      return;
    }

    try {
      setIsProcessing(true);
      const response = await authService.sendOtp(email);

      // Move to OTP entry step
      setStep("otp");
      setOtpExpiresAt(new Date(response.expiresAt));
      setOtpTimeRemaining(response.expirationMinutes * 60);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to send OTP. Please try again.");
      console.error("Send OTP error:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Step 2: Verify OTP and move to details entry
   */
  const handleVerifyOtp = async (e) => {
    e?.preventDefault?.();
    setError(null);

    if (!otp || otp.trim() === "") {
      setError("Please enter the OTP code");
      return;
    }

    if (otp.length !== 6 || !/^\d+$/.test(otp)) {
      setError("OTP must be 6 digits");
      return;
    }

    try {
      setIsProcessing(true);
      // Just verify OTP is valid format - actual verification happens in step 3
      setStep("details");
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to verify OTP. Please try again.");
      console.error("Verify OTP error:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Step 3: Complete registration with details
   */
  const handleCompleteRegistration = async (e) => {
    e.preventDefault();
    setError(null);

    // Validate all fields
    if (!displayName || !password || !confirmPassword) {
      setError("Please fill in all fields");
      return;
    }

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

    try {
      setIsProcessing(true);

      // Verify OTP and register
      const response = await authService.verifyOtpAndRegister(
        email,
        otp,
        displayName,
        password,
        confirmPassword,
        roleRequested,
        registrationNumber,
        employeeNumber,
      );

      // Store auth tokens
      if (response.token) {
        authService.setAuthTokens(
          response.token,
          response.refreshToken,
          response.expiresAt,
        );
      }

      // Redirect to dashboard - user is already logged in
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err.message || "Registration failed. Please try again.");
      // If OTP verification failed, go back to OTP entry
      if (err.type === "OTP_VERIFICATION_FAILED") {
        setStep("otp");
      }
      console.error("Registration error:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Go back to previous step
   */
  const goBack = () => {
    if (step === "otp") {
      setStep("email");
      setOtp("");
      setError(null);
    } else if (step === "details") {
      setStep("otp");
      setError(null);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>Smart Campus Registration</h1>

        {/* Step 1: Email Entry */}
        {step === "email" && (
          <form onSubmit={handleSendOtp}>
            <div className="form-group">
              <label htmlFor="email">Email Address</label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your.email@smartcampus.edu"
                disabled={isProcessing}
                autoFocus
              />
              <small>Only @smartcampus.edu email addresses are accepted</small>
            </div>

            {error && <div className="error-message">{error}</div>}

            <button
              type="submit"
              disabled={isProcessing}
              className="btn btn-primary"
            >
              {isProcessing ? "Sending OTP..." : "Send OTP"}
            </button>

            <p className="auth-link">
              Already have an account? <a href="/login">Sign In</a>
            </p>
          </form>
        )}

        {/* Step 2: OTP Entry */}
        {step === "otp" && (
          <form onSubmit={handleVerifyOtp}>
            <p className="info-text">
              We've sent a 6-digit code to <strong>{email}</strong>
            </p>

            <div className="form-group">
              <label htmlFor="otp">Enter OTP Code</label>
              <input
                type="text"
                id="otp"
                value={otp}
                onChange={(e) =>
                  setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))
                }
                placeholder="000000"
                maxLength="6"
                disabled={isProcessing}
                autoFocus
              />
              {otpTimeRemaining !== null && (
                <small>
                  {otpTimeRemaining > 0
                    ? `Code expires in ${Math.floor(otpTimeRemaining / 60)}:${String(otpTimeRemaining % 60).padStart(2, "0")}`
                    : "Code has expired"}
                </small>
              )}
            </div>

            {error && <div className="error-message">{error}</div>}

            <button
              type="submit"
              disabled={isProcessing}
              className="btn btn-primary"
            >
              {isProcessing ? "Verifying..." : "Verify & Continue"}
            </button>

            <button
              type="button"
              onClick={goBack}
              disabled={isProcessing}
              className="btn btn-secondary"
            >
              Back
            </button>

            <p className="auth-link">
              Didn't receive the code?{" "}
              <a onClick={() => handleSendOtp()} style={{ cursor: "pointer" }}>
                Resend
              </a>
            </p>
          </form>
        )}

        {/* Step 3: Registration Details */}
        {step === "details" && (
          <form onSubmit={handleCompleteRegistration}>
            <p className="info-text">Complete your registration details</p>

            <div className="form-group">
              <label htmlFor="displayName">Full Name</label>
              <input
                type="text"
                id="displayName"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Your Full Name"
                disabled={isProcessing}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label htmlFor="roleRequested">Role</label>
              <select
                id="roleRequested"
                value={roleRequested}
                onChange={(e) => setRoleRequested(e.target.value)}
                disabled={isProcessing}
              >
                <option value="USER">Student</option>
                <option value="LECTURER">Lecturer</option>
                <option value="TECHNICIAN">Technician</option>
                <option value="FACILITY_MANAGER">Facility Manager</option>
              </select>
            </div>

            {roleRequested === "USER" ? (
              <div className="form-group">
                <label htmlFor="registrationNumber">Registration Number</label>
                <input
                  type="text"
                  id="registrationNumber"
                  value={registrationNumber}
                  onChange={(e) => setRegistrationNumber(e.target.value)}
                  placeholder="e.g., 20230001234"
                  disabled={isProcessing}
                />
              </div>
            ) : (
              <div className="form-group">
                <label htmlFor="employeeNumber">Employee Number</label>
                <input
                  type="text"
                  id="employeeNumber"
                  value={employeeNumber}
                  onChange={(e) => setEmployeeNumber(e.target.value)}
                  placeholder="e.g., EMP00123"
                  disabled={isProcessing}
                />
              </div>
            )}

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
                disabled={isProcessing}
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword">Confirm Password</label>
              <input
                type="password"
                id="confirmPassword"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Confirm your password"
                disabled={isProcessing}
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <button
              type="submit"
              disabled={isProcessing}
              className="btn btn-primary"
            >
              {isProcessing ? "Creating Account..." : "Complete Registration"}
            </button>

            <button
              type="button"
              onClick={goBack}
              disabled={isProcessing}
              className="btn btn-secondary"
            >
              Back
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default OtpRegistrationPage;
