package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TopicSuggestionTests {

    @Test
    void keywordRequestedSetsTheRequestDefaults() {
        UUID studentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        TopicSuggestion suggestion = TopicSuggestion.keywordRequested(studentId, "football", "CREATED", createdAt);

        assertThat(suggestion.getStudentId()).isEqualTo(studentId);
        assertThat(suggestion.getSuggestedTopicName()).isEqualTo("football");
        assertThat(suggestion.getKeyword()).isEqualTo("football");
        assertThat(suggestion.getReasonText()).isEqualTo("CREATED");
        assertThat(suggestion.getCreatedAt()).isEqualTo(createdAt);
        assertThat(suggestion.getInterestDimension()).isEqualTo("UNKNOWN");
        assertThat(suggestion.getCurriculumGroup()).isNull();
        assertThat(suggestion.getConfidence()).isEqualTo(BigDecimal.ZERO);
        assertThat(suggestion.getEvidenceJson()).isEqualTo("{}");
        assertThat(suggestion.getStatus()).isEqualTo("REQUESTED");
        assertThat(suggestion.getRespondedAt()).isNull();
        assertThat(suggestion.getId()).isNull();
    }
}
