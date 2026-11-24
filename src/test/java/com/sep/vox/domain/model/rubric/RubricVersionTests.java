package com.sep.vox.domain.model.rubric;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubricVersionTests {

    @Test
    void draftSetsTheGivenValuesAndStartsInDraft() {
        UUID rubricId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T00:00:00Z");

        RubricVersion version = RubricVersion.draft(
                rubricId, 2, "R_V2", "Name", "desc", from, to,
                BigDecimal.ZERO, BigDecimal.TEN, RubricTotalScoreMethod.WEIGHTED_AVERAGE);

        assertThat(version.getRubricId()).isEqualTo(rubricId);
        assertThat(version.getVersion()).isEqualTo(2);
        assertThat(version.getCode()).isEqualTo("R_V2");
        assertThat(version.getName()).isEqualTo("Name");
        assertThat(version.getDescription()).isEqualTo("desc");
        assertThat(version.getStatus()).isEqualTo(RubricStatus.DRAFT);
        assertThat(version.getEffectiveFrom()).isEqualTo(from);
        assertThat(version.getEffectiveTo()).isEqualTo(to);
        assertThat(version.getScoringScaleMin()).isEqualTo(BigDecimal.ZERO);
        assertThat(version.getScoringScaleMax()).isEqualTo(BigDecimal.TEN);
        assertThat(version.getTotalScoreMethod()).isEqualTo(RubricTotalScoreMethod.WEIGHTED_AVERAGE);
        assertThat(version.getId()).isNull();
        assertThat(version.getSourceRubricVersionId()).isNull();
    }
}
