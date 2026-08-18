package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendSubscriptionRequest(
    @NotBlank(message = "Phải nêu lý do đình chỉ gói đăng ký")
    @Size(max = 512, message = "Lý do đình chỉ tối đa 512 ký tự")
    String reason
) {
}
