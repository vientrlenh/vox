package com.sep.vox.interfaces.rest.dto.request.assessmentsession;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateRemainingTimeRequest(
    @NotNull(message = "Thời gian còn lại không được để trống")
    @Min(value = 0, message = "Thời gian còn lại không được âm")
    Integer remainingSeconds
) {

}
