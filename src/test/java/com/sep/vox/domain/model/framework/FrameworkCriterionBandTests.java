package com.sep.vox.domain.model.framework;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FrameworkCriterionBandTests {

    @Test
    void forCriterionSetsEveryArgument() {
        UUID criterionId = UUID.randomUUID();
        UUID resultBandId = UUID.randomUUID();
        FrameworkCriterionSignals positive = new FrameworkCriterionSignals(List.of());
        FrameworkCriterionSignals negative = new FrameworkCriterionSignals(List.of());

        FrameworkCriterionBand band =
                FrameworkCriterionBand.forCriterion(criterionId, resultBandId, "descriptor", positive, negative);

        assertThat(band.getId()).isNull();
        assertThat(band.getFrameworkCriterionId()).isEqualTo(criterionId);
        assertThat(band.getFrameworkResultBandId()).isEqualTo(resultBandId);
        assertThat(band.getDescriptor()).isEqualTo("descriptor");
        assertThat(band.getPositiveSignals()).isSameAs(positive);
        assertThat(band.getNegativeSignals()).isSameAs(negative);
    }
}
