package com.sep.vox.domain.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionEvaluationGuideTests {

    @Test
    void forQuestionSetsTheGivenValuesWithoutId() {
        UUID questionId = UUID.randomUUID();

        QuestionEvaluationGuide guide = QuestionEvaluationGuide.forQuestion(
                questionId, "expected", "points", "acceptable", "offTopic", "hints", "mistakes");

        assertThat(guide.getQuestionId()).isEqualTo(questionId);
        assertThat(guide.getExpectedContent()).isEqualTo("expected");
        assertThat(guide.getKeyPoints()).isEqualTo("points");
        assertThat(guide.getAcceptableResponses()).isEqualTo("acceptable");
        assertThat(guide.getOffTopicExamples()).isEqualTo("offTopic");
        assertThat(guide.getScoringHints()).isEqualTo("hints");
        assertThat(guide.getCommonMistakes()).isEqualTo("mistakes");
        assertThat(guide.getId()).isNull();
    }

    @Test
    void emptyForLeavesEveryGuideFieldNull() {
        UUID questionId = UUID.randomUUID();

        QuestionEvaluationGuide guide = QuestionEvaluationGuide.emptyFor(questionId);

        assertThat(guide.getQuestionId()).isEqualTo(questionId);
        assertThat(guide).usingRecursiveComparison()
                .ignoringFields("questionId")
                .isEqualTo(new QuestionEvaluationGuide());
    }

    @Test
    void copyToKeepsTheGuideTextForAnotherQuestionWithoutId() {
        QuestionEvaluationGuide source = QuestionEvaluationGuide.forQuestion(
                UUID.randomUUID(), "expected", "points", "acceptable", "offTopic", "hints", "mistakes");
        source.setId(UUID.randomUUID());
        UUID targetQuestionId = UUID.randomUUID();

        QuestionEvaluationGuide copy = source.copyTo(targetQuestionId);

        assertThat(copy).usingRecursiveComparison().ignoringFields("id", "questionId").isEqualTo(source);
        assertThat(copy.getQuestionId()).isEqualTo(targetQuestionId);
        assertThat(copy.getId()).isNull();
    }
}
