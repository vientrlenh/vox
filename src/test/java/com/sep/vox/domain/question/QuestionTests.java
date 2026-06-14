package com.sep.vox.domain.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;

class QuestionTests {

    @Test
    void create_should_initialize_draft_question_with_given_fields() {
        var now = OffsetDateTime.now();
        var topicId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();

        var question = Question.create(
            topicId,
            "Q_01",
            "Instruction",
            "Question text",
            "Prompt",
            "Preparation",
            QuestionType.SHORT_ANSWER,
            15,
            30,
            60,
            QuestionScope.QUESTION_BANK,
            QuestionVisibility.BANK_VISIBLE,
            null,
            false,
            now,
            createdBy
        );

        assertThat(question.getQuestionTopicId()).isEqualTo(topicId);
        assertThat(question.getCode()).isEqualTo("Q_01");
        assertThat(question.getInstructionText()).isEqualTo("Instruction");
        assertThat(question.getQuestionText()).isEqualTo("Question text");
        assertThat(question.getPromptText()).isEqualTo("Prompt");
        assertThat(question.getPreparationText()).isEqualTo("Preparation");
        assertThat(question.getType()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(question.getPreparationTimeSeconds()).isEqualTo(15);
        assertThat(question.getMinResponseSeconds()).isEqualTo(30);
        assertThat(question.getMaxResponseSeconds()).isEqualTo(60);
        assertThat(question.getScope()).isEqualTo(QuestionScope.QUESTION_BANK);
        assertThat(question.getVisibility()).isEqualTo(QuestionVisibility.BANK_VISIBLE);
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(question.isLocked()).isFalse();
        assertThat(question.getCreatedAt()).isEqualTo(now);
        assertThat(question.getUpdatedAt()).isEqualTo(now);
        assertThat(question.getCreatedBy()).isEqualTo(createdBy);
        assertThat(question.getUpdatedBy()).isEqualTo(createdBy);
    }
}
