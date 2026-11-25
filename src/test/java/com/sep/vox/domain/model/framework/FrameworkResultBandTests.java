package com.sep.vox.domain.model.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FrameworkResultBandTests {

    @Test
    void forVersionSetsEveryArgument() {
        UUID versionId = UUID.randomUUID();

        FrameworkResultBand band = FrameworkResultBand.forVersion(versionId, "B1", "Intermediate", "mid level", 3);

        assertThat(band.getId()).isNull();
        assertThat(band.getFrameworkVersionId()).isEqualTo(versionId);
        assertThat(band.getCode()).isEqualTo("B1");
        assertThat(band.getLabel()).isEqualTo("Intermediate");
        assertThat(band.getDescription()).isEqualTo("mid level");
        assertThat(band.getOrder()).isEqualTo(3);
    }
}
