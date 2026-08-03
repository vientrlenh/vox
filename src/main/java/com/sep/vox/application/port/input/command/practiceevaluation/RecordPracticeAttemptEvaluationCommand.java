package com.sep.vox.application.port.input.command.practiceevaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.examevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.examevaluation.EvaluationSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.TurnDetailInput;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

public record RecordPracticeAttemptEvaluationCommand(
    UUID practiceResponseId,
    boolean validForScoring,
    ConfidenceCaseSignals confidenceCase,
    BigDecimal audioQuality,
    BigDecimal codeSwitchingRatio,
    int wordCount,
    List<PracticeCriterionScoreInput> criteria,
    String evaluatedAt,
    /**
     * Ba trường dưới đây phục vụ riêng việc suy quan sát điểm yếu
     * ({@code WeaknessObservationDerivationService}), không dùng cho việc chấm điểm.
     *
     * Tách khỏi {@code criteria} ở trên vì hai bên cần hai lát cắt khác nhau của cùng dữ liệu:
     * chấm điểm chỉ cần mã tiêu chí + điểm + band, còn suy điểm yếu cần nhãn/bằng chứng thô
     * ({@code weaknessLabels}, {@code evidenceSpans}), dữ liệu âm vị trong {@code turns}, và
     * nhịp nói / tỉ lệ im lặng trong {@code signals}.
     */
    Map<String, CriterionScoreInput> rawCriteria,
    List<TurnDetailInput> turns,
    EvaluationSignalsInput signals
) {
}
