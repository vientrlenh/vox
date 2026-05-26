package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.infrastructure.persistence.entity.PasswordSetUpTokenJpaEntity;

public final class PasswordSetUpTokenMapper {
    
    public static PasswordSetUpToken toDomain(PasswordSetUpTokenJpaEntity jpa) {
        return new PasswordSetUpToken(
            jpa.getId(),
            jpa.getUserId(),
            jpa.getTokenHash(),
            jpa.getCreatedAt(),
            jpa.getExpiredAt(),
            jpa.getUsedAt()
        );
    }

    public static PasswordSetUpTokenJpaEntity toJpa(PasswordSetUpToken passwordSetUpToken) {
        return new PasswordSetUpTokenJpaEntity(
            passwordSetUpToken.getId(),
            passwordSetUpToken.getUserId(),
            passwordSetUpToken.getTokenHash(),
            passwordSetUpToken.getCreatedAt(),
            passwordSetUpToken.getExpiredAt(),
            passwordSetUpToken.getUsedAt()
        );
    }
}
