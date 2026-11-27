package com.sep.vox.domain.model.gradelevel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GradeLevelTests {

    @Test
    void registeredSetsEveryArgumentAndStartsActive() {
        GradeLevel gradeLevel = GradeLevel.registered("G10", "Grade 10", "First year", 10);

        assertThat(gradeLevel.getId()).isNull();
        assertThat(gradeLevel.getCode()).isEqualTo("G10");
        assertThat(gradeLevel.getName()).isEqualTo("Grade 10");
        assertThat(gradeLevel.getDescription()).isEqualTo("First year");
        assertThat(gradeLevel.getOrder()).isEqualTo(10);
        assertThat(gradeLevel.getStatus()).isEqualTo(GradeLevelStatus.ACTIVE);
    }
}
