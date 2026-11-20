package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectExamAppealRequest(
    @NotBlank(message = "Phải nêu lý do từ chối đơn phúc khảo")
    @Size(max = 512, message = "Lý do từ chối tối đa 512 ký tự")
    String reason
) {
}
