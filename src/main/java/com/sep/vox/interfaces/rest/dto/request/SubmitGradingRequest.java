package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body dùng chung cho {@code /grade} và {@code /grade/preview} — hai đường phải
 * nhận đúng một hình dạng đầu vào thì tổng chúng trả về mới bằng nhau.
 *
 * <p>Không có trường điểm tổng: tổng được BE tính lại từ điểm tiêu chí.
 * Dải min/max của mỗi tiêu chí lấy từ {@code gradingTaskDetail}, BE validate lại.
 */
public record SubmitGradingRequest(
    @NotEmpty(message = "Phải chấm điểm cho ít nhất một phần thi")
    @Valid
    List<ItemGradeRequest> items
) {
    public record ItemGradeRequest(
        @NotNull(message = "Thiếu phần thi cần chấm")
        UUID paperItemId,

        @NotEmpty(message = "Phải chấm điểm cho các tiêu chí")
        @Valid
        List<CriterionScoreRequest> criterionScores,

        @Size(max = 2048, message = "Nhận xét tối đa 2048 ký tự")
        String feedbackSummary
    ) {
    }

    public record CriterionScoreRequest(
        @NotNull(message = "Thiếu tiêu chí chấm điểm")
        UUID rubricCriterionId,

        @NotNull(message = "Phải chấm điểm cho tiêu chí")
        BigDecimal score,

        @Size(max = 512, message = "Diễn giải tối đa 512 ký tự")
        String rationale
    ) {
    }
}
