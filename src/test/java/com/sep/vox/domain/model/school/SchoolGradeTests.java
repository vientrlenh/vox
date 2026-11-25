package com.sep.vox.domain.model.school;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchoolGradeTests {

    @Test
    void openedSetsEveryArgumentAndStartsActive() {
        UUID schoolId = UUID.randomUUID();
        UUID gradeLevelId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2027, 5, 31);

        SchoolGrade grade = SchoolGrade.opened(schoolId, gradeLevelId, "G10-2026", "Grade 10", "First year", startDate, endDate);

        assertThat(grade.getId()).isNull();
        assertThat(grade.getSchoolId()).isEqualTo(schoolId);
        assertThat(grade.getGradeLevelId()).isEqualTo(gradeLevelId);
        assertThat(grade.getCode()).isEqualTo("G10-2026");
        assertThat(grade.getName()).isEqualTo("Grade 10");
        assertThat(grade.getDescription()).isEqualTo("First year");
        assertThat(grade.getStartDate()).isEqualTo(startDate);
        assertThat(grade.getEndDate()).isEqualTo(endDate);
        assertThat(grade.getStatus()).isEqualTo(SchoolGradeStatus.ACTIVE);
    }
}
