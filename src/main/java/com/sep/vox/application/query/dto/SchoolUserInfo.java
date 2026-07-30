package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

public record SchoolUserInfo(
    UUID id,
    String email,
    String phone,
    String fullName,
    String roleCode,
    String status,
    UUID schoolId,
    Instant createdAt,
    UUID userId,
    Instant startDate,
    Instant endDate
) {

}
