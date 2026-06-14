package com.sep.vox.domain.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;

class QuestionTopicTests {

    @Test
    void is_active_should_return_true_only_when_status_published() {
        var topic = new QuestionTopic(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "TOPIC_01",
            "Topic",
            "Description",
            QuestionTopicStatus.PUBLISHED,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        assertThat(topic.isActive()).isTrue();

        topic.setStatus(QuestionTopicStatus.DRAFT);
        assertThat(topic.isActive()).isFalse();

        topic.setStatus(QuestionTopicStatus.ARCHIVED);
        assertThat(topic.isActive()).isFalse();
    }
}
