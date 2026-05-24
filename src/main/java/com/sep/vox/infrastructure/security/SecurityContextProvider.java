package com.sep.vox.infrastructure.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.UserContextPort;

@Component
public class SecurityContextProvider implements UserContextPort {

    @Override
    public UUID getCurrentAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Người dùng hiện không đăng nhập");
        }
        var principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException("Nguyên tắc xác thực không hợp lệ");
        }
        return userDetails.getId();
    }

    
}
