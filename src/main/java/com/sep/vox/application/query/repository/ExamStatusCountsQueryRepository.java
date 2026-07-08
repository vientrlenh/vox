package com.sep.vox.application.query.repository;

import java.util.UUID;

import com.sep.vox.application.query.dto.ExamStatusCountsDto;

public interface ExamStatusCountsQueryRepository {
    ExamStatusCountsDto countAccessibleByStatus(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID schoolId,
        String kind
    );
}
