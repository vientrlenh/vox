package com.sep.vox.domain.repository.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.shared.PageResult;

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
    boolean existsSubmittedSessionByExamId(UUID examId);
    /** Bài thi đang có bài chờ soát điểm AI mà chưa nhắc lần nào. */
    List<Assessment> findDueForHumanGradingNotice();

    List<Assessment> findByStatusAndOpenAtBefore(AssessmentStatus status, Instant time);
    List<Assessment> findByStatusAndCloseAtBefore(AssessmentStatus status, Instant time);
    void deleteById(UUID id);
}
