package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.infrastructure.persistence.adapter.AssessmentScheduleRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

/** The atomic schedule update must tell an omitted room (keep) from an explicit null room (clear). */
@RepositoryTest
@Import(AssessmentScheduleRepositoryImpl.class)
class AssessmentScheduleAtomicUpdateTests {

    private static final Instant START = Instant.parse("2026-07-10T01:00:00Z");
    private static final Instant END = START.plusSeconds(7200);

    @Autowired
    private AssessmentScheduleRepository assessmentScheduleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenTheRoomIsNotProvided_thenTheStoredRoomIsKept() {
        UUID roomId = UUID.randomUUID();
        UUID scheduleId = savedScheduleInRoom(roomId);

        int updated = assessmentScheduleRepository.updateAtomic(
            scheduleId, null, false, START.plusSeconds(60), null, Instant.now(), "user");

        assertThat(updated).isEqualTo(1);
        AssessmentSchedule stored = reload(scheduleId);
        assertThat(stored.getSchoolRoomId()).isEqualTo(roomId);
        assertThat(stored.getStartDate()).isEqualTo(START.plusSeconds(60));
        assertThat(stored.getEndDate()).isEqualTo(END);
    }

    @Test
    void whenTheRoomIsProvidedAsNull_thenTheStoredRoomIsCleared() {
        UUID scheduleId = savedScheduleInRoom(UUID.randomUUID());

        assessmentScheduleRepository.updateAtomic(scheduleId, null, true, null, null, Instant.now(), "user");

        assertThat(reload(scheduleId).getSchoolRoomId()).isNull();
    }

    @Test
    void whenTheRoomIsProvidedWithAValue_thenTheRoomIsReplaced() {
        UUID newRoomId = UUID.randomUUID();
        UUID scheduleId = savedScheduleInRoom(UUID.randomUUID());

        assessmentScheduleRepository.updateAtomic(scheduleId, newRoomId, true, null, null, Instant.now(), "user");

        assertThat(reload(scheduleId).getSchoolRoomId()).isEqualTo(newRoomId);
    }

    private UUID savedScheduleInRoom(UUID roomId) {
        return assessmentScheduleRepository
            .save(AssessmentSchedule.createFresh(UUID.randomUUID(), roomId, START, END)).getId();
    }

    private AssessmentSchedule reload(UUID scheduleId) {
        entityManager.flush();
        entityManager.clear();
        return assessmentScheduleRepository.findById(scheduleId).orElseThrow();
    }
}
