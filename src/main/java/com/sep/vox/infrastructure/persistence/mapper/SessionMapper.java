package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.session.Session;
import com.sep.vox.infrastructure.persistence.entity.SessionJpaEntity;

public class SessionMapper {
    
    public static Session toDomain(SessionJpaEntity jpa) {
        return new Session(
            jpa.getId(),
            jpa.getUserId(),
            jpa.getRefreshTokenHash(),
            jpa.getIssuedAt(),
            jpa.getExpiredAt(),
            jpa.getRevokedAt(),
            jpa.getReplacedBy()
        );
    }

    public static SessionJpaEntity toJpa(Session session) {
        return new SessionJpaEntity(
            session.getId(),
            session.getUserId(),
            session.getRefreshTokenHash(),
            session.getIssuedAt(),
            session.getExpiredAt(),
            session.getRevokedAt(),
            session.getReplacedBy()
        );
    }
}
