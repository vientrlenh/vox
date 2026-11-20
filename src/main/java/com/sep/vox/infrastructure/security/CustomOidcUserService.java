package com.sep.vox.infrastructure.security;


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
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser>{
    
    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @Nullable OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        var oidcUser = delegate.loadUser(userRequest);

        if (oidcUser.getEmailVerified() == null || !oidcUser.getEmailVerified()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_verified", "Người dùng chưa được xác thực để đăng nhập", null)
            );
        }
        userRepository.findByEmailAndStatus(oidcUser.getEmail(), UserStatus.ACTIVE)
        .orElseThrow(() -> new OAuth2AuthenticationException(
            new OAuth2Error("user_not_found", "Người dùng hiện chưa tồn tại, vui lòng gửi đơn đăng ký hoặc liên hệ bên nhà trường để được hỗ trợ", null)
        ));

        return oidcUser;        
    }


}
