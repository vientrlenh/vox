package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateQuestionCollaboratorRequest(
    @NotNull(message = "ID người cộng tác không được để trống")
    UUID userId,

    @NotNull(message = "Quyền cộng tác không được để trống")
    @Pattern(
        regexp = "READ_ONLY|CAN_USE|CAN_EDIT",
        message = "Quyền cộng tác không hợp lệ"
    )
    String permission
) {
}
