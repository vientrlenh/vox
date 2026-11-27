package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One exam part under appeal, with the data needed to compare.
 * `finalScore` is null until the appeal is published.
 */
public record AppealItemInfo(
    UUID appealItemId,
    UUID paperItemId,
    String partLabel,
    /**
     * The criterion score of the grade currently in effect: in the first round it is the AI grade,
     * in later rounds it is the manual grade of the previous round. It is not always the AI score.
     */
    List<AppealCriterionScoreInfo> baselineScores,
    /** Always taken from the AI grade -- only the AI grade has the speaking turn. */
    List<AppealTurnInfo> turns,
    BigDecimal finalScore
) {
}
