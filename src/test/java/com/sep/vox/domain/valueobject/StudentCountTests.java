package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class StudentCountTests {

    @Test
    void should_accept_positive_student_count() {
        var studentCount = new StudentCount(1);

        assertThat(studentCount.value()).isEqualTo(1);
    }

    @Test
    void should_reject_zero_student_count() {
        assertThrows(IllegalArgumentException.class, () -> new StudentCount(0));
    }

    @Test
    void should_reject_negative_student_count() {
        assertThrows(IllegalArgumentException.class, () -> new StudentCount(-1));
    }
}
