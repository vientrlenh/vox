package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentBlueprintTests {

    @Test
    void createSetsTheGivenValuesAndIsActive() {
        UUID schoolId = UUID.randomUUID();
        UUID languageId = UUID.randomUUID();
        UUID gradeLevelId = UUID.randomUUID();

        AssessmentBlueprint blueprint = AssessmentBlueprint.create(
                schoolId, languageId, gradeLevelId, "BP-1", "Name", "Description");

        assertThat(blueprint.getSchoolId()).isEqualTo(schoolId);
        assertThat(blueprint.getLanguageId()).isEqualTo(languageId);
        assertThat(blueprint.getGradeLevelId()).isEqualTo(gradeLevelId);
        assertThat(blueprint.getCode()).isEqualTo("BP-1");
        assertThat(blueprint.getName()).isEqualTo("Name");
        assertThat(blueprint.getDescription()).isEqualTo("Description");
        assertThat(blueprint.isActive()).isTrue();
        assertThat(blueprint.getId()).isNull();
    }
}
