package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.infrastructure.persistence.entity.passwordsetuptoken.PasswordSetUpTokenJpaEntity;

public final class PasswordSetUpTokenMapper {
    
    public static PasswordSetUpToken toDomain(PasswordSetUpTokenJpaEntity jpa) {
        return PasswordSetUpToken.builder()
            .id(jpa.getId())
            .userId(jpa.getUserId())
            .tokenHash(jpa.getTokenHash())
            .createdAt(jpa.getCreatedAt())
            .expiredAt(jpa.getExpiredAt())
            .usedAt(jpa.getUsedAt())
            .build();
    }

    public static PasswordSetUpTokenJpaEntity toJpa(PasswordSetUpToken passwordSetUpToken) {
        return PasswordSetUpTokenJpaEntity.builder()
            .id(passwordSetUpToken.getId())
            .userId(passwordSetUpToken.getUserId())
            .tokenHash(passwordSetUpToken.getTokenHash())
            .createdAt(passwordSetUpToken.getCreatedAt())
            .expiredAt(passwordSetUpToken.getExpiredAt())
            .usedAt(passwordSetUpToken.getUsedAt())
            .build();
    }
}
