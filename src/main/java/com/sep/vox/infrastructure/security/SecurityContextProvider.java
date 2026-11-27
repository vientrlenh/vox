package com.sep.vox.infrastructure.security;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.UserContextPort;

@Component
public class SecurityContextProvider implements UserContextPort {

    @Override
    public UUID getCurrentAuthenticatedUserId() {
        Authentication authentication = getAuthentication();
        CustomUserDetails userDetails = getUserDetails(authentication);
        return userDetails.getId();
    }

    /**
     * The version of {@link #getCurrentAuthenticatedUserId()} that does NOT throw, for flows that must
     * keep working even when the caller is not / no longer logged in -- currently logout.
     *
     * <p>The access token lives 15 minutes, so an abandoned session almost always calls /logout with an
     * expired token: {@code JwtAuthenticationFilter} swallows the token error and lets the request continue under
     * an anonymous identity. Throwing there means the very session that most needs revoking is the one that
     * cannot be revoked.
     */
    @Override
    public Optional<UUID> findCurrentAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return Optional.empty();
        }

        return Optional.of(userDetails.getId());
    }

    @Override
    public UUID getCurrentSchoolId() {
        Authentication authentication = getAuthentication();
        CustomUserDetails userDetails = getUserDetails(authentication);
        return userDetails.getSchoolId();
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

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("The user is not currently signed in");
        }
        return authentication;
    }
    
    private CustomUserDetails getUserDetails(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException("Invalid authentication principal");
        }
        return userDetails;
    }

    private boolean isRoleMatch(CustomUserDetails userDetails, String role) {
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        return authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
