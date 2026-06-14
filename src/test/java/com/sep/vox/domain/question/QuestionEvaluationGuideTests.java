package com.sep.vox.domain.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;

class QuestionEvaluationGuideTests {

    @Test
    void constructor_should_store_guide_fields() {
        var questionId = UUID.randomUUID();
        var guide = new QuestionEvaluationGuide(
            questionId,
            "Expected content",
            "Key points",
            "Acceptable responses",
            "Off topic",
            "Hints",
            "Mistakes"
        );

        assertThat(guide.getQuestionId()).isEqualTo(questionId);
        assertThat(guide.getExpectedContent()).isEqualTo("Expected content");
        assertThat(guide.getKeyPoints()).isEqualTo("Key points");
        assertThat(guide.getAcceptableResponses()).isEqualTo("Acceptable responses");
        assertThat(guide.getOffTopicExamples()).isEqualTo("Off topic");
        assertThat(guide.getScoringHints()).isEqualTo("Hints");
        assertThat(guide.getCommonMistakes()).isEqualTo("Mistakes");
    }
}
