package com.sep.vox.application.response.input.schooluser;

import java.time.OffsetDateTime;
import java.util.UUID;


public record SchoolUserResponse(
    UUID id,
    UUID schoolId,
    UUID userId,
    String roleCode,
    String studentId,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {

}
