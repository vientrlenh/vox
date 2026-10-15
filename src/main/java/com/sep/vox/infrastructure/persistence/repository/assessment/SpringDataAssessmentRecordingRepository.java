package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentRecordingJpaEntity;

public interface SpringDataAssessmentRecordingRepository extends JpaRepository<AssessmentRecordingJpaEntity, UUID> {
    List<AssessmentRecordingJpaEntity> findByExamSessionId(UUID examSessionId);
    Optional<AssessmentRecordingJpaEntity> findByExamSessionIdAndStreamTypeAndSource(
        UUID examSessionId, String streamType, String source);
    void deleteByExamSessionId(UUID examSessionId);
}
