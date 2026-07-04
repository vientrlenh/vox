package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamStatus;

public interface ExamRepository {
    Optional<Exam> findById(UUID id);
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
    List<Exam> findAllByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintIdAndKindAndStatusNot(UUID blueprintId, ExamKind kind, ExamStatus status);
    boolean existsSubmittedSessionByExamId(UUID examId);
    void deleteById(UUID id);
}
