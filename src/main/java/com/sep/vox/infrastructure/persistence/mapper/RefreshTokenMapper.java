package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.infrastructure.persistence.entity.refreshtoken.RefreshTokenJpaEntity;

public final class RefreshTokenMapper {
    
    public static RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        return RefreshToken.builder()
            .id(jpa.getId())
            .sessionId(jpa.getSessionId())
            .tokenHash(jpa.getTokenHash())
            .issuedAt(jpa.getIssuedAt())
            .expiredAt(jpa.getExpiredAt())
            .usedAt(jpa.getUsedAt())
            .replacedBy(jpa.getReplacedBy())
            .build();
    }

    public static RefreshTokenJpaEntity toJpa(RefreshToken token) {
        return RefreshTokenJpaEntity.builder()
            .id(token.getId())
            .sessionId(token.getSessionId())
            .tokenHash(token.getTokenHash())
            .issuedAt(token.getIssuedAt())
            .expiredAt(token.getExpiredAt())
            .usedAt(token.getUsedAt())
            .replacedBy(token.getReplacedBy())
            .build();
    }
}
