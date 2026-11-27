package com.sep.vox.interfaces.rest.dto.request.subscription;

import jakarta.validation.constraints.Size;

public record UnsuspendSubscriptionRequest(
    @Size(max = 512, message = "Note must not exceed 512 characters")
    String note
) {
}
