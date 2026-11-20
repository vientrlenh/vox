package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamResultStatusHistoryJpaEntity;

public interface SpringDataExamResultStatusHistoryRepository
        extends JpaRepository<ExamResultStatusHistoryJpaEntity, UUID> {

    List<ExamResultStatusHistoryJpaEntity> findByCandidateResultIdOrderByCreatedAtAsc(UUID candidateResultId);

    List<ExamResultStatusHistoryJpaEntity> findByCandidateResultIdInOrderByCreatedAtAsc(
        Collection<UUID> candidateResultIds);

    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
