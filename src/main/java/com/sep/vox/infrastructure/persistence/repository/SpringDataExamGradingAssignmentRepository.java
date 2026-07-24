package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;

public interface SpringDataExamGradingAssignmentRepository
        extends JpaRepository<ExamGradingAssignmentJpaEntity, UUID> {
    Optional<ExamGradingAssignmentJpaEntity> findByCandidateResultId(UUID candidateResultId);
    boolean existsByCandidateResultId(UUID candidateResultId);
    List<ExamGradingAssignmentJpaEntity> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
