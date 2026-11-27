package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentStatus;

public interface AssessmentRepository {
    Optional<Assessment> findById(UUID id);
    List<Assessment> findByIdIn(Collection<UUID> ids);
    Assessment save(Assessment assessment);
    PageResult<Assessment> findAccessible(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID schoolId,
        UUID schoolClassId,
        AssessmentKind kind,
        AssessmentStatus status,
        String keyword,
        int page,
        int size
    );
    List<Assessment> findAllByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintIdAndKindAndStatusNot(UUID blueprintId, AssessmentKind kind, AssessmentStatus status);
    boolean existsSubmittedSessionByAssessmentId(UUID assessmentId);
    /** Assessments that have results awaiting AI score review and have never been reminded. */
    List<Assessment> findDueForHumanGradingNotice();

    List<Assessment> findByStatusAndOpenAtBefore(AssessmentStatus status, Instant time);
    List<Assessment> findByStatusAndCloseAtBefore(AssessmentStatus status, Instant time);
    void deleteById(UUID id);
}
