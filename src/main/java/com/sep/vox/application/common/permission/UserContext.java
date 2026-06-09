package com.sep.vox.application.common.permission;

import java.util.List;
import java.util.UUID;

public record UserContext(
    UUID userId,
    UserRole role,
    UUID schoolId,
    List<String> roleCodes
) {
}
