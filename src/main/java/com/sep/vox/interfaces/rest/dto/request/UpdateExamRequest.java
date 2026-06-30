package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

public record UpdateExamRequest(
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId
) {
}
