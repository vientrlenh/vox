package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.infrastructure.persistence.entity.RefreshTokenJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class RefreshTokenMapper {

    private RefreshTokenMapper() {}
    
    public static RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        try {
            return RefreshToken.builder()
                .id(jpa.getId())
                .deviceSessionId(jpa.getDeviceSessionId())
                .tokenHash(jpa.getTokenHash())
                .createdAt(jpa.getCreatedAt())
                .expiresAt(jpa.getExpiresAt())
                .usedAt(jpa.getUsedAt())
                .replacedBy(jpa.getReplacedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping refresh token entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }


    public static RefreshTokenJpaEntity toJpa(RefreshToken token) {
        try {
            return RefreshTokenJpaEntity.builder()
                .id(token.getId())
                .deviceSessionId(token.getDeviceSessionId())
                .tokenHash(token.getTokenHash())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .usedAt(token.getUsedAt())
                .replacedBy(token.getReplacedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping refresh token model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }

    }
}
