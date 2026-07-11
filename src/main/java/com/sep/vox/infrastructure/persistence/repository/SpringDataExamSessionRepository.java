package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;

public interface SpringDataExamSessionRepository extends JpaRepository<ExamSessionJpaEntity, UUID> {
    Optional<ExamSessionJpaEntity> findTopByExamIdAndCandidateIdOrderByStartedAtDesc(UUID examId, UUID candidateId);
    Optional<ExamSessionJpaEntity> findTopByCandidateIdOrderByStartedAtDesc(UUID candidateId);
    List<ExamSessionJpaEntity> findByCandidateId(UUID candidateId);
    List<ExamSessionJpaEntity> findByCandidateIdIn(Collection<UUID> candidateIds);
}
