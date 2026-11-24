package com.sep.vox.domain.model.rubric;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubricCriterionTests {

    @Test
    void createSetsTheGivenValuesAndLeavesTheIdUnset() {
        UUID versionId = UUID.randomUUID();
        UUID frameworkCriterionId = UUID.randomUUID();

        RubricCriterion criterion = RubricCriterion.create(
                versionId, frameworkCriterionId, "FLU", "Fluency", "desc", null,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, 3, true);

        assertThat(criterion.getRubricVersionId()).isEqualTo(versionId);
        assertThat(criterion.getFrameworkCriterionId()).isEqualTo(frameworkCriterionId);
        assertThat(criterion.getCode()).isEqualTo("FLU");
        assertThat(criterion.getName()).isEqualTo("Fluency");
        assertThat(criterion.getDescription()).isEqualTo("desc");
        assertThat(criterion.getExamples()).isNull();
        assertThat(criterion.getWeight()).isEqualTo(BigDecimal.ONE);
        assertThat(criterion.getMinScore()).isEqualTo(BigDecimal.ZERO);
        assertThat(criterion.getMaxScore()).isEqualTo(BigDecimal.TEN);
        assertThat(criterion.getOrder()).isEqualTo(3);
        assertThat(criterion.isRequired()).isTrue();
        assertThat(criterion.getId()).isNull();
    }
}
