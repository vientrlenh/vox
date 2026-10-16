package com.sep.vox.application.projection.repository;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.projection.dto.AssessmentStatusCountsDto;

public interface AssessmentStatusCountsQueryRepository {
    AssessmentStatusCountsDto countAccessibleByStatus(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID schoolId,
        String kind,
        Instant createdFrom,
        Instant createdTo
    );
}
