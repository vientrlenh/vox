package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentScheduleProctorTests {

    @Test
    void assignedLinksTheTeacherToTheSchedule() {
        UUID scheduleId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        AssessmentScheduleProctor proctor = AssessmentScheduleProctor.assigned(scheduleId, teacherId);

        assertThat(proctor.getId()).isNull();
        assertThat(proctor.getScheduleId()).isEqualTo(scheduleId);
        assertThat(proctor.getTeacherId()).isEqualTo(teacherId);
    }
}
