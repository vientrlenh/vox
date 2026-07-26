package com.sep.vox.interfaces.rest.dto.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Gán tay nhiều bài trong một lần — admin tick nhiều dòng rồi gán một phát.
 *
 * <p>Một lần gán chỉ một {@code roundType}: trên UI admin cũng chọn vòng trước rồi
 * mới tick bài, và trộn vòng trong cùng lô làm luật kiểm tra trạng thái nhập nhằng.
 */
public record AssignGradingRequest(
    @NotBlank(message = "Phải chọn vòng chấm")
    String roundType,

    OffsetDateTime deadlineAt,

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
