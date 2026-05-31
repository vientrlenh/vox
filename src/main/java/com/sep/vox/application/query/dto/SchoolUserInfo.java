package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolUserInfo(
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
