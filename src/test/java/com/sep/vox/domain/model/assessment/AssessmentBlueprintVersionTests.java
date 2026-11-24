package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentBlueprintVersionTests {

    @Test
    void draftStartsInDraftAndLeavesTheTimeLimitEmpty() {
        UUID blueprintId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T00:00:00Z");

        AssessmentBlueprintVersion version = AssessmentBlueprintVersion.draft(blueprintId, 3, "BP-V3", from, to);

        assertThat(version.getBlueprintId()).isEqualTo(blueprintId);
        assertThat(version.getVersion()).isEqualTo(3);
        assertThat(version.getCode()).isEqualTo("BP-V3");
        assertThat(version.getStatus()).isEqualTo(AssessmentBlueprintVersionStatus.DRAFT);
        assertThat(version.getEffectiveFrom()).isEqualTo(from);
        assertThat(version.getEffectiveTo()).isEqualTo(to);
        assertThat(version.getTotalTimeLimitSeconds()).isNull();
        assertThat(version.getId()).isNull();
    }
}
