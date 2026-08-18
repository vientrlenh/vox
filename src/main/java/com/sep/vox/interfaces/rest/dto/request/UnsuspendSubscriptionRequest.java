package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Size;

public record UnsuspendSubscriptionRequest(
    @Size(max = 512, message = "Ghi chú tối đa 512 ký tự")
    String note
) {
}
