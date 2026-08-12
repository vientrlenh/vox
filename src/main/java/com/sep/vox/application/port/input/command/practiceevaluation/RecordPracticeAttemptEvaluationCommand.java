package com.sep.vox.application.port.input.command.practiceevaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

public record RecordPracticeAttemptEvaluationCommand(
    UUID practiceResponseId,
    boolean validForScoring,
    ConfidenceCaseSignals confidenceCase,
    BigDecimal audioQuality,
    BigDecimal codeSwitchingRatio,
    int wordCount,
    List<PracticeCriterionScoreInput> criteria,
    String evaluatedAt
    // Trước đây còn rawCriteria / turns / signals -- ba lát cắt thô chỉ phục vụ việc suy quan
    // sát điểm yếu. Hồ sơ điểm yếu đã gỡ, use case không đọc chúng, nên bỏ khỏi command.
    // Wire DTO của Kafka không đổi: Python vẫn phát đủ, Java bỏ qua.
) {
}
