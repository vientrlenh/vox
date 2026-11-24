package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentItemCriterionScoreTests {

    @Test
    void scoredUsesTheScoreAsRawAndFinalScoreAndKeepsTheBand() {
        UUID evaluationId = UUID.randomUUID();
        UUID criterionId = UUID.randomUUID();

        AssessmentItemCriterionScore score = AssessmentItemCriterionScore.scored(
                evaluationId, criterionId, new BigDecimal("3.5"), "Clear speech", "B2");

        assertThat(score.getEvaluationId()).isEqualTo(evaluationId);
        assertThat(score.getRubricCriterionId()).isEqualTo(criterionId);
        assertThat(score.getRawScore()).isEqualByComparingTo("3.5");
        assertThat(score.getFinalScore()).isEqualByComparingTo("3.5");
        assertThat(score.getRationale()).isEqualTo("Clear speech");
        assertThat(score.getMatchedBandCode()).isEqualTo("B2");
        assertThat(score.getId()).isNull();
    }

    @Test
    void scoredWithoutBandLeavesTheMatchedBandEmpty() {
        UUID evaluationId = UUID.randomUUID();
        UUID criterionId = UUID.randomUUID();

        AssessmentItemCriterionScore score = AssessmentItemCriterionScore.scoredWithoutBand(
                evaluationId, criterionId, new BigDecimal("2"), "Teacher note");

        assertThat(score.getEvaluationId()).isEqualTo(evaluationId);
        assertThat(score.getRubricCriterionId()).isEqualTo(criterionId);
        assertThat(score.getRawScore()).isEqualByComparingTo("2");
        assertThat(score.getFinalScore()).isEqualByComparingTo("2");
        assertThat(score.getRationale()).isEqualTo("Teacher note");
        assertThat(score.getMatchedBandCode()).isNull();
    }
}
