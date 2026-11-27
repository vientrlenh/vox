package com.sep.vox.interfaces.rest.dto.request.assessmentsession;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateRemainingTimeRequest(
    @NotNull(message = "Remaining time must not be blank")
    @Min(value = 0, message = "Remaining time must not be negative")
    Integer remainingSeconds
) {

}
