package com.sep.vox.interfaces.rest.dto.request.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendSubscriptionRequest(
    @NotBlank(message = "The reason for suspending the subscription must be stated")
    @Size(max = 512, message = "Suspension reason must not exceed 512 characters")
    String reason
) {
}
