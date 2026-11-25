package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TopicInterestScoreEntryTests {

    @Test
    void scoredSetsEveryArgument() {
        UUID topicId = UUID.randomUUID();
        Instant lastEventAt = Instant.parse("2026-01-01T00:00:00Z");

        TopicInterestScoreEntry entry = TopicInterestScoreEntry.scored(topicId, 0.75, 4, lastEventAt);

        assertThat(entry.getTopicId()).isEqualTo(topicId);
        assertThat(entry.getScore()).isEqualTo(0.75);
        assertThat(entry.getSessionCount()).isEqualTo(4);
        assertThat(entry.getLastEventAt()).isEqualTo(lastEventAt);
    }

    @Test
    void scoredAllowsAnEmptyLastEventAt() {
        TopicInterestScoreEntry entry = TopicInterestScoreEntry.scored(UUID.randomUUID(), 0.1, 0, null);

        assertThat(entry.getLastEventAt()).isNull();
    }
}
