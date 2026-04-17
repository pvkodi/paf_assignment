import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";

/**
 * Registration Pending Page Component
 * Shows pending message after user submits registration request
 * User sees this page while awaiting admin approval
 * Upon approval, user receives email and can login
 * Upon rejection, user receives email with reason and can resubmit
 */
export function RegistrationPendingPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email || "your email";
  const [timeRemaining, setTimeRemaining] = useState(300); // 5 minutes

  useEffect(() => {
    // Countdown timer
    const timer = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const handleBackToLogin = () => {
    navigate("/login");
  };

  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-lg p-8">
          {/* Success Icon */}
          <div className="flex justify-center mb-6">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
              <svg
                className="w-8 h-8 text-green-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
          </div>

          {/* Header */}
          <h1 className="text-3xl font-bold text-center text-gray-900 mb-2">
            Registration Submitted
          </h1>
          <p className="text-center text-gray-600 mb-6">
            Thank you for registering!
          </p>

          {/* Status Message */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0">
                <svg
                  className="h-6 w-6 text-blue-600 animate-spin"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
              </div>
              <div className="flex-1">
                <h3 className="text-sm font-medium text-blue-900 mb-1">
                  Registration Approval Pending
                </h3>
                <p className="text-sm text-blue-700">
                  Your registration request has been submitted to our admin
                  team. You'll receive an email at{" "}
                  <span className="font-semibold">{email}</span> when we've
                  reviewed your application.
                </p>
              </div>
            </div>
          </div>

          {/* Details */}
          <div className="space-y-4 mb-6">
            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
                What Happens Next
              </p>
              <ol className="text-sm text-gray-700 space-y-2">
                <li className="flex gap-2">
                  <span className="flex-shrink-0 font-semibold text-indigo-600">
                    1.
                  </span>
                  <span>Our admin team reviews your submission</span>
                </li>
                <li className="flex gap-2">
                  <span className="flex-shrink-0 font-semibold text-indigo-600">
                    2.
                  </span>
                  <span>You'll receive an approval or rejection email</span>
                </li>
                <li className="flex gap-2">
                  <span className="flex-shrink-0 font-semibold text-indigo-600">
                    3.
                  </span>
                  <span>If approved, you can log in immediately</span>
                </li>
              </ol>
            </div>

            <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg">
              <p className="text-xs font-semibold text-amber-900 uppercase tracking-wide mb-1">
                Typical Response Time
              </p>
              <p className="text-sm text-amber-800">
                We typically respond to registration requests within 24-48 hours
              </p>
            </div>
          </div>

          {/* Check Status Button */}
          <button
            onClick={() => {
              // TODO: Implement check registration status endpoint
              // For now, just show a message
              alert(
                "Status check functionality coming soon.\nCheck your email for updates.",
              );
            }}
            className="w-full py-2 px-4 mb-4 text-center text-indigo-600 border-2 border-indigo-600 hover:bg-indigo-50 font-medium rounded-lg transition"
          >
            Check Status
          </button>

          {/* Back to Login */}
          <button
            onClick={handleBackToLogin}
            className="w-full py-3 px-4 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition"
          >
            Back to Login
          </button>

          {/* Footer Info */}
          <p className="text-xs text-gray-500 text-center mt-6">
            Check your email (including spam folder) for updates. Contact
            support if you don't receive a response within 48 hours.
          </p>

          {/* Timer */}
          <div className="text-center mt-4">
            <p className="text-xs text-gray-400">
              This page auto-closes in {formatTime(timeRemaining)}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
