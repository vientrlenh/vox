package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;

/**
 * Either {@code assessmentId} or {@code scheduleId} must be present -- the "at least one" constraint lives in the
 * use case because Bean Validation cannot express it more concisely. The same goes for the constraint
 * "{@code percent} is required when the mode is RANDOM_PERCENT".
 *
 * @param selectionMode {@code ALL} | {@code RANDOM_PERCENT} | {@code RISK_BASED} |
 *                      {@code MANUAL_LIST}; empty = {@code ALL}
 */
public record AutoAssignGradingRequest(
    UUID assessmentId,

    UUID scheduleId,

    @NotNull(message = "Phải chọn vòng chấm")
    GradingRoundType roundType,

    GradingSampleSelectionMode selectionMode,

    @Min(value = 1, message = "Tỉ lệ chọn mẫu phải từ 1%")
    @Max(value = 100, message = "Tỉ lệ chọn mẫu tối đa 100%")
    Integer percent,

    List<UUID> candidateResultIds,

    Instant deadlineAt,

    @NotEmpty(message = "Phải chọn ít nhất một giáo viên để phân công")
    List<UUID> teacherIds
) {
}
