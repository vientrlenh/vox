package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.infrastructure.persistence.entity.RefreshTokenJpaEntity;

public final class RefreshTokenMapper {
    
    public static RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        return new RefreshToken(
            jpa.getId(), 
            jpa.getSessionId(), 
            jpa.getTokenHash(), 
            jpa.getIssuedAt(), 
            jpa.getExpiredAt(), 
            jpa.getUsedAt(), 
            jpa.getReplacedBy()
        );
    }

    public static RefreshTokenJpaEntity toJpa(RefreshToken token) {
        return new RefreshTokenJpaEntity(
            token.getId(), 
            token.getSessionId(), 
            token.getTokenHash(), 
            token.getIssuedAt(), 
            token.getExpiredAt(), 
            token.getUsedAt(),
            token.getReplacedBy()
        );
    }
}
