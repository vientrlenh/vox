package com.sep.vox.infrastructure.security;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.UserContextPort;

@Component
public class SecurityContextProvider implements UserContextPort {

    @Override
    public Optional<UUID> getCurrentAuthenticatedUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        CustomUserDetails userDetails = getUserDetails(authentication);
        if (userDetails == null) {
            return Optional.empty();
        }
        return Optional.of(userDetails.getId());
    }

    @Override
    public Optional<UUID> getCurrentSchoolId() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        CustomUserDetails userDetails = getUserDetails(authentication);
        if (userDetails == null || userDetails.getSchoolId() == null) {
            return Optional.empty();
        }
        return Optional.of(userDetails.getSchoolId());
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    @Override
    public boolean isSystemAdmin() {
        Authentication authentication = getAuthentication();
        CustomUserDetails userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_SYSTEM_ADMIN");
    }

    
    @Override
    public boolean isSchoolAdmin() {
        Authentication authentication = getAuthentication();
        CustomUserDetails userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_SCHOOL_ADMIN");
    }

    @Override
    public boolean isTeacher() {
        Authentication authentication = getAuthentication();
        CustomUserDetails userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_TEACHER");
    }
    
    private CustomUserDetails getUserDetails(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return null;
        }
        return userDetails;
    }

    private boolean isRoleMatch(CustomUserDetails userDetails, String role) {
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        return authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
