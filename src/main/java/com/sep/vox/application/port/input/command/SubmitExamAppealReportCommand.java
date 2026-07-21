package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SubmitExamAppealReportCommand(
    UUID appealId,
    List<CriterionScoreItem> scores,
    String note
) {
    public record CriterionScoreItem(
        UUID criterionId,
        BigDecimal score,
        String rationale
    ) {
    }
}
