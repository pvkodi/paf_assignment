import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import authService from "../services/authService";

/**
 * Auth Context
 * Provides authentication state and methods globally across the application
 * Manages user authentication, suspension state, and role-based access control
 * Per FR-001, FR-002, FR-003
 */

export const AuthContext = createContext(null);

/**
 * Auth Provider Component
 * Wraps application and provides auth state via context
 */
export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Initialize auth state on app load
   * Checks for stored token and validates it
   */
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = authService.getAccessToken();

        if (token && !authService.isTokenExpired()) {
          // Token exists and not expired
          const currentUser = authService.getCurrentUser();
          setUser(currentUser);
          setIsAuthenticated(true);

          // Try to fetch fresh profile from backend
          try {
            const profile = await authService.fetchUserProfile();
            setUser(profile);
          } catch (err) {
            // Profile fetch failed but we can still use stored user data
            console.warn("Failed to fetch profile:", err);
          }
        } else if (token && authService.isTokenExpired()) {
          // Token expired, try to refresh
          try {
            const newToken = await authService.refreshAccessToken();
            const currentUser = authService.getCurrentUser();
            setUser(currentUser);
            setIsAuthenticated(true);
          } catch (err) {
            // Refresh failed, logout
            authService.logout();
            setIsAuthenticated(false);
            setUser(null);
          }
        } else {
          // No token, not authenticated
          setIsAuthenticated(false);
          setUser(null);
        }
      } catch (err) {
        console.error("Auth initialization failed:", err);
        setError(err.message);
        setIsAuthenticated(false);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  /**
   * Handle login with OAuth
   * Exchanges Google token for JWT and stores auth state
   */
  const login = useCallback(async (googleToken) => {
    try {
      setLoading(true);
      setError(null);

      const response = await authService.exchangeGoogleToken(googleToken);

      // Store tokens and user
      authService.setAuthTokens(
        response.token || response.accessToken,
        response.refreshToken,
        response.user,
      );

      // Update state
      setUser(response.user);
      setIsAuthenticated(true);

      return response.user;
    } catch (err) {
      console.error("Login error:", err);
      setError(err.message || "Login failed");
      setIsAuthenticated(false);
      setUser(null);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Handle logout
   * Clears tokens and auth state
   */
  const logout = useCallback(async () => {
    try {
      setLoading(true);
      await authService.logout();
      setIsAuthenticated(false);
      setUser(null);
      setError(null);
    } catch (err) {
      console.error("Logout error:", err);
      // Still clear local state even if backend logout fails
      setIsAuthenticated(false);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Refresh user profile from backend
   */
  const refreshProfile = useCallback(async () => {
    try {
      const profile = await authService.fetchUserProfile();
      setUser(profile);
      return profile;
    } catch (err) {
      console.error("Profile refresh failed:", err);
      throw err;
    }
  }, []);

  /**
   * Check if user has required roles
   */
  const hasRole = useCallback((roles) => {
    return authService.hasRole(roles);
  }, []);

  /**
   * Check if user is suspended
   */
  const isSuspended = useCallback(() => {
    return authService.isSuspended();
  }, []);

  const value = {
    // State
    isAuthenticated,
    user,
    loading,
    error,

    // Methods
    login,
    logout,
    refreshProfile,
    hasRole,
    isSuspended,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook: Use Auth Context
 * Provides access to auth state and methods from anywhere in the app
 */
export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}

export default AuthContext;
