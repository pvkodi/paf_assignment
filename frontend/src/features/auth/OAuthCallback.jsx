import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";

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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        setLoading(true);

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

        // Exchange code for tokens
        await login(code);

        // Redirect to dashboard on success
        navigate("/dashboard", { replace: true });
      } catch (err) {
        console.error("OAuth callback error:", err);
        setError(err.message || "Authentication failed. Please try again.");
        setLoading(false);

        // Redirect to login after 3 seconds
        const timeout = setTimeout(() => {
          navigate("/login", { replace: true });
        }, 3000);

        return () => clearTimeout(timeout);
      }
    };

    handleCallback();
  }, [login, navigate]);

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-red-50">
        <div className="max-w-md text-center">
          <div className="text-red-600 text-6xl mb-4">❌</div>
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
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
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
