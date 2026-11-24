package com.sep.vox.domain.model.rubric;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubricResultBandTests {

    @Test
    void createSetsTheGivenValuesAndLeavesTheIdUnset() {
        UUID versionId = UUID.randomUUID();

        RubricResultBand band = RubricResultBand.create(
                versionId, "B2", "Upper intermediate", null, BigDecimal.valueOf(6), BigDecimal.valueOf(8), 4);

        assertThat(band.getRubricVersionId()).isEqualTo(versionId);
        assertThat(band.getCode()).isEqualTo("B2");
        assertThat(band.getName()).isEqualTo("Upper intermediate");
        assertThat(band.getDescription()).isNull();
        assertThat(band.getScoreMin()).isEqualTo(BigDecimal.valueOf(6));
        assertThat(band.getScoreMax()).isEqualTo(BigDecimal.valueOf(8));
        assertThat(band.getOrder()).isEqualTo(4);
        assertThat(band.getId()).isNull();
    }
}
