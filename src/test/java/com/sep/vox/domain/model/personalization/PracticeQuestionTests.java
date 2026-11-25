package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticeQuestionTests {

    @Test
    void generatedSetsEveryArgumentAndTheInitialDefaults() {
        UUID id = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        PracticeQuestion question = PracticeQuestion.generated(
                id, topicId, "Tell me about...", "FLUENCY", "pace", "PAST", 2, "{\"f\":1}", "{\"g\":1}",
                "[\"i\"]", "OPEN", 90, 30, 2, createdAt);

        assertThat(question.getId()).isEqualTo(id);
        assertThat(question.getPracticeTopicId()).isEqualTo(topicId);
        assertThat(question.getQuestionText()).isEqualTo("Tell me about...");
        assertThat(question.getTargetCriterionCode()).isEqualTo("FLUENCY");
        assertThat(question.getTargetSubAttribute()).isEqualTo("pace");
        assertThat(question.getTargetTense()).isEqualTo("PAST");
        assertThat(question.getDifficultyRank()).isEqualTo(2);
        assertThat(question.getDifficultyFeaturesJson()).isEqualTo("{\"f\":1}");
        assertThat(question.getEvaluationGuideJson()).isEqualTo("{\"g\":1}");
        assertThat(question.getSuggestedIdeasJson()).isEqualTo("[\"i\"]");
        assertThat(question.getQuestionType()).isEqualTo("OPEN");
        assertThat(question.getMaxResponseSeconds()).isEqualTo(90);
        assertThat(question.getMinResponseSeconds()).isEqualTo(30);
        assertThat(question.getVstepPart()).isEqualTo(2);
        assertThat(question.getCreatedAt()).isEqualTo(createdAt);
        assertThat(question.getSource()).isNull();
        assertThat(question.getUsageCount()).isZero();
        assertThat(question.isActive()).isTrue();
    }
}
