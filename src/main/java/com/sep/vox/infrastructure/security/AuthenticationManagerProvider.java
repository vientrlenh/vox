package com.sep.vox.infrastructure.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthenticationManagerPort;

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
    
}
