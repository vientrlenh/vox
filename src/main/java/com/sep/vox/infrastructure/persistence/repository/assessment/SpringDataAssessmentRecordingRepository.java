package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentRecordingJpaEntity;

public interface SpringDataAssessmentRecordingRepository extends JpaRepository<AssessmentRecordingJpaEntity, UUID> {
    List<AssessmentRecordingJpaEntity> findByAssessmentSessionId(UUID assessmentSessionId);
    Optional<AssessmentRecordingJpaEntity> findByAssessmentSessionIdAndStreamTypeAndSource(
        UUID assessmentSessionId, String streamType, String source);
    void deleteByAssessmentSessionId(UUID assessmentSessionId);
}
