package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSchoolClassUserStatusRequest(
    @NotNull(message = "trạng thái là bắt buộc") Boolean isActive
) {
}
