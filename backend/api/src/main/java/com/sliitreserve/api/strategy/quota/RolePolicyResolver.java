package com.sliitreserve.api.strategy.quota;

import com.sliitreserve.api.entities.auth.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Role-Policy Resolver for selecting applicable quota strategy.
 * 
 * Resolves user roles to the most permissive applicable quota strategy.
 * When a user has multiple roles, the resolver selects the role with highest permissiveness priority.
 * 
 * This ensures that users benefit from the most favorable policy available to them.
 * For example:
 * - User with both STUDENT and LECTURER roles gets LECTURER policy (more permissive)
 * - User with LECTURER and ADMIN roles gets ADMIN policy (most permissive)
 * - User with only STUDENT role gets STUDENT policy
 * 
 * Strategy lookup:
 * - Strategies are registered with role keys (e.g., "USER", "LECTURER", "ADMIN")
 * - Registry is populated at application startup (T053 will implement concrete strategies)
 * - Unknown roles are rejected with clear error messaging
 * 
 * Usage:
 * <pre>
 * Set<String> userRoles = user.getRoles(); // e.g., {"USER", "LECTURER"}
 * QuotaStrategy strategy = resolver.resolveStrategy(userRoles);
 * 
 * // Now use strategy for quota enforcement
 * if (!strategy.canBookDuringPeakHours(bookedTime)) {
 *     throw new QuotaException("Peak hour booking denied for role: " + strategy.getRoleName());
 * }
 * </pre>
 */
@Slf4j
@Component
public class RolePolicyResolver {

    private final Map<String, QuotaStrategy> strategyRegistry = new HashMap<>();

    /**
     * Register a quota strategy for a role.
     * Called at application startup by T053 (quota strategy implementations).
     * 
     * @param roleName Role identifier (e.g., "USER", "LECTURER", "ADMIN")
     * @param strategy Strategy implementation for the role
     */
    public void registerStrategy(String roleName, QuotaStrategy strategy) {
        log.debug("Registering quota strategy for role: {}", roleName);
        strategyRegistry.put(roleName, strategy);
    }

    /**
     * Resolve the most permissive applicable strategy from a collection of user roles.
     * 
     * Algorithm:
     * 1. Look up each role in the strategy registry
     * 2. Compare strategies by permissivenessPriority() (higher = more permissive)
     * 3. Return the strategy with the highest priority
     * 
     * Example:
     * - Input roles: {"USER", "LECTURER"}
     * - USER strategy priority: 1
     * - LECTURER strategy priority: 2
     * - Returns LECTURER strategy (priority 2 > 1)
     * 
     * @param roleNames Collection of role identifiers (case-sensitive)
     * @return Most permissive applicable strategy
     * @throws IllegalArgumentException if no strategies found for any role, or if roleNames is empty
     */
    public QuotaStrategy resolveStrategy(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            log.warn("No roles provided for policy resolution");
            throw new IllegalArgumentException("At least one role must be provided");
        }

        log.debug("Resolving policy for roles: {}", roleNames);

        // Find the most permissive strategy among user's roles
        QuotaStrategy selectedStrategy = roleNames.stream()
                .map(roleName -> {
                    QuotaStrategy strategy = strategyRegistry.get(roleName);
                    if (strategy == null) {
                        log.warn("No strategy registered for role: {}", roleName);
                        throw new IllegalArgumentException("Unknown role: " + roleName);
                    }
                    return strategy;
                })
                .max(Comparator.comparingInt(QuotaStrategy::getPermissivenessPriority))
                .orElseThrow(() -> new IllegalArgumentException("Unable to resolve policy for roles: " + roleNames));

        log.debug("Resolved to strategy: {} with priority: {}", 
                  selectedStrategy.getRoleName(), 
                  selectedStrategy.getPermissivenessPriority());

        return selectedStrategy;
    }

    /**
     * Check if a role is registered with a strategy.
     * Useful for validation before resolveStrategy() is called.
     * 
     * @param roleName Role identifier to check
     * @return true if role has a registered strategy, false otherwise
     */
    public boolean hasStrategy(String roleName) {
        return strategyRegistry.containsKey(roleName);
    }

    /**
     * Get the strategy for a specific role.
     * Useful for direct role-specific lookups and testing.
     * 
     * @param roleName Role identifier
     * @return Strategy for the role, or null if not registered
     */
    public QuotaStrategy getStrategy(String roleName) {
        return strategyRegistry.get(roleName);
    }

    /**
     * Get all registered role names.
     * Useful for listing available policies.
     * 
     * @return Set of registered role names
     */
    public java.util.Set<String> getRegisteredRoles() {
        return strategyRegistry.keySet();
    }

    /**
     * Get the most permissive role from a set of roles (FR-042).
     * 
     * For users with multiple roles, this resolves to the most permissive role
     * based on the permissiveness hierarchy:
     * ADMIN > FACILITY_MANAGER > LECTURER > TECHNICIAN > USER
     * 
     * @param roles Set of Role enums to evaluate
     * @return The most permissive Role from the set
     * @throws IllegalArgumentException if roles set is null or empty
     */
    public Role getMostPermissiveRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Roles set cannot be null or empty");
        }

        // Permissiveness hierarchy (higher value = more permissive)
        Map<Role, Integer> rolePriority = new HashMap<>();
        rolePriority.put(Role.ADMIN, 5);
        rolePriority.put(Role.FACILITY_MANAGER, 4);
        rolePriority.put(Role.LECTURER, 3);
        rolePriority.put(Role.TECHNICIAN, 2);
        rolePriority.put(Role.USER, 1);

        return roles.stream()
                .max(Comparator.comparingInt(r -> rolePriority.getOrDefault(r, 0)))
                .orElseThrow(() -> new IllegalArgumentException("Unable to determine most permissive role"));
    }
}
