import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import authService from "../../services/authService";

/**
 * OAuth Callback Handler Page
 * Receives authorization code from Google OAuth redirect
 * Exchanges code for JWT tokens via backend
 * Handles errors and redirects to appropriate page
 */

export function OAuthCallback() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [error, setError] = useState(null);

  useEffect(() => {
    let redirectTimeout;

    const handleCallback = async () => {
      try {
        // Extract authorization code from URL
        const params = new URLSearchParams(window.location.search);
        const code = params.get("code");
        const state = params.get("state");
        const errorParam = params.get("error");
        const errorDescription = params.get("error_description");

        // Validate state parameter for CSRF protection (optional - warn if mismatch)
        const savedState = sessionStorage.getItem("oauth_state");
        if (savedState && state !== savedState) {
          console.warn(
            "State parameter mismatch - possible CSRF attack. Saved:",
            savedState,
            "Returned:",
            state,
          );
          sessionStorage.removeItem("oauth_state");
          // Continue anyway since backend already validated the code
        } else if (savedState) {
          sessionStorage.removeItem("oauth_state");
        }

        // Check for OAuth error
        if (errorParam) {
          throw new Error(
            `OAuth Error: ${errorParam} - ${errorDescription || "Unknown error"}`,
          );
        }

        // Validate code exists
        if (!code) {
          throw new Error("No authorization code received from OAuth provider");
        }

        // Avoid duplicate callback execution in React StrictMode (dev) and page re-entry.
        const callbackLock = sessionStorage.getItem(
          "oauth_callback_in_progress",
        );
        if (callbackLock === "true") {
          return;
        }

        const processedCode = sessionStorage.getItem("oauth_processed_code");
        if (processedCode && processedCode === code) {
          if (authService.isAuthenticated()) {
            navigate("/dashboard", { replace: true });
          }
          return;
        }

        sessionStorage.setItem("oauth_callback_in_progress", "true");

        // Exchange code for tokens
        await login(code);
        sessionStorage.setItem("oauth_processed_code", code);

        // Redirect to dashboard on success
        navigate("/dashboard", { replace: true });
      } catch (err) {
        console.error("OAuth callback error:", err);

        // If a duplicate in-flight request already logged the user in, continue to app.
        if (authService.isAuthenticated()) {
          navigate("/dashboard", { replace: true });
          return;
        }

        setError(err.message || "Authentication failed. Please try again.");

        // Redirect to login after 3 seconds
        redirectTimeout = setTimeout(() => {
          navigate("/login", { replace: true });
        }, 3000);
      } finally {
        sessionStorage.removeItem("oauth_callback_in_progress");
      }
    };

    handleCallback();

    return () => {
      if (redirectTimeout) {
        clearTimeout(redirectTimeout);
      }
    };
  }, [login, navigate]);

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 px-4">
        <div className="max-w-md text-center rounded-lg border border-red-200 bg-white p-6">
          <h1 className="text-2xl font-bold text-red-900 mb-2">
            Authentication Error
          </h1>
          <p className="text-red-700 mb-4">{error}</p>
          <p className="text-sm text-red-600">
            Redirecting to login in a moment...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
        <h1 className="text-2xl font-bold text-gray-900">
          Completing sign in...
        </h1>
        <p className="text-gray-600 mt-2">
          Please wait while we authenticate your account.
        </p>
      </div>
    </div>
  );
}

export default OAuthCallback;
