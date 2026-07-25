package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExamAppealRequest(
    @NotNull(message = "Phải chọn kết quả bài thi cần phúc khảo")
    UUID candidateResultId,

    @NotEmpty(message = "Phải chọn ít nhất một phần thi cần phúc khảo")
    List<UUID> paperItemIds,

    @NotBlank(message = "Phải nêu lý do phúc khảo")
    @Size(max = 512, message = "Lý do phúc khảo tối đa 512 ký tự")
    String reason,

    @Size(max = 512, message = "Ghi chú tối đa 512 ký tự")
    String notes
) {
}
