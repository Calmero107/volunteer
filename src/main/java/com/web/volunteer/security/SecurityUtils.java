package com.web.volunteer.security;

import com.web.volunteer.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Get current authenticated user ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getId();
        }
        throw new UnauthorizedException("User not authenticated");
    }

    /**
     * Get current authenticated user details
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new UnauthorizedException("User not authenticated");
    }

    /**
     * Check if current user has specific role
     */
    public static boolean hasRole(String role) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getRole().equals(role);
    }

    /**
     * Check if current user is admin
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if current user is event manager or admin
     */
    public static boolean isEventManagerOrAdmin() {
        return hasRole("EVENT_MANAGER") || hasRole("ADMIN");
    }
}
