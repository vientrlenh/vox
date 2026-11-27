package com.sep.vox.domain.model.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FrameworkCriterionTests {

    @Test
    void forVersionSetsEveryArgument() {
        UUID versionId = UUID.randomUUID();

        FrameworkCriterion criterion = FrameworkCriterion.forVersion(versionId, "FLUENCY", "Fluency", "pace", 2);

        assertThat(criterion.getId()).isNull();
        assertThat(criterion.getFrameworkVersionId()).isEqualTo(versionId);
        assertThat(criterion.getCode()).isEqualTo("FLUENCY");
        assertThat(criterion.getName()).isEqualTo("Fluency");
        assertThat(criterion.getDescription()).isEqualTo("pace");
        assertThat(criterion.getOrder()).isEqualTo(2);
    }
}
