package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.TurnCorrectionJpaEntity;

public interface SpringDataTurnCorrectionRepository
        extends JpaRepository<TurnCorrectionJpaEntity, UUID> {

    List<TurnCorrectionJpaEntity> findByTurnIdOrderById(UUID turnId);
}
