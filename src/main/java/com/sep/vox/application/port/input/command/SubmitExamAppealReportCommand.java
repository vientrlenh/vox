package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giám khảo nộp báo cáo cho TOÀN BỘ phần thi của đơn trong một lần — không có
 * báo cáo dở dang, nên "đã nộp" là đúng theo cấu trúc chứ không phải một phép đếm.
 */
public record SubmitExamAppealReportCommand(
    UUID appealId,
    List<ItemReport> items
) {
    public record ItemReport(
        UUID appealItemId,
        List<CriterionScoreItem> scores,
        String note
    ) {
    }

    public record CriterionScoreItem(
        UUID criterionId,
        BigDecimal score,
        String rationale
    ) {
    }
}
