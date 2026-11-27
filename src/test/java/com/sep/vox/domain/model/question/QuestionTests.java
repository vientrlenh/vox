package com.sep.vox.domain.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionTests {

    @Test
    void draftSetsTheGivenValuesAndTheDraftDefaults() {
        UUID bankId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();

        Question question = Question.draft(bankId, topicId, "Q-1", "Instruction", "Text", "Prompt", "Prepare",
                QuestionType.SHORT_ANSWER, 30, 10, 60, QuestionSharing.PRIVATE);

        assertThat(question.getQuestionBankId()).isEqualTo(bankId);
        assertThat(question.getQuestionTopicId()).isEqualTo(topicId);
        assertThat(question.getCode()).isEqualTo("Q-1");
        assertThat(question.getInstructionText()).isEqualTo("Instruction");
        assertThat(question.getQuestionText()).isEqualTo("Text");
        assertThat(question.getPromptText()).isEqualTo("Prompt");
        assertThat(question.getPreparationText()).isEqualTo("Prepare");
        assertThat(question.getType()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(question.getPreparationTimeSeconds()).isEqualTo(30);
        assertThat(question.getMinResponseSeconds()).isEqualTo(10);
        assertThat(question.getMaxResponseSeconds()).isEqualTo(60);
        assertThat(question.getSharing()).isEqualTo(QuestionSharing.PRIVATE);
        assertThat(question.getSourceQuestionId()).isNull();
        assertThat(question.isLocked()).isFalse();
        assertThat(question.getConfidentiality()).isEqualTo(QuestionConfidentiality.OPEN);
        assertThat(question.getSecurePoolId()).isNull();
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(question.getId()).isNull();
    }

    @Test
    void draftCopyWithCodeCopiesContentAndPointsBackToTheSource() {
        Question source = Question.draft(UUID.randomUUID(), UUID.randomUUID(), "Q-1", "Instruction", "Text",
                "Prompt", "Prepare", QuestionType.SHORT_ANSWER, 30, 10, 60, QuestionSharing.PRIVATE);
        source.setId(UUID.randomUUID());
        source.setLocked(true);
        source.setStatus(QuestionStatus.PUBLISHED);
        source.setConfidentiality(QuestionConfidentiality.RELEASED);
        source.setSecurePoolId(UUID.randomUUID());

        Question copy = source.draftCopyWithCode("Q-1-COPY");

        assertThat(copy).usingRecursiveComparison()
                .ignoringFields("id", "code", "sourceQuestionId", "locked", "status")
                .isEqualTo(source);
        assertThat(copy.getCode()).isEqualTo("Q-1-COPY");
        assertThat(copy.getSourceQuestionId()).isEqualTo(source.getId());
        assertThat(copy.isLocked()).isFalse();
        assertThat(copy.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(copy.getId()).isNull();
    }
}
