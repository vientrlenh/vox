package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExamAppealRequest(
    @NotNull(message = "Phải chọn kết quả bài thi cần phúc khảo")
    UUID candidateResultId,

    /**
     * Bỏ trống = phúc khảo TOÀN BÀI, use case tự điền mọi phần đã có câu trả lời.
     * Vẫn nhận danh sách cụ thể để client cũ không gãy — nhưng người chấm phúc khảo
     * luôn phải chấm đủ mọi phần, nên danh sách này không thu hẹp phạm vi chấm.
     */
    List<UUID> paperItemIds,

    @NotBlank(message = "Phải nêu lý do phúc khảo")
    @Size(max = 512, message = "Lý do phúc khảo tối đa 512 ký tự")
    String reason,

    @Size(max = 512, message = "Ghi chú tối đa 512 ký tự")
    String notes
) {
}
