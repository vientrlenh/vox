package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentResultAppealItemTests {

    @Test
    void forResponseHasNoFinalScoreYet() {
        UUID appealId = UUID.randomUUID();
        UUID paperItemId = UUID.randomUUID();
        UUID responseId = UUID.randomUUID();

        AssessmentResultAppealItem item = AssessmentResultAppealItem.forResponse(appealId, paperItemId, responseId);

        assertThat(item.getId()).isNull();
        assertThat(item.getAppealId()).isEqualTo(appealId);
        assertThat(item.getPaperItemId()).isEqualTo(paperItemId);
        assertThat(item.getResponseId()).isEqualTo(responseId);
        assertThat(item.getFinalScore()).isNull();
    }
}
