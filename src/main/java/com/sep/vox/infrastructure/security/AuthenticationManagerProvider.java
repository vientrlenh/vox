package com.sep.vox.infrastructure.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.response.output.AuthenticatedInfo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
public class AuthenticationManagerProvider implements AuthenticationManagerPort {

    private final AuthenticationManager authenticationManager;

    public AuthenticationManagerProvider(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthenticatedInfo setAuthenticationAndGetInfo(String login, String password) {
        Authentication authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login, password));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            log.error("Authentication manager set authentication failed");
            throw new UnauthorizedException("Authentication set failed");
        }
        return new AuthenticatedInfo(userDetails.getId(), userDetails.getUsername());
    }
    
}
