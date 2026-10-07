package com.sep.vox.infrastructure.persistence.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.projection.repository.RefreshTokenProjectionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository 
public class JpaRefreshTokenProjectionRepository implements RefreshTokenProjectionRepository {

    @PersistenceContext 
    private EntityManager em;

    @Override
    public Optional<UUID> findDeviceSessionIdByTokenHash(String tokenHash) {
        UUID deviceSessionId = em.createQuery("""
            SELECT r.deviceSessionId FROM RefreshTokenJpaEntity r 
            WHERE r.tokenHash = :tokenHash 
        """, UUID.class)
            .setParameter("tokenHash", tokenHash)
            .getSingleResult();
        if (deviceSessionId == null) {
            return Optional.empty();
        }
        return Optional.of(deviceSessionId);
    }
    
}
