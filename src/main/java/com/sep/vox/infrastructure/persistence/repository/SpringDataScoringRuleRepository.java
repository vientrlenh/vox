package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ScoringRuleJpaEntity;

public interface SpringDataScoringRuleRepository extends JpaRepository<ScoringRuleJpaEntity, UUID> {
}
