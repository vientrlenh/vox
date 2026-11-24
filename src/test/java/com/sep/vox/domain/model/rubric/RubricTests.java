package com.sep.vox.domain.model.rubric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubricTests {

    @Test
    void forSchoolSetsTheGivenValuesAndTheSchoolOwner() {
        UUID languageId = UUID.randomUUID();
        UUID frameworkId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();

        Rubric rubric = Rubric.forSchool(languageId, frameworkId, "ENG-K10", "English 10", "desc", schoolId);

        assertThat(rubric.getLanguageId()).isEqualTo(languageId);
        assertThat(rubric.getFrameworkId()).isEqualTo(frameworkId);
        assertThat(rubric.getCode()).isEqualTo("ENG-K10");
        assertThat(rubric.getName()).isEqualTo("English 10");
        assertThat(rubric.getDescription()).isEqualTo("desc");
        assertThat(rubric.getOwnerType()).isEqualTo(RubricOwnerType.SCHOOL);
        assertThat(rubric.getSchoolId()).isEqualTo(schoolId);
        assertThat(rubric.getId()).isNull();
    }

    @Test
    void forSystemSetsTheGivenValuesAndHasNoSchool() {
        UUID languageId = UUID.randomUUID();
        UUID frameworkId = UUID.randomUUID();

        Rubric rubric = Rubric.forSystem(languageId, frameworkId, "CEFR", "CEFR", null);

        assertThat(rubric.getLanguageId()).isEqualTo(languageId);
        assertThat(rubric.getFrameworkId()).isEqualTo(frameworkId);
        assertThat(rubric.getCode()).isEqualTo("CEFR");
        assertThat(rubric.getDescription()).isNull();
        assertThat(rubric.getOwnerType()).isEqualTo(RubricOwnerType.SYSTEM);
        assertThat(rubric.getSchoolId()).isNull();
        assertThat(rubric.getId()).isNull();
    }
}
