package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamStatus;

public interface ExamRepository {
    Optional<Exam> findById(UUID id);
    List<Exam> findByIdIn(Collection<UUID> ids);
    Exam save(Exam exam);
    PageResult<Exam> findAccessible(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID schoolId,
        UUID schoolClassId,
        ExamKind kind,
        ExamStatus status,
        String keyword,
        int page,
        int size
    );
    Optional<Exam> findByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintId(UUID blueprintId);
    boolean existsSubmittedSessionByExamId(UUID examId);
    void deleteById(UUID id);
}
