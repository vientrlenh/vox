package com.sep.vox.infrastructure.security;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.UserContextPort;

@Component
public class SecurityContextProvider implements UserContextPort {

    @Override
    public UUID getCurrentAuthenticatedUserId() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return userDetails.getId();
    }

    @Override
    public UUID getCurrentSchoolId() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return userDetails.getSchoolId();
    }

    @Override
    public boolean isSystemAdmin() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_SYSTEM_ADMIN");
    }

    
    @Override
    public boolean isSchoolAdmin() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_SCHOOL_ADMIN");
    }

    @Override
    public boolean isTeacher() {
         var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_TEACHER");
    }

    private Authentication getAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Người dùng hiện không đăng nhập");
        }
        return authentication;
    }
    
    private CustomUserDetails getUserDetails(Authentication authentication) {
        var principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException("Nguyên tắc xác thực không hợp lệ");
        }
        return userDetails;
    }

    private boolean isRoleMatch(CustomUserDetails userDetails, String role) {
        var authorities = userDetails.getAuthorities();
        return authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
