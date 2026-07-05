package com.sep.vox.domain.repository;

import java.util.UUID;

public interface ExamCandidateRepository {
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
}
