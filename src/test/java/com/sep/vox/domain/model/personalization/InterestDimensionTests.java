package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InterestDimensionTests {

    @Test
    void definedLeavesTheAuditTimestampsEmpty() {
        InterestDimension dimension = InterestDimension.defined("SPORT", "Sport", "Games", true, false, 3);

        assertThat(dimension.getCode()).isEqualTo("SPORT");
        assertThat(dimension.getLabel()).isEqualTo("Sport");
        assertThat(dimension.getDescription()).isEqualTo("Games");
        assertThat(dimension.isActive()).isTrue();
        assertThat(dimension.isQuizEligible()).isFalse();
        assertThat(dimension.getDisplayOrder()).isEqualTo(3);
        assertThat(dimension.getCreatedAt()).isNull();
        assertThat(dimension.getUpdatedAt()).isNull();
    }

    @Test
    void withEditableFieldsKeepsCodeAndCreatedAtAndClearsUpdatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        InterestDimension existing = InterestDimension.builder()
                .code("SPORT").label("Sport").description("Games").active(true).quizEligible(true)
                .displayOrder(1).createdAt(createdAt).updatedAt(Instant.parse("2026-02-01T00:00:00Z")).build();

        InterestDimension edited = existing.withEditableFields("Sports", null, false, false, 5);

        assertThat(edited.getCode()).isEqualTo("SPORT");
        assertThat(edited.getCreatedAt()).isEqualTo(createdAt);
        assertThat(edited.getUpdatedAt()).isNull();
        assertThat(edited.getLabel()).isEqualTo("Sports");
        assertThat(edited.getDescription()).isNull();
        assertThat(edited.isActive()).isFalse();
        assertThat(edited.isQuizEligible()).isFalse();
        assertThat(edited.getDisplayOrder()).isEqualTo(5);
        assertThat(existing.getLabel()).isEqualTo("Sport");
    }
}
