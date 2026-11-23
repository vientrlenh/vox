package com.sep.vox.infrastructure.security;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
@RequiredArgsConstructor
public class AuthenticationManagerProvider implements AuthenticationManagerPort {

    private final AuthenticationManager authenticationManager;

    @Override
    public String setAuthenticationAndGetUserEmail(String login, String password) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, password));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) 
            return "";
        return userDetails.getUsername();
    }

    @Override
    public UUID setAuthenticationAndGetUserId(String login, String password) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, password));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            throw new InfrastructureException("An unexpected error when setting username password authentication token");
        }
        return userDetails.getId();
    }
    
}
