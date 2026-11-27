package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.service.personalization.TensePolicy;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticeTopicTests {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void generatedFromNameUsesTheNameAsDescriptionAndFillsDefaults() {
        PracticeTopic topic = PracticeTopic.generatedFromName(
                "USER_GENERATED", "Football", "football", "SPORT", null, null, CREATED_AT);

        assertThat(topic.getId()).isNull();
        assertThat(topic.getName()).isEqualTo("Football");
        assertThat(topic.getNormalizedName()).isEqualTo("football");
        assertThat(topic.getDescription()).isEqualTo("Football");
        assertThat(topic.getSource()).isEqualTo("USER_GENERATED");
        assertThat(topic.getInterestDimension()).isEqualTo("SPORT");
        assertThat(topic.getCurriculumGroup()).isEqualTo("OUT_OF_CURRICULUM");
        assertThat(topic.getTemporalAffordance()).isEqualTo(TensePolicy.AFFORDANCE_MIXED);
        assertThat(topic.isActive()).isTrue();
        assertThat(topic.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(topic.getSourceQuestionTopicId()).isNull();
    }

    @Test
    void generatedFromNameKeepsGivenCurriculumGroupAndAffordance() {
        PracticeTopic topic = PracticeTopic.generatedFromName(
                "AI", "Past trips", "past trips", "TRAVEL", "FAMILY", "PAST", CREATED_AT);

        assertThat(topic.getSource()).isEqualTo("AI");
        assertThat(topic.getCurriculumGroup()).isEqualTo("FAMILY");
        assertThat(topic.getTemporalAffordance()).isEqualTo("PAST");
    }

    @Test
    void fromAssessmentQuestionTopicLinksTheSourceTopicAndUsesMixedTense() {
        UUID sourceId = UUID.randomUUID();

        PracticeTopic topic = PracticeTopic.fromAssessmentQuestionTopic(
                sourceId, "Environment", "environment", "About nature", "GENERAL", "SCHOOL", CREATED_AT);

        assertThat(topic.getId()).isNull();
        assertThat(topic.getName()).isEqualTo("Environment");
        assertThat(topic.getNormalizedName()).isEqualTo("environment");
        assertThat(topic.getDescription()).isEqualTo("About nature");
        assertThat(topic.getSource()).isEqualTo("ASSESSMENT_QUESTION_BANK");
        assertThat(topic.getInterestDimension()).isEqualTo("GENERAL");
        assertThat(topic.getCurriculumGroup()).isEqualTo("SCHOOL");
        assertThat(topic.isActive()).isTrue();
        assertThat(topic.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(topic.getSourceQuestionTopicId()).isEqualTo(sourceId);
        assertThat(topic.getTemporalAffordance()).isEqualTo(TensePolicy.AFFORDANCE_MIXED);
    }
}
