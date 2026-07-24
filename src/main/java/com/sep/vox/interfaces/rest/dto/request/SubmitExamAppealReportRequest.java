package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Giám khảo nộp báo cáo cho TOÀN BỘ phần thi của đơn trong một lần — không có
 * báo cáo dở dang.
 */
public record SubmitExamAppealReportRequest(
    @NotEmpty(message = "Phải chấm điểm cho tất cả phần thi được phúc khảo")
    @Valid
    List<ItemReportRequest> items
) {
    public record ItemReportRequest(
        @NotNull(message = "Thiếu phần thi cần chấm")
        UUID appealItemId,

        @NotEmpty(message = "Phải chấm điểm cho các tiêu chí")
        @Valid
        List<CriterionScoreRequest> scores,

        @Size(max = 2048, message = "Nhận xét tối đa 2048 ký tự")
        String note
    ) {
    }

    public record CriterionScoreRequest(
        @NotNull(message = "Thiếu tiêu chí chấm điểm")
        UUID criterionId,

        @NotNull(message = "Phải chấm điểm cho tiêu chí")
        BigDecimal score,

        @Size(max = 512, message = "Diễn giải tối đa 512 ký tự")
        String rationale
    ) {
    }
}
