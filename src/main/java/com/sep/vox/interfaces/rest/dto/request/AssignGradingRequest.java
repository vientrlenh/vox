package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Gán tay nhiều bài trong một lần — admin tick nhiều dòng rồi gán một phát. */
public record AssignGradingRequest(
    @NotEmpty(message = "Phải chọn ít nhất một bài thi để phân công")
    @Valid
    List<AssignmentItemRequest> assignments
) {
    public record AssignmentItemRequest(
        @NotNull(message = "Thiếu bài thi cần phân công")
        UUID candidateResultId,

        @NotNull(message = "Thiếu giáo viên cần phân công")
        UUID teacherId
    ) {
    }
}
