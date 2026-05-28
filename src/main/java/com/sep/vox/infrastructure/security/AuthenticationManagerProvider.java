package com.sep.vox.infrastructure.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
public class AuthenticationManagerProvider implements AuthenticationManagerPort {

    private final AuthenticationManager authenticationManager;

    public AuthenticationManagerProvider(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public String setAuthenticationAndGetUserEmail(String login, String password) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, password));
        var userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) 
            return "";
        return userDetails.getUsername();
    }

    @Override
    public UUID setAuthenticationAndGetUserId(String login, String password) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, password));
        var userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            throw new InfrastructureException("An unexpected error when setting username password authentication token");
        }
        return userDetails.getId();
    }
    
}
