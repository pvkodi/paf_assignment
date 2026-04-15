package com.sliitreserve.api.config.security;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.auth.SuspensionPolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter that enforces endpoint-level RBAC using EndpointAuthorizationConfig.
 * Runs after JWT authentication so SecurityContext has user authorities populated.
 */
@Component
@Slf4j
public class EndpointAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    private EndpointAuthorizationConfig endpointAuthorizationConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SuspensionPolicyService suspensionPolicyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isBlank() && path.startsWith(context)) {
            path = path.substring(context.length());
        }

        String endpointKey = request.getMethod() + " " + path;

        try {
            Set<Role> requiredRoles = endpointAuthorizationConfig.findRequiredRoles(endpointKey);

            // Public endpoints (empty required set) are allowed
            if (requiredRoles == null || requiredRoles.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                return;
            }

            String principal = authentication.getName();

            // Load user to check suspension status
            var userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (suspensionPolicyService.isSuspended(user) && !endpointAuthorizationConfig.isSuspendedUserWhitelisted(endpointKey)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    String msg = suspensionPolicyService.getSuspensionMessage(user);
                    response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"" + msg + "\"}");
                    return;
                }
            }

            Set<Role> userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.replace("ROLE_", ""))
                    .map(name -> {
                        try {
                            return Role.valueOf(name);
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .filter(r -> r != null)
                    .collect(Collectors.toSet());

            if (!endpointAuthorizationConfig.checkEndpointAccess(userRoles, endpointKey)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
                return;
            }

        } catch (Exception e) {
            log.error("Error during endpoint authorization for {}: {}", endpointKey, e.getMessage(), e);
            // Fail-safe: allow to proceed so GlobalExceptionHandler produces consistent responses
        }

        filterChain.doFilter(request, response);
    }
}

