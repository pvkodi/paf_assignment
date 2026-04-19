package com.sliitreserve.api.config.security;

import com.sliitreserve.api.entities.auth.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Endpoint Authorization Configuration for Role-Based Access Control (RBAC).
 *
 * <p><b>Purpose</b>: Defines and centrally manages role-based access control (RBAC) rules for all API
 * endpoints. Enforces FR-002 (role assignment), FR-003 (suspension exceptions), and SV-001 (endpoint
 * access matrix).
 *
 * <p><b>Key Responsibilities</b>:
 * <ul>
 *   <li>Define required roles for each endpoint pattern
 *   <li>Provide whitelist for suspended-user access exceptions
 *   <li>Enable role-hierarchy and inheritance (ADMIN > all, LECTURER > USER, etc.)
 *   <li>Support dynamic role checks and audit logging
 * </ul>
 *
 * <p><b>Access Control Hierarchy</b> (most permissive):
 * <ul>
 *   <li><b>ADMIN</b>: Full access to all endpoints
 *   <li><b>FACILITY_MANAGER</b>: Facility, booking approvals, ticket assignment
 *   <li><b>LECTURER</b>: Booking creation/approval, facility search
 *   <li><b>TECHNICIAN</b>: Ticket creation/updates, check-ins
 *   <li><b>USER</b>: Booking creation, facility search, ticket creation
 * </ul>
 *
 * <p><b>Authorization Flow</b> (integrated with Spring Security):
 * <ol>
 *   <li>JwtAuthenticationFilter validates JWT and extracts user email
 *   <li>UserRepository loads User entity with roles
 *   <li>SuspensionPolicyService checks if user is suspended
 *   <li>If suspended, only whitelist endpoints are allowed
 *   <li>For non-whitelisted endpoints, checkEndpointAccess() validates role
 *   <li>If no role match, throw ForbiddenException (403)
 * </ol>
 *
 * <p><b>Integration Points</b>:
 * <ul>
 *   <li>AuthController endpoints (oauth, profile, logout)
 *   <li>BookingController endpoints (create, approve, check-in)
 *   <li>FacilityController endpoints (search)
 *   <li>TicketController endpoints (create, comment, assign)
 *   <li>AppealController endpoints (create appeal)
 *   <li>AnalyticsController endpoints (utilization)
 *   <li>NotificationController endpoints (list, mark as read)
 * </ul>
 *
 * @see SuspensionPolicyService for suspension enforcement
 * @see com.sliitreserve.api.controllers.AuthController for auth endpoints
 * @see com.sliitreserve.api.exceptions.ForbiddenException for access denied responses
 */
@Slf4j
@Component
public class EndpointAuthorizationConfig {

    /**
     * Endpoint access matrix: Maps endpoint patterns to required roles.
     *
     * <p><b>Pattern Matching Rules</b>:
     * - Exact match: "/api/v1/auth/profile"
     * - Prefix match: "/api/v1/bookings/*"
     * - Wildcard: "/api/v1/admin/**"
     *
     * <p><b>Role Resolution</b>:
     * - If user has ANY of the required roles, access is granted
     * - ADMIN role bypasses all checks (has implicit access)
     * - Empty role set = public endpoint (no auth required)
     */
    private static final Map<String, Set<Role>> ENDPOINT_ACCESS_MATRIX = new LinkedHashMap<>();

    // ========== Authorization Constants ==========
    public static final String WHITELISTED_SUSPENDED = "profile|logout|appeals";

    static {
        // ========== AUTH ENDPOINTS ==========
        // FR-001: OAuth endpoints are public (no auth required)
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/auth/oauth/google/callback", new HashSet<>());

        // Email/Password authentication: public endpoints
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/auth/register", new HashSet<>());
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/auth/login", new HashSet<>());

        // OTP-based registration: public endpoints (no auth required)
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/auth/otp/send", new HashSet<>());
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/auth/otp/verify-and-register", new HashSet<>());

        // ========== FILE UPLOADS (PUBLIC) ==========
        // Allow downloads of uploaded attachments without authentication
        ENDPOINT_ACCESS_MATRIX.put("GET /api/uploads/**", new HashSet<>());
        ENDPOINT_ACCESS_MATRIX.put("GET /api/uploads/original/*", new HashSet<>());
        ENDPOINT_ACCESS_MATRIX.put("GET /api/uploads/thumbnails/*", new HashSet<>());

        // FR-003: Profile and logout are whitelisted for suspended users
        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/auth/profile",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/auth/logout",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // ========== FACILITY ENDPOINTS (US2) ==========
        // FR-006: All authenticated users can search facilities
        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/facilities",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/facilities/search",
            new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/facilities/*",
            new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/facilities",
            new HashSet<>(Collections.singletonList(Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("PUT /api/v1/facilities/*",
            new HashSet<>(Collections.singletonList(Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("DELETE /api/v1/facilities/*",
            new HashSet<>(Collections.singletonList(Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/facilities/*/utilization",
            new HashSet<>(Collections.singletonList(Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/facilities/underutilized",
            new HashSet<>(Collections.singletonList(Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/facilities/suggestions",
            new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // ========== BOOKING ENDPOINTS (US2, US3) ==========
        // FR-007: All users can create booking requests
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/bookings",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.FACILITY_MANAGER, Role.ADMIN)));

        // FR-015, FR-016, FR-017: Approvers can approve bookings
        // LECTURER can approve USER bookings
        // FACILITY_MANAGER can approve high-capacity hall bookings
        // ADMIN can approve all bookings
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/bookings/*/approve",
                new HashSet<>(Arrays.asList(Role.LECTURER, Role.FACILITY_MANAGER, Role.ADMIN)));

        // FR-020: Check-in by staff or manual by end-user
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/bookings/*/check-in",
                new HashSet<>(Arrays.asList(Role.USER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // ========== APPEAL ENDPOINTS (US3) ==========
        // FR-023: Suspended users can submit appeals
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/appeals",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // FR-023: Only ADMIN can approve/reject appeals
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/appeals/*/approve",
                new HashSet<>(Collections.singletonList(Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/appeals/*/reject",
                new HashSet<>(Collections.singletonList(Role.ADMIN)));

        // ========== TICKET ENDPOINTS (US4) ==========
        // FR-024: All authenticated users can create tickets
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/tickets",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // Ticket viewing: All authenticated users
        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/tickets", 
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/tickets/*",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // FR-030: All users can comment on tickets
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/tickets/*/comments",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // FR-029: Assignment by technicians, managers, or admins
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/tickets/*/assign",
                new HashSet<>(Arrays.asList(Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // Ticket status updates: Staff roles only
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/tickets/*/status",
                new HashSet<>(Arrays.asList(Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // FR-030, FR-031: File attachments - all authenticated users can upload
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/tickets/*/attachments",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/tickets/*/attachments",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("DELETE /api/v1/tickets/*/attachments/*",
                new HashSet<>(Collections.singletonList(Role.ADMIN)));

        // ========== ACTUAL ENDPOINT PATHS (without /v1) ==========
        // These are the actual paths used by controllers
        ENDPOINT_ACCESS_MATRIX.put("POST /api/auth/login", new HashSet<>());
        ENDPOINT_ACCESS_MATRIX.put("POST /api/auth/register", new HashSet<>());
        ENDPOINT_ACCESS_MATRIX.put("GET /api/auth/profile",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/tickets",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/tickets",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/tickets/*",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/tickets/*/comments",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/tickets/*/assign",
                new HashSet<>(Arrays.asList(Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/tickets/*/status",
                new HashSet<>(Arrays.asList(Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("POST /api/tickets/*/attachments",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("GET /api/tickets/*/attachments",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        ENDPOINT_ACCESS_MATRIX.put("DELETE /api/tickets/*/attachments/*",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // ========== NOTIFICATION ENDPOINTS (US5) ==========
        // All authenticated users can view their notifications
        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/notifications",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // Mark notification as read
        ENDPOINT_ACCESS_MATRIX.put("POST /api/v1/notifications/*/read",
                new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN)));

        // ========== ANALYTICS ENDPOINTS (US5) ==========
        // FR-036: Admin-only utilization analytics
        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/analytics/utilization",
                new HashSet<>(Collections.singletonList(Role.ADMIN)));

        // Admin-only analytics recommendations
        ENDPOINT_ACCESS_MATRIX.put("GET /api/v1/analytics/recommendations",
                new HashSet<>(Collections.singletonList(Role.ADMIN)));
    }

    /**
     * Check if user has access to a specific endpoint.
     *
     * <p>Uses the endpoint access matrix to determine required roles.
     * ADMIN role has implicit access to all endpoints.
     *
     * <p><b>Usage</b>: Call from controller methods or security advisors:
     * <pre>{@code
     * if (!endpointAuthConfig.checkEndpointAccess(user.getRoles(), "GET /api/v1/bookings")) {
     *     throw new ForbiddenException("Access denied to bookings endpoint");
     * }
     * }</pre>
     *
     * @param userRoles Set of roles assigned to user
     * @param endpointPattern Endpoint pattern (e.g., "GET /api/v1/bookings")
     * @return true if user has at least one required role; false otherwise
     */
    public boolean checkEndpointAccess(Set<Role> userRoles, String endpointPattern) {
        if (userRoles == null || userRoles.isEmpty()) {
            log.debug("Access check: no roles provided for endpoint {}", endpointPattern);
            return false;
        }

        // ADMIN role has implicit access to all endpoints
        if (userRoles.contains(Role.ADMIN)) {
            log.debug("Access granted: ADMIN role for endpoint {}", endpointPattern);
            return true;
        }

        // Find required roles for this endpoint
        Set<Role> requiredRoles = findRequiredRoles(endpointPattern);

        if (requiredRoles == null || requiredRoles.isEmpty()) {
            // Public endpoint (no auth required)
            log.debug("Access granted: public endpoint {}", endpointPattern);
            return true;
        }

        // Check if user has any of the required roles
        boolean hasAccess = userRoles.stream().anyMatch(requiredRoles::contains);

        if (hasAccess) {
            log.debug("Access granted to endpoint {}: user roles {} match required {}", 
                endpointPattern, userRoles, requiredRoles);
        } else {
            log.warn("Access denied to endpoint {}: user roles {} do not match required {}", 
                endpointPattern, userRoles, requiredRoles);
        }

        return hasAccess;
    }

    /**
     * Get required roles for an endpoint pattern.
     *
     * <p>Performs pattern matching to find the endpoint in the access matrix:
     * - Exact matches are preferred
     * - Wildcard patterns (* and **) are matched
     *
     * @param endpointPattern Endpoint pattern (e.g., "GET /api/v1/bookings", "POST /api/v1/bookings/123/approve")
     * @return Set of required roles, or empty set for public endpoints, or null if not found
     */
    public Set<Role> findRequiredRoles(String endpointPattern) {
        if (endpointPattern == null || endpointPattern.isBlank()) {
            return null;
        }

        // Try exact match first
        if (ENDPOINT_ACCESS_MATRIX.containsKey(endpointPattern)) {
            return ENDPOINT_ACCESS_MATRIX.get(endpointPattern);
        }

        // Try pattern matching (e.g., "POST /api/v1/bookings/123/approve" matches "POST /api/v1/bookings/*/approve")
        for (Map.Entry<String, Set<Role>> entry : ENDPOINT_ACCESS_MATRIX.entrySet()) {
            if (matches(endpointPattern, entry.getKey())) {
                return entry.getValue();
            }
        }

        // Not found in matrix - assume protected (require auth)
        log.warn("Endpoint not found in authorization matrix: {}. Defaulting to protected.", endpointPattern);
        return new HashSet<>(Arrays.asList(Role.USER, Role.LECTURER, Role.TECHNICIAN, Role.FACILITY_MANAGER, Role.ADMIN));
    }

    /**
     * Check if endpoint pattern matches a URL (with wildcards).
     *
     * <p><b>Matching Rules</b>:
     * - "*" matches a single path segment (e.g., "/bookings/123" matches "/bookings/*")
     * - "**" matches multiple segments (e.g., "/api/v1/admin/users/123" matches "/api/v1/**")
     *
     * @param url Actual request URL (e.g., "GET /api/v1/bookings/123/approve")
     * @param pattern Pattern with wildcards 
     * @return true if URL matches pattern; false otherwise
     */
    private boolean matches(String url, String pattern) {
        // Split method and path
        String[] urlParts = url.split(" ");
        String[] patternParts = pattern.split(" ");

        if (urlParts.length != 2 || patternParts.length != 2) {
            return false;
        }

        // Match HTTP method (GET, POST, etc.)
        if (!urlParts[0].equalsIgnoreCase(patternParts[0])) {
            return false;
        }

        // Match path segments
        String urlPath = urlParts[1];
        String patternPath = patternParts[1];

        return matchPath(urlPath, patternPath);
    }

    /**
     * Match a URL path against a pattern with wildcard support.
     *
     * @param path Actual URL path (e.g., "/api/v1/bookings/123/approve")
     * @param pattern Pattern with wildcards 
     * @return true if path matches pattern; false otherwise
     */
    private boolean matchPath(String path, String pattern) {
        String[] pathSegments = path.split("/");
        String[] patternSegments = pattern.split("/");

        if (patternSegments[patternSegments.length - 1].equals("**")) {
            // ** pattern matches any remaining segments
            return pathSegments.length >= patternSegments.length - 1;
        }

        if (pathSegments.length != patternSegments.length) {
            return false;
        }

        for (int i = 0; i < patternSegments.length; i++) {
            if (!patternSegments[i].equals("*") && !patternSegments[i].equals(pathSegments[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if endpoint is whitelisted for suspended users.
     *
     * <p>FR-003: Suspended users are allowed to access: auth/session, profile view, and suspension
     * appeal submission.
     *
     * @param endpointPattern Endpoint pattern (e.g., "GET /api/v1/auth/profile")
     * @return true if suspended users are allowed on this endpoint
     */
    public boolean isSuspendedUserWhitelisted(String endpointPattern) {
        if (endpointPattern == null || endpointPattern.isBlank()) {
            return false;
        }

        // Whitelisted patterns for suspended users
        return endpointPattern.contains("profile") || 
               endpointPattern.contains("logout") || 
               endpointPattern.contains("appeals");
    }

    /**
     * Get all endpoints that require a specific role.
     *
     * <p>Useful for auditing and documentation: "What can a LECTURER do?"
     *
     * @param role Role to query
     * @return Set of endpoint patterns that require this role
     */
    public Set<String> getEndpointsForRole(Role role) {
        Set<String> endpoints = new HashSet<>();

        for (Map.Entry<String, Set<Role>> entry : ENDPOINT_ACCESS_MATRIX.entrySet()) {
            if (entry.getValue().isEmpty() || entry.getValue().contains(role)) {
                endpoints.add(entry.getKey());
            }
        }

        return endpoints;
    }

    /**
     * Get all configured endpoints.
     *
     * <p>Useful for documentation and testing.
     *
     * @return Unmodifiable set of all endpoint patterns
     */
    public Set<String> getAllEndpoints() {
        return Collections.unmodifiableSet(ENDPOINT_ACCESS_MATRIX.keySet());
    }

    /**
     * Get the access matrix for testing or documentation purposes.
     *
     * <p><b>Note</b>: Returns an unmodifiable copy to prevent runtime modifications.
     *
     * @return Unmodifiable copy of endpoint access matrix
     */
    public Map<String, Set<Role>> getAccessMatrix() {
        return Collections.unmodifiableMap(ENDPOINT_ACCESS_MATRIX);
    }

    /**
     * Log the complete authorization configuration for audit or debugging.
     *
     * Call this during application startup to verify RBAC configuration is loaded correctly.
     */
    public void logAuthorizationMatrix() {
        log.info("========== ENDPOINT AUTHORIZATION MATRIX ==========");
        log.info("Total endpoints configured: {}", ENDPOINT_ACCESS_MATRIX.size());
        
        for (Map.Entry<String, Set<Role>> entry : ENDPOINT_ACCESS_MATRIX.entrySet()) {
            String roles = entry.getValue().isEmpty() ? "PUBLIC" : 
                          entry.getValue().stream()
                              .map(Role::name)
                              .sorted()
                              .reduce((a, b) -> a + ", " + b)
                              .orElse("NONE");
            
            String whitelisted = isSuspendedUserWhitelisted(entry.getKey()) ? " [SUSPENDED_WHITELIST]" : "";
            log.info("  {} -> {}{}", entry.getKey(), roles, whitelisted);
        }
        
        log.info("=================================================");
    }
}
