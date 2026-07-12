package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ScoringRuleJpaEntity;

public interface SpringDataScoringRuleRepository extends JpaRepository<ScoringRuleJpaEntity, UUID> {

    boolean existsByPolicyIdAndCode(UUID policyId, String code);

    @Query("""
            SELECT r FROM ScoringRuleJpaEntity r
            WHERE r.policyId = :policyId
                AND (:keywordPattern IS NULL OR LOWER(r.name) LIKE :keywordPattern OR LOWER(r.code) LIKE :keywordPattern)
                AND (:isActive IS NULL OR r.isActive = :isActive)
            ORDER BY r.priority ASC
            """)
    Page<ScoringRuleJpaEntity> searchByPolicyId(
            @Param("policyId") UUID policyId,
            @Param("keywordPattern") String keywordPattern,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
