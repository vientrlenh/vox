package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSchedule;

public interface ExamScheduleRepository {
    Optional<ExamSchedule> findById(UUID id);
    List<ExamSchedule> findByExamId(UUID examId);
    List<ExamSchedule> findByExamIdAndInSchedule(UUID examId, OffsetDateTime now);
    List<ExamSchedule> findByIdInAndInSchedule(Collection<UUID> ids, OffsetDateTime now);
}
