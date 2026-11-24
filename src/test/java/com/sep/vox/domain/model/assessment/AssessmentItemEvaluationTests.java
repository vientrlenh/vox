package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentItemEvaluationTests {

    private static final Instant EVALUATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void zeroScoredWithoutRecordingFinalizesAZeroAiScoreWithoutReview() {
        UUID responseId = UUID.randomUUID();
        UUID paperItemId = UUID.randomUUID();

        AssessmentItemEvaluation evaluation = AssessmentItemEvaluation.zeroScoredWithoutRecording(
                responseId, paperItemId, "system", "NO_RECORDING", "No recording", EVALUATED_AT);

        assertThat(evaluation.getResponseId()).isEqualTo(responseId);
        assertThat(evaluation.getPaperItemId()).isEqualTo(paperItemId);
        assertThat(evaluation.getEngineType()).isEqualTo(AssessmentEvaluationEngineType.AI_SINGLE);
        assertThat(evaluation.getGradedByModel()).isEqualTo("system");
        assertThat(evaluation.getReviewReasonCode()).isEqualTo("NO_RECORDING");
        assertThat(evaluation.getFeedbackSummary()).isEqualTo("No recording");
        assertThat(evaluation.getRawItemScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(evaluation.getItemScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(evaluation.isRequiresHumanReview()).isFalse();
        assertThat(evaluation.isMarkedInvalid()).isFalse();
        assertThat(evaluation.isRequiresRetake()).isFalse();
        assertThat(evaluation.getSampleCount()).isNull();
        assertThat(evaluation.getReviewerId()).isNull();
        assertThat(evaluation.getStatus()).isEqualTo(AssessmentItemEvaluationStatus.FINALIZED);
        assertThat(evaluation.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(evaluation.getId()).isNull();
    }

    @Test
    void autoGradedUsesOneSampleTheSameRawAndFinalScoreAndAutoGradedStatus() {
        UUID responseId = UUID.randomUUID();
        UUID paperItemId = UUID.randomUUID();

        AssessmentItemEvaluation evaluation = AssessmentItemEvaluation.autoGraded(
                responseId, paperItemId, "gpt-4o", new BigDecimal("7.5"), new BigDecimal("0.9"),
                true, "LOW_CONFIDENCE", false, true, null, "{\"valid\":true}",
                "Good", "[]", "v1", EVALUATED_AT);

        assertThat(evaluation.getResponseId()).isEqualTo(responseId);
        assertThat(evaluation.getPaperItemId()).isEqualTo(paperItemId);
        assertThat(evaluation.getEngineType()).isEqualTo(AssessmentEvaluationEngineType.AI_SINGLE);
        assertThat(evaluation.getGradedByModel()).isEqualTo("gpt-4o");
        assertThat(evaluation.getSampleCount()).isEqualTo(1);
        assertThat(evaluation.getReviewerId()).isNull();
        assertThat(evaluation.getRawItemScore()).isEqualByComparingTo("7.5");
        assertThat(evaluation.getItemScore()).isEqualByComparingTo("7.5");
        assertThat(evaluation.getOverallConfidence()).isEqualByComparingTo("0.9");
        assertThat(evaluation.isRequiresHumanReview()).isTrue();
        assertThat(evaluation.getReviewReasonCode()).isEqualTo("LOW_CONFIDENCE");
        assertThat(evaluation.isMarkedInvalid()).isFalse();
        assertThat(evaluation.isRequiresRetake()).isTrue();
        assertThat(evaluation.getValidityJson()).isEqualTo("{\"valid\":true}");
        assertThat(evaluation.getFeedbackSummary()).isEqualTo("Good");
        assertThat(evaluation.getSuggestionsJson()).isEqualTo("[]");
        assertThat(evaluation.getPromptVersion()).isEqualTo("v1");
        assertThat(evaluation.getStatus()).isEqualTo(AssessmentItemEvaluationStatus.AUTO_GRADED);
        assertThat(evaluation.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
    }

    @Test
    void humanGradedIsFinalizedWithTheReviewerAndNeverWaitsForAnotherGrader() {
        UUID responseId = UUID.randomUUID();
        UUID paperItemId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        AssessmentItemEvaluation evaluation = AssessmentItemEvaluation.humanGraded(
                responseId, paperItemId, "HUMAN", reviewerId, new BigDecimal("8"), "Well done", EVALUATED_AT);

        assertThat(evaluation.getResponseId()).isEqualTo(responseId);
        assertThat(evaluation.getPaperItemId()).isEqualTo(paperItemId);
        assertThat(evaluation.getEngineType()).isEqualTo(AssessmentEvaluationEngineType.HUMAN);
        assertThat(evaluation.getGradedByModel()).isEqualTo("HUMAN");
        assertThat(evaluation.getReviewerId()).isEqualTo(reviewerId);
        assertThat(evaluation.getRawItemScore()).isEqualByComparingTo("8");
        assertThat(evaluation.getItemScore()).isEqualByComparingTo("8");
        assertThat(evaluation.isRequiresHumanReview()).isFalse();
        assertThat(evaluation.isMarkedInvalid()).isFalse();
        assertThat(evaluation.isRequiresRetake()).isFalse();
        assertThat(evaluation.getSampleCount()).isNull();
        assertThat(evaluation.getFeedbackSummary()).isEqualTo("Well done");
        assertThat(evaluation.getStatus()).isEqualTo(AssessmentItemEvaluationStatus.FINALIZED);
        assertThat(evaluation.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
    }
}
