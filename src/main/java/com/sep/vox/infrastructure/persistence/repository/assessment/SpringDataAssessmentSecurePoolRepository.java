package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSecurePoolJpaEntity;

public interface SpringDataAssessmentSecurePoolRepository extends JpaRepository<AssessmentSecurePoolJpaEntity, UUID> {
    Optional<AssessmentSecurePoolJpaEntity> findByAssessmentId(UUID assessmentId);
    List<AssessmentSecurePoolJpaEntity> findByAssessmentIdIn(Collection<UUID> assessmentIds);
    void deleteByAssessmentId(UUID assessmentId);
}
