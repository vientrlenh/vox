package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSchoolClassUserStatusRequest(
    @NotNull Boolean isActive
) {
}
