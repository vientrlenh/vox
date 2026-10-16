package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultStatusHistoryJpaEntity;

public interface SpringDataAssessmentResultStatusHistoryRepository
        extends JpaRepository<AssessmentResultStatusHistoryJpaEntity, UUID> {

    List<AssessmentResultStatusHistoryJpaEntity> findByCandidateResultIdOrderByCreatedAtAsc(UUID candidateResultId);

    List<AssessmentResultStatusHistoryJpaEntity> findByCandidateResultIdInOrderByCreatedAtAsc(
        Collection<UUID> candidateResultIds);

    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
