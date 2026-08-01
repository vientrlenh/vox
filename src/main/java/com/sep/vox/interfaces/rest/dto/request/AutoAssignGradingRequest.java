package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Phải có {@code examId} hoặc {@code scheduleId} — ràng buộc "ít nhất một" nằm ở
 * use case vì Bean Validation không diễn đạt được nó gọn hơn. Ràng buộc
 * "{@code percent} bắt buộc khi chế độ là RANDOM_PERCENT" cũng vậy.
 *
 * @param selectionMode {@code ALL} | {@code RANDOM_PERCENT} | {@code RISK_BASED} |
 *                      {@code MANUAL_LIST}; bỏ trống = {@code ALL}
 */
public record AutoAssignGradingRequest(
    UUID examId,

    UUID scheduleId,

    @NotBlank(message = "Phải chọn vòng chấm")
    String roundType,

    String selectionMode,

    @Min(value = 1, message = "Tỉ lệ chọn mẫu phải từ 1%")
    @Max(value = 100, message = "Tỉ lệ chọn mẫu tối đa 100%")
    Integer percent,

    List<UUID> candidateResultIds,

    Instant deadlineAt,

    @NotEmpty(message = "Phải chọn ít nhất một giáo viên để phân công")
    List<UUID> teacherIds
) {
}
