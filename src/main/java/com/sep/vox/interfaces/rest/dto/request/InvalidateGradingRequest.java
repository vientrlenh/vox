package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Size;

public record InvalidateGradingRequest(
    @Size(max = 1024, message = "Lý do tối đa 1024 ký tự")
    String reason
) {
}
