package com.sep.vox.domain.repository;

import java.util.UUID;

public interface ExamScheduleProctorRepository {
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
}
