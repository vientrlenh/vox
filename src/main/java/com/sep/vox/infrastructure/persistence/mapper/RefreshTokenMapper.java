package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.infrastructure.persistence.entity.RefreshTokenJpaEntity;

public final class RefreshTokenMapper {
    
    public static RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        return RefreshToken.builder()
            .id(jpa.getId())
            .deviceSessionId(jpa.getDeviceSessionId())
            .tokenHash(jpa.getTokenHash())
            .createdAt(jpa.getCreatedAt())
            .expiresAt(jpa.getExpiresAt())
            .usedAt(jpa.getUsedAt())
            .replacedBy(jpa.getReplacedBy())
            .build();
    }


    public static RefreshTokenJpaEntity toJpa(RefreshToken token) {
        return RefreshTokenJpaEntity.builder()
            .id(token.getId())
            .deviceSessionId(token.getDeviceSessionId())
            .tokenHash(token.getTokenHash())
            .createdAt(token.getCreatedAt())
            .expiresAt(token.getExpiresAt())
            .usedAt(token.getUsedAt())
            .replacedBy(token.getReplacedBy())
            .build();
    }
}
