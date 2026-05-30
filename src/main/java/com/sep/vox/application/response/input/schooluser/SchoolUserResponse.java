package com.sep.vox.application.response.input.schooluser;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolUserResponse(
    UUID id,
    String email,
    String phone,
    String fullName,
    String roleCode,
    String status,
    UUID schoolId,
    String studentId,
    OffsetDateTime createdAt
) {

}
