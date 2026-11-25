package com.sep.vox.domain.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionTopicTests {

    @Test
    void draftSetsTheGivenValuesAndTheDraftStatus() {
        UUID bankId = UUID.randomUUID();

        QuestionTopic topic = QuestionTopic.draft(bankId, "T-1", "Name", "Description");

        assertThat(topic.getQuestionBankId()).isEqualTo(bankId);
        assertThat(topic.getCode()).isEqualTo("T-1");
        assertThat(topic.getName()).isEqualTo("Name");
        assertThat(topic.getDescription()).isEqualTo("Description");
        assertThat(topic.getStatus()).isEqualTo(QuestionTopicStatus.DRAFT);
        assertThat(topic.getId()).isNull();
    }
}
