package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentPaperTests {

    @Test
    void draftStartsInDraftWithZeroDuration() {
        UUID assessmentId = UUID.randomUUID();
        UUID blueprintVersionId = UUID.randomUUID();

        AssessmentPaper paper = AssessmentPaper.draft(assessmentId, blueprintVersionId, "A-P2", 2);

        assertThat(paper.getAssessmentId()).isEqualTo(assessmentId);
        assertThat(paper.getBlueprintVersionId()).isEqualTo(blueprintVersionId);
        assertThat(paper.getCode()).isEqualTo("A-P2");
        assertThat(paper.getVariant()).isEqualTo(2);
        assertThat(paper.getStatus()).isEqualTo(AssessmentPaperStatus.DRAFT);
        assertThat(paper.getTimeDurationSeconds()).isZero();
        assertThat(paper.getId()).isNull();
    }

    @Test
    void draftWithoutBlueprintLeavesTheBlueprintVersionEmpty() {
        UUID assessmentId = UUID.randomUUID();

        AssessmentPaper paper = AssessmentPaper.draftWithoutBlueprint(assessmentId, "A-P1", 1);

        assertThat(paper.getAssessmentId()).isEqualTo(assessmentId);
        assertThat(paper.getBlueprintVersionId()).isNull();
        assertThat(paper.getCode()).isEqualTo("A-P1");
        assertThat(paper.getVariant()).isEqualTo(1);
        assertThat(paper.getStatus()).isEqualTo(AssessmentPaperStatus.DRAFT);
        assertThat(paper.getTimeDurationSeconds()).isZero();
    }
}
