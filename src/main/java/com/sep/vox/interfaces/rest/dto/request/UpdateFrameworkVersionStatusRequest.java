package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateFrameworkVersionStatusRequest(
    @NotNull(message = "Trạng thái không được để trống")
    FrameworkVersionStatus status
) {
}
