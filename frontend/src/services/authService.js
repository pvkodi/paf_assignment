/**
 * Authentication Service
 * Handles OAuth login, token management, refresh, validation, and logout
 * Integrates with backend OAuth/JWT endpoints per FR-001, FR-002, FR-003
 */

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080/api";

/**
 * Token storage keys
 */
const TOKEN_KEYS = {
  ACCESS_TOKEN: "accessToken",
  REFRESH_TOKEN: "refreshToken",
  USER: "user",
  TOKEN_EXPIRY: "tokenExpiry",
};

/**
 * Get stored access token
 */
const getAccessToken = () => {
  return localStorage.getItem(TOKEN_KEYS.ACCESS_TOKEN);
};

/**
 * Get stored refresh token
 */
const getRefreshToken = () => {
  return localStorage.getItem(TOKEN_KEYS.REFRESH_TOKEN);
};

/**
 * Get stored user profile
 */
const getCurrentUser = () => {
  const user = localStorage.getItem(TOKEN_KEYS.USER);
  return user ? JSON.parse(user) : null;
};

/**
 * Check if token is expired
 */
const isTokenExpired = () => {
  const expiry = localStorage.getItem(TOKEN_KEYS.TOKEN_EXPIRY);
  if (!expiry) return true;
  return Date.now() > parseInt(expiry);
};

/**
 * Check if user is authenticated
 */
const isAuthenticated = () => {
  const token = getAccessToken();
  return !!token && !isTokenExpired();
};

/**
 * Exchange Google OAuth token for backend session JWT
 * @param {string} googleToken - Token from Google OAuth provider
 * @param {string} redirectUri - OAuth redirect URI
 * @returns {Promise} Response with accessToken, refreshToken, and user
 */
const exchangeGoogleToken = async (googleToken) => {
  try {
    const response = await fetch(
      `${API_BASE_URL}/v1/auth/oauth/google/callback`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          code: googleToken,
          redirectUri: `${window.location.origin}/auth/callback`,
        }),
      },
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw {
        status: response.status,
        message: errorData.message || "OAuth exchange failed",
        code: errorData.code,
      };
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("OAuth exchange error:", error);
    throw {
      type: "OAUTH_EXCHANGE_FAILED",
      message: error.message || "Failed to exchange OAuth token",
      ...error,
    };
  }
};

/**
 * Store authentication tokens and user profile
 * @param {string} accessToken - JWT access token
 * @param {string} refreshToken - JWT refresh token (optional)
 * @param {object} user - User profile data
 * @param {number} expiresIn - Token expiry in seconds OR ISO timestamp string from backend
 */
const setAuthTokens = (accessToken, refreshToken, user, expiresIn = 86400) => {
  // Store tokens
  localStorage.setItem(TOKEN_KEYS.ACCESS_TOKEN, accessToken);
  if (refreshToken) {
    localStorage.setItem(TOKEN_KEYS.REFRESH_TOKEN, refreshToken);
  }

  // Store user profile
  if (user) {
    localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(user));
  }

  // Store expiry time - handle both epoch seconds and ISO timestamp from backend
  let expiryTime;
  if (typeof expiresIn === "string") {
    // expiresIn is an ISO timestamp from backend (e.g., "2025-08-15T10:30:00")
    expiryTime = new Date(expiresIn).getTime();
  } else {
    // expiresIn is in seconds (legacy)
    expiryTime = Date.now() + expiresIn * 1000;
  }
  localStorage.setItem(TOKEN_KEYS.TOKEN_EXPIRY, expiryTime.toString());
};

/**
 * Refresh access token using refresh token
 * @returns {Promise} New access token
 */
const refreshAccessToken = async () => {
  try {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      throw new Error("No refresh token available");
    }

    const response = await fetch(`${API_BASE_URL}/v1/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${refreshToken}`,
      },
    });

    if (!response.ok) {
      if (response.status === 401) {
        logout();
        throw { type: "REFRESH_FAILED_401", message: "Refresh token expired" };
      }
      throw { type: "REFRESH_FAILED", message: "Token refresh failed" };
    }

    const data = await response.json();
    const accessToken = data.accessToken || data.token;
    const newRefreshToken = data.refreshToken;

    setAuthTokens(accessToken, newRefreshToken, getCurrentUser());
    return accessToken;
  } catch (error) {
    console.error("Token refresh failed:", error);
    logout();
    throw error;
  }
};

/**
 * Validate token on backend
 * @param {string} token - JWT token to validate
 * @returns {Promise} Boolean indicating if token is valid
 */
const validateToken = async (token) => {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/auth/validate`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      return false;
    }

    const data = await response.json();
    return data.valid === true || response.status === 200;
  } catch (error) {
    console.error("Token validation error:", error);
    return false;
  }
};

/**
 * Get user profile from backend
 * @returns {Promise} User profile data
 */
const fetchUserProfile = async () => {
  try {
    const token = getAccessToken();
    if (!token) {
      throw new Error("No access token");
    }

    const response = await fetch(`${API_BASE_URL}/v1/auth/profile`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw { status: response.status, message: "Failed to fetch profile" };
    }

    const user = await response.json();

    // Update stored user profile
    const currentUser = getCurrentUser();
    const updatedUser = { ...currentUser, ...user };
    localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(updatedUser));

    return updatedUser;
  } catch (error) {
    console.error("Failed to fetch user profile:", error);
    throw error;
  }
};

/**
 * Logout user - clear tokens and navigate to login
 */
const logout = async () => {
  try {
    const token = getAccessToken();
    if (token) {
      // Notify backend of logout
      await fetch(`${API_BASE_URL}/v1/auth/logout`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      }).catch(() => {
        // Logout endpoint may fail, but we still clear local tokens
      });
    }
  } finally {
    // Clear all stored tokens and user data
    localStorage.removeItem(TOKEN_KEYS.ACCESS_TOKEN);
    localStorage.removeItem(TOKEN_KEYS.REFRESH_TOKEN);
    localStorage.removeItem(TOKEN_KEYS.USER);
    localStorage.removeItem(TOKEN_KEYS.TOKEN_EXPIRY);
  }
};

/**
 * Handle OAuth callback from redirect
 * Extracts code from URL params and exchanges for tokens
 * @returns {Promise} Response with tokens and user data
 */
const handleOAuthCallback = async () => {
  try {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");

    if (!code) {
      throw new Error("No authorization code in callback");
    }

    const response = await exchangeGoogleToken(code);
    return response;
  } catch (error) {
    console.error("OAuth callback error:", error);
    throw error;
  }
};

/**
 * Get authorization header for API requests
 * @returns {object} Authorization header object
 */
const getAuthHeaders = () => {
  const token = getAccessToken();
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
};

/**
 * Parse JWT to extract claims (without verification - client-side only)
 * @param {string} token - JWT token
 * @returns {object} Decoded token payload
 */
const parseJwt = (token) => {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error("JWT parsing error:", error);
    return null;
  }
};

/**
 * Check if user has required roles
 * @param {string|array} requiredRoles - Role or array of roles to check
 * @returns {boolean} True if user has any of required roles
 */
const hasRole = (requiredRoles) => {
  const user = getCurrentUser();
  if (!user || !user.roles) return false;

  const userRoles = Array.isArray(user.roles) ? user.roles : [user.roles];
  const required = Array.isArray(requiredRoles)
    ? requiredRoles
    : [requiredRoles];

  return userRoles.some((role) => required.includes(role));
};

/**
 * Check if user is suspended
 * @returns {boolean} True if user is suspended
 */
const isSuspended = () => {
  const user = getCurrentUser();
  return user?.suspended === true;
};

/**
 * Login with email and password
 * @param {string} email - User email
 * @param {string} password - User password
 * @returns {Promise} Response with accessToken, refreshToken, and user
 */
const loginWithEmailPassword = async (email, password) => {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email,
        password,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw {
        status: response.status,
        message: errorData.message || "Login failed",
        code: errorData.code,
      };
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Email/password login error:", error);
    throw {
      type: "LOGIN_FAILED",
      message: error.message || "Failed to login",
      ...error,
    };
  }
};

/**
 * Register new user with email and password
 * Creates a registration request awaiting admin approval
 * @param {string} email - User email
 * @param {string} displayName - User display name
 * @param {string} password - User password
 * @param {string} confirmPassword - Password confirmation
 * @param {string} roleRequested - User role (USER, LECTURER, TECHNICIAN, FACILITY_MANAGER)
 * @param {string} registrationNumber - Registration number (required if role=USER)
 * @param {string} employeeNumber - Employee number (required if role!=USER)
 * @returns {Promise} Response with registration status and ID
 */
const registerWithEmailPassword = async (
  email,
  displayName,
  password,
  confirmPassword,
  roleRequested = "USER",
  registrationNumber = "",
  employeeNumber = "",
) => {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/auth/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email,
        displayName,
        password,
        confirmPassword,
        roleRequested,
        registrationNumber,
        employeeNumber,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw {
        status: response.status,
        message: errorData.message || "Registration failed",
        code: errorData.code,
      };
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Registration error:", error);
    throw {
      type: "REGISTRATION_FAILED",
      message: error.message || "Failed to register",
      ...error,
    };
  }
};

/**
 * Send OTP to email address
 * NEW OTP-based registration flow (no admin approval needed)
 * Step 1: Request OTP
 */
const sendOtp = async (email) => {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/auth/otp/send`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email }),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw {
        status: response.status,
        message: errorData.message || "Failed to send OTP",
        code: errorData.code,
      };
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Send OTP error:", error);
    throw {
      type: "OTP_SEND_FAILED",
      message: error.message || "Failed to send OTP",
      ...error,
    };
  }
};

/**
 * Verify OTP and complete registration
 * NEW OTP-based registration flow (no admin approval needed)
 * Step 2: Verify OTP and auto-register
 */
const verifyOtpAndRegister = async (
  email,
  otp,
  displayName,
  password,
  confirmPassword,
  roleRequested = "USER",
  registrationNumber = "",
  employeeNumber = "",
) => {
  try {
    const response = await fetch(
      `${API_BASE_URL}/v1/auth/otp/verify-and-register`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          otp,
          displayName,
          password,
          confirmPassword,
          roleRequested,
          registrationNumber,
          employeeNumber,
        }),
      },
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw {
        status: response.status,
        message: errorData.message || "OTP verification failed",
        code: errorData.code,
      };
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("OTP verification error:", error);
    throw {
      type: "OTP_VERIFICATION_FAILED",
      message: error.message || "Failed to verify OTP and register",
      ...error,
    };
  }
};

export const authService = {
  // Token management
  getAccessToken,
  getRefreshToken,
  setAuthTokens,
  refreshAccessToken,
  validateToken,
  isTokenExpired,
  parseJwt,

  // User management
  getCurrentUser,
  fetchUserProfile,
  hasRole,
  isSuspended,

  // Authentication state
  isAuthenticated,

  // OAuth
  exchangeGoogleToken,
  handleOAuthCallback,

  // Email/Password authentication
  loginWithEmailPassword,
  registerWithEmailPassword,

  // OTP-based registration (new flow)
  sendOtp,
  verifyOtpAndRegister,

  // Logout
  logout,

  // Utilities
  getAuthHeaders,
};

export default authService;
