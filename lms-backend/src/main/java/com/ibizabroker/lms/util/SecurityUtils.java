package com.ibizabroker.lms.util;

import com.ibizabroker.lms.entity.Users;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * 🔐 Security Utilities - Centralized Spring Security Context Access
 * 
 * ✅ Purpose:
 * - Type-safe access to current authenticated user
 * - No direct SecurityContextHolder access in Controllers/Services
 * - Clean separation of security concerns
 * 
 * 📌 Usage Examples:
 * ```java
 * // Get current user ID (for business logic)
 * Integer userId = SecurityUtils.getCurrentUserId()
 *     .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
 * 
 * // Get current username (for logging)
 * String username = SecurityUtils.getCurrentUsername()
 *     .orElse("anonymous");
 * 
 * // Check if authenticated
 * if (SecurityUtils.isAuthenticated()) {
 *     // Protected logic
 * }
 * ```
 * 
 * 🎯 Benefits:
 * - Single source of truth for auth context
 * - Easy to mock in unit tests
 * - Consistent error handling
 * 
 * @author Library Management System
 * @since Phase 8: Security Enhancement
 */
public class SecurityUtils {

    /**
     * 👤 Get current authenticated user's ID
     * 
     * ⚠️ IMPORTANT: This assumes your UserDetails implementation has getUserId() method
     * Adjust the cast based on your actual UserDetails class (e.g., CustomUserDetails)
     * 
     * @return Optional<Integer> containing userId, or empty if not authenticated
     */
    public static Optional<Integer> getCurrentUserId() {
        return getAuthentication()
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof UserDetails)
            .map(principal -> {
                // TODO: Replace with your actual UserDetails implementation
                // Example: if (principal instanceof CustomUserDetails userDetails) {
                //     return userDetails.getUserId();
                // }
                
                // Fallback: Try to extract from username if it's numeric
                UserDetails userDetails = (UserDetails) principal;
                String username = userDetails.getUsername();
                if (username != null && username.matches("\\d+")) {
                    return Integer.parseInt(username);
                }
                return null;
            });
    }

    /**
     * 👤 Get current authenticated username
     * 
     * @return Optional<String> containing username, or empty if not authenticated
     */
    public static Optional<String> getCurrentUsername() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .filter(name -> !"anonymousUser".equals(name));
    }

    /**
     * 🔍 Get current Authentication object
     * 
     * @return Optional<Authentication> from SecurityContextHolder
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(ctx -> ctx.getAuthentication());
    }

    /**
     * ✅ Check if user is authenticated (not anonymous)
     * 
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .filter(name -> !"anonymousUser".equals(name))
            .isPresent();
    }

    /**
     * 🔐 Get current UserDetails (if available)
     * 
     * @return Optional<UserDetails> or empty if not authenticated
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        return getAuthentication()
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof UserDetails)
            .map(principal -> (UserDetails) principal);
    }

    /**
     * 🎯 Get current user entity (requires repository lookup)
     * 
     * ⚠️ Note: This method requires database access. Use sparingly.
     * Prefer passing userId to service methods instead.
     * 
     * Example implementation:
     * ```java
     * public static Optional<Users> getCurrentUser(UsersRepository usersRepo) {
     *     return getCurrentUsername()
     *         .flatMap(usersRepo::findByUsername);
     * }
     * ```
     * 
     * @deprecated Use getCurrentUserId() and pass to service layer instead
     */
    @Deprecated
    public static void getCurrentUserExample() {
        // This is intentionally not implemented to avoid repository dependency in util class
        // Controllers/Services should use: usersRepository.findById(SecurityUtils.getCurrentUserId())
    }

    /**
     * 🚫 Check if user has specific role
     * 
     * @param role Role name (e.g., "ROLE_ADMIN", "ADMIN")
     * @return true if user has role
     */
    public static boolean hasRole(String role) {
        return getAuthentication()
            .map(Authentication::getAuthorities)
            .stream()
            .flatMap(authorities -> authorities.stream())
            .anyMatch(authority -> {
                String authorityName = authority.getAuthority();
                // Support both "ROLE_ADMIN" and "ADMIN" formats
                return authorityName.equals(role) || 
                       authorityName.equals("ROLE_" + role) ||
                       ("ROLE_" + authorityName).equals(role);
            });
    }

    /**
     * 🔒 Require authentication (throw if not authenticated)
     * 
     * @throws IllegalStateException if not authenticated
     */
    public static void requireAuthentication() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Authentication required");
        }
    }

    /**
     * 🔒 Require admin role (throw if not admin)
     * 
     * @throws IllegalStateException if not admin
     */
    public static void requireAdmin() {
        if (!hasRole("ADMIN")) {
            throw new IllegalStateException("Admin role required");
        }
    }
}
