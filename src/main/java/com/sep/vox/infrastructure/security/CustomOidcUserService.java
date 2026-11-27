package com.sep.vox.infrastructure.security;


import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser>{
    
    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository userRepository;

    @Override
    public @Nullable OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        if (oidcUser.getEmailVerified() == null || !oidcUser.getEmailVerified()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_verified", "The user has not been verified to log in", null)
            );
        }
        userRepository.findByEmailAndStatus(oidcUser.getEmail(), UserStatus.ACTIVE)
        .orElseThrow(() -> new OAuth2AuthenticationException(
            new OAuth2Error("user_not_found", "The user does not exist yet. Please submit a registration form or contact your school for support", null)
        ));

        return oidcUser;        
    }


}
