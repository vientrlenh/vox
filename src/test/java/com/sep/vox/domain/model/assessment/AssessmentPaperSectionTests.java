package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentPaperSectionTests {

    @Test
    void fromBlueprintSectionCopiesEveryBlueprintValue() {
        UUID paperId = UUID.randomUUID();
        AssessmentBlueprintSection blueprintSection = AssessmentBlueprintSection.create(
                UUID.randomUUID(), 3, "Part 3", "Answer", 120, new BigDecimal("0.40"));

        AssessmentPaperSection section = AssessmentPaperSection.fromBlueprintSection(paperId, blueprintSection);

        assertThat(section.getPaperId()).isEqualTo(paperId);
        assertThat(section.getOrder()).isEqualTo(3);
        assertThat(section.getTitle()).isEqualTo("Part 3");
        assertThat(section.getInstruction()).isEqualTo("Answer");
        assertThat(section.getSectionTimeLimitSeconds()).isEqualTo(120);
        assertThat(section.getWeight()).isEqualByComparingTo("0.40");
        assertThat(section.getId()).isNull();
    }

    @Test
    void forClassTestHasNoTimeLimit() {
        UUID paperId = UUID.randomUUID();

        AssessmentPaperSection section = AssessmentPaperSection.forClassTest(
                paperId, 2, "Title", "Instruction", new BigDecimal("0.5"));

        assertThat(section.getPaperId()).isEqualTo(paperId);
        assertThat(section.getOrder()).isEqualTo(2);
        assertThat(section.getTitle()).isEqualTo("Title");
        assertThat(section.getInstruction()).isEqualTo("Instruction");
        assertThat(section.getWeight()).isEqualByComparingTo("0.5");
        assertThat(section.getSectionTimeLimitSeconds()).isNull();
    }

    @Test
    void copyIntoKeepsContentMovesToTargetPaperAndDropsTheId() {
        UUID targetPaperId = UUID.randomUUID();
        AssessmentPaperSection source = AssessmentPaperSection.builder()
                .id(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .order(4)
                .title("T")
                .instruction("I")
                .sectionTimeLimitSeconds(90)
                .weight(BigDecimal.ONE)
                .build();

        AssessmentPaperSection copy = source.copyInto(targetPaperId);

        assertThat(copy.getId()).isNull();
        assertThat(copy.getPaperId()).isEqualTo(targetPaperId);
        assertThat(copy.getOrder()).isEqualTo(4);
        assertThat(copy.getTitle()).isEqualTo("T");
        assertThat(copy.getInstruction()).isEqualTo("I");
        assertThat(copy.getSectionTimeLimitSeconds()).isEqualTo(90);
        assertThat(copy.getWeight()).isEqualTo(BigDecimal.ONE);
    }
}
