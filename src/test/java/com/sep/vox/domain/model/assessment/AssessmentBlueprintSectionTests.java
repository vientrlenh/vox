package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentBlueprintSectionTests {

    @Test
    void createSetsTheGivenValuesWithoutId() {
        UUID versionId = UUID.randomUUID();

        AssessmentBlueprintSection section = AssessmentBlueprintSection.create(
                versionId, 1, "Title", "Instruction", 60, new BigDecimal("0.5"));

        assertThat(section.getBlueprintVersionId()).isEqualTo(versionId);
        assertThat(section.getOrder()).isEqualTo(1);
        assertThat(section.getTitle()).isEqualTo("Title");
        assertThat(section.getInstruction()).isEqualTo("Instruction");
        assertThat(section.getSectionTimeLimitSeconds()).isEqualTo(60);
        assertThat(section.getSectionWeight()).isEqualByComparingTo("0.5");
        assertThat(section.getId()).isNull();
    }

    @Test
    void revisedWithKeepsIdVersionAndCreationAuditAndReplacesContent() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        AssessmentBlueprintSection existing = AssessmentBlueprintSection.builder()
                .id(UUID.randomUUID())
                .blueprintVersionId(UUID.randomUUID())
                .order(1)
                .title("Old")
                .createdAt(createdAt)
                .createdBy("user-1")
                .build();

        AssessmentBlueprintSection revised = existing.revisedWith(
                2, "New", "Instruction", 30, BigDecimal.ONE);

        assertThat(revised).isNotSameAs(existing);
        assertThat(revised.getId()).isEqualTo(existing.getId());
        assertThat(revised.getBlueprintVersionId()).isEqualTo(existing.getBlueprintVersionId());
        assertThat(revised.getCreatedAt()).isEqualTo(createdAt);
        assertThat(revised.getCreatedBy()).isEqualTo("user-1");
        assertThat(revised.getOrder()).isEqualTo(2);
        assertThat(revised.getTitle()).isEqualTo("New");
        assertThat(revised.getInstruction()).isEqualTo("Instruction");
        assertThat(revised.getSectionTimeLimitSeconds()).isEqualTo(30);
        assertThat(revised.getSectionWeight()).isEqualTo(BigDecimal.ONE);
        assertThat(revised.getUpdatedAt()).isNull();
    }
}
