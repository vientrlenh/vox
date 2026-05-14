package com.sep.vox.infrastructure.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;

import lombok.Builder;

@Builder
public class CustomUserDetails implements UserDetails {
    private UUID userId;
    private String password;
    private UserStatus status;
    private Collection<? extends GrantedAuthority> authorities;

    private static final String AUTHORITY_ROLE_PREFIX = "ROLE_";

    public static CustomUserDetails createFromUser(User user) {
        var authorities = Collections.singleton(new SimpleGrantedAuthority(AUTHORITY_ROLE_PREFIX + user.getRoleId().toString()));
        
        return CustomUserDetails.builder()
            .userId(user.getId().value())
            .password(user.getPasswordHash())
            .status(user.getStatus())
            .authorities(authorities)
            .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    @Override
    public @Nullable String getPassword() {
        return password;
    }
    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE || status == UserStatus.INACTIVE;
    }

}
