package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentPaperItemTests {

    private static AssessmentBlueprintSlot blueprintSlot() {
        return AssessmentBlueprintSlot.builder()
                .id(UUID.randomUUID())
                .order(5)
                .weight(new BigDecimal("0.25"))
                .build();
    }

    @Test
    void emptySlotHasNeitherQuestionNorBlueprintSlotId() {
        UUID sectionId = UUID.randomUUID();
        UUID paperId = UUID.randomUUID();

        AssessmentPaperItem item = AssessmentPaperItem.emptySlot(sectionId, paperId, blueprintSlot());

        assertThat(item.getSectionId()).isEqualTo(sectionId);
        assertThat(item.getPaperId()).isEqualTo(paperId);
        assertThat(item.getOrder()).isEqualTo(5);
        assertThat(item.getWeight()).isEqualByComparingTo("0.25");
        assertThat(item.getQuestionId()).isNull();
        assertThat(item.getBlueprintSlotId()).isNull();
    }

    @Test
    void forBlueprintSlotRemembersTheSlotAndTakesItsOrderAndWeight() {
        UUID sectionId = UUID.randomUUID();
        UUID paperId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        AssessmentBlueprintSlot slot = blueprintSlot();

        AssessmentPaperItem item = AssessmentPaperItem.forBlueprintSlot(sectionId, paperId, slot, questionId);

        assertThat(item.getBlueprintSlotId()).isEqualTo(slot.getId());
        assertThat(item.getSectionId()).isEqualTo(sectionId);
        assertThat(item.getPaperId()).isEqualTo(paperId);
        assertThat(item.getQuestionId()).isEqualTo(questionId);
        assertThat(item.getOrder()).isEqualTo(5);
        assertThat(item.getWeight()).isEqualByComparingTo("0.25");
    }

    @Test
    void forBlueprintSlotAllowsAnEmptyQuestion() {
        AssessmentPaperItem item = AssessmentPaperItem.forBlueprintSlot(
                UUID.randomUUID(), UUID.randomUUID(), blueprintSlot(), null);

        assertThat(item.getQuestionId()).isNull();
        assertThat(item.getBlueprintSlotId()).isNotNull();
    }

    @Test
    void forQuestionSetsTheGivenValuesWithoutBlueprintSlot() {
        UUID sectionId = UUID.randomUUID();
        UUID paperId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        AssessmentPaperItem item = AssessmentPaperItem.forQuestion(sectionId, paperId, questionId, 2, BigDecimal.TEN);

        assertThat(item.getSectionId()).isEqualTo(sectionId);
        assertThat(item.getPaperId()).isEqualTo(paperId);
        assertThat(item.getQuestionId()).isEqualTo(questionId);
        assertThat(item.getOrder()).isEqualTo(2);
        assertThat(item.getWeight()).isEqualTo(BigDecimal.TEN);
        assertThat(item.getBlueprintSlotId()).isNull();
        assertThat(item.getId()).isNull();
    }

    @Test
    void copyIntoKeepsSlotQuestionOrderAndWeightAndMovesToTarget() {
        UUID targetPaperId = UUID.randomUUID();
        UUID targetSectionId = UUID.randomUUID();
        AssessmentPaperItem source = AssessmentPaperItem.builder()
                .id(UUID.randomUUID())
                .blueprintSlotId(UUID.randomUUID())
                .sectionId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .questionId(UUID.randomUUID())
                .order(3)
                .weight(BigDecimal.ONE)
                .build();

        AssessmentPaperItem copy = source.copyInto(targetPaperId, targetSectionId);

        assertThat(copy.getId()).isNull();
        assertThat(copy.getBlueprintSlotId()).isEqualTo(source.getBlueprintSlotId());
        assertThat(copy.getQuestionId()).isEqualTo(source.getQuestionId());
        assertThat(copy.getOrder()).isEqualTo(3);
        assertThat(copy.getWeight()).isEqualTo(BigDecimal.ONE);
        assertThat(copy.getPaperId()).isEqualTo(targetPaperId);
        assertThat(copy.getSectionId()).isEqualTo(targetSectionId);
    }
}
