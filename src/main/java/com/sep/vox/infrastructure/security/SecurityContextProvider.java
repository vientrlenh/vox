package com.sep.vox.infrastructure.security;

import java.util.UUID;

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
        var authorities = userDetails.getAuthorities();
        return authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
    }

    private Authentication getAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
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

}
