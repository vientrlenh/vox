package com.sep.vox.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;

import lombok.Builder;

@Builder
public class CustomUserDetails implements UserDetails {
    private UUID id;
    private String email;
    private String password;
    private UserStatus status;
    private Collection<? extends GrantedAuthority> authorities;

    private static final String AUTHORITY_ROLE_PREFIX = "ROLE_";

    public static CustomUserDetails createFromUser(User user, List<String> roleCodes) {
        var authorities = roleCodes.stream()
            .map(role -> new SimpleGrantedAuthority(AUTHORITY_ROLE_PREFIX + roleCodes))
            .collect(Collectors.toList());
        
        return CustomUserDetails.builder()
            .id(user.getId())
            .email(user.getEmail().value())
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
        return email;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE || status == UserStatus.INACTIVE;
    }

}
