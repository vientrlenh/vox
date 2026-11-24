package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

class AssessmentBlueprintSlotTests {

    private static final QuestionSelectionSpec SELECTION_SPEC =
            new QuestionSelectionSpec(QuestionType.values()[0], null, null, null, null);

    @Test
    void createSetsTheGivenValuesWithoutId() {
        UUID sectionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID fixedQuestionId = UUID.randomUUID();

        AssessmentBlueprintSlot slot = AssessmentBlueprintSlot.create(
                sectionId, versionId, 2, BigDecimal.ONE, 10, 20,
                AssessmentBlueprintSlotType.FIXED, fixedQuestionId, SELECTION_SPEC);

        assertThat(slot.getSectionId()).isEqualTo(sectionId);
        assertThat(slot.getBlueprintVersionId()).isEqualTo(versionId);
        assertThat(slot.getOrder()).isEqualTo(2);
        assertThat(slot.getWeight()).isEqualTo(BigDecimal.ONE);
        assertThat(slot.getPrepTimeSecondsOverride()).isEqualTo(10);
        assertThat(slot.getResponseTimeSecondsOverride()).isEqualTo(20);
        assertThat(slot.getSlotType()).isEqualTo(AssessmentBlueprintSlotType.FIXED);
        assertThat(slot.getFixedQuestionId()).isEqualTo(fixedQuestionId);
        assertThat(slot.getSelectionSpec()).isEqualTo(SELECTION_SPEC);
        assertThat(slot.getId()).isNull();
    }

    @Test
    void revisedWithKeepsIdPlacementAndCreationAuditAndReplacesContent() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        AssessmentBlueprintSlot existing = AssessmentBlueprintSlot.builder()
                .id(UUID.randomUUID())
                .sectionId(UUID.randomUUID())
                .blueprintVersionId(UUID.randomUUID())
                .order(1)
                .createdAt(createdAt)
                .createdBy("System")
                .build();
        UUID newFixedQuestionId = UUID.randomUUID();

        AssessmentBlueprintSlot revised = existing.revisedWith(
                4, BigDecimal.TEN, 5, 6, AssessmentBlueprintSlotType.FIXED, newFixedQuestionId, null);

        assertThat(revised).isNotSameAs(existing);
        assertThat(revised.getId()).isEqualTo(existing.getId());
        assertThat(revised.getSectionId()).isEqualTo(existing.getSectionId());
        assertThat(revised.getBlueprintVersionId()).isEqualTo(existing.getBlueprintVersionId());
        assertThat(revised.getCreatedAt()).isEqualTo(createdAt);
        assertThat(revised.getCreatedBy()).isEqualTo("System");
        assertThat(revised.getOrder()).isEqualTo(4);
        assertThat(revised.getWeight()).isEqualTo(BigDecimal.TEN);
        assertThat(revised.getPrepTimeSecondsOverride()).isEqualTo(5);
        assertThat(revised.getResponseTimeSecondsOverride()).isEqualTo(6);
        assertThat(revised.getSlotType()).isEqualTo(AssessmentBlueprintSlotType.FIXED);
        assertThat(revised.getFixedQuestionId()).isEqualTo(newFixedQuestionId);
        assertThat(revised.getSelectionSpec()).isNull();
    }
}
