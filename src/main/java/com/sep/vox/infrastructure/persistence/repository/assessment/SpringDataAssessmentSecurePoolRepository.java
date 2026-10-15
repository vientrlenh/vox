package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSecurePoolJpaEntity;

public interface SpringDataAssessmentSecurePoolRepository extends JpaRepository<AssessmentSecurePoolJpaEntity, UUID> {
    Optional<AssessmentSecurePoolJpaEntity> findByExamId(UUID examId);
    List<AssessmentSecurePoolJpaEntity> findByExamIdIn(Collection<UUID> examIds);
    void deleteByExamId(UUID examId);
}
