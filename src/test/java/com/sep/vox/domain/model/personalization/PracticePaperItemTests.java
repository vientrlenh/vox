package com.sep.vox.domain.model.personalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticePaperItemTests {

    @Test
    void forSlotSetsEveryArgument() {
        UUID paperId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        PracticePaperItem item = PracticePaperItem.forSlot(paperId, questionId, 2, "FLUENCY", "pace", 3);

        assertThat(item.getId()).isNull();
        assertThat(item.getPracticePaperId()).isEqualTo(paperId);
        assertThat(item.getPracticeQuestionId()).isEqualTo(questionId);
        assertThat(item.getSlotOrder()).isEqualTo(2);
        assertThat(item.getTargetCriterionCode()).isEqualTo("FLUENCY");
        assertThat(item.getTargetSubAttribute()).isEqualTo("pace");
        assertThat(item.getTargetDifficultyRank()).isEqualTo(3);
    }
}
