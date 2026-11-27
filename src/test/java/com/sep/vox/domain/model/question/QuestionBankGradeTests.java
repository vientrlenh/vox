package com.sep.vox.domain.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionBankGradeTests {

    @Test
    void attachedSetsTheGivenValuesWithoutId() {
        UUID bankId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID attachedBy = UUID.randomUUID();
        Instant attachedAt = Instant.parse("2026-01-01T00:00:00Z");

        QuestionBankGrade grade = QuestionBankGrade.attached(bankId, gradeId, attachedAt, attachedBy);

        assertThat(grade.getQuestionBankId()).isEqualTo(bankId);
        assertThat(grade.getSchoolGradeId()).isEqualTo(gradeId);
        assertThat(grade.getAttachedAt()).isEqualTo(attachedAt);
        assertThat(grade.getAttachedBy()).isEqualTo(attachedBy);
        assertThat(grade.getId()).isNull();
    }
}
