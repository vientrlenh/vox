package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.infrastructure.persistence.adapter.AssessmentScheduleRepositoryImpl;
import com.sep.vox.infrastructure.security.CustomUserDetails;
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
            scheduleId, null, false, START.plusSeconds(60), null);

        assertThat(updated).isEqualTo(1);
        AssessmentSchedule stored = reload(scheduleId);
        assertThat(stored.getSchoolRoomId()).isEqualTo(roomId);
        assertThat(stored.getStartDate()).isEqualTo(START.plusSeconds(60));
        assertThat(stored.getEndDate()).isEqualTo(END);
    }

    @Test
    void whenTheRoomIsProvidedAsNull_thenTheStoredRoomIsCleared() {
        UUID scheduleId = savedScheduleInRoom(UUID.randomUUID());

        assessmentScheduleRepository.updateAtomic(scheduleId, null, true, null, null);

        assertThat(reload(scheduleId).getSchoolRoomId()).isNull();
    }

    @Test
    void whenTheRoomIsProvidedWithAValue_thenTheRoomIsReplaced() {
        UUID newRoomId = UUID.randomUUID();
        UUID scheduleId = savedScheduleInRoom(UUID.randomUUID());

        assessmentScheduleRepository.updateAtomic(scheduleId, newRoomId, true, null, null);

        assertThat(reload(scheduleId).getSchoolRoomId()).isEqualTo(newRoomId);
    }

    @Test
    void whenUpdated_thenUpdatedByIsTheCurrentActorAndUpdatedAtMoves() {
        UUID userId = UUID.randomUUID();
        UUID scheduleId = savedScheduleInRoom(UUID.randomUUID());
        Instant createdAt = reload(scheduleId).getUpdatedAt();
        logIn(userId);

        assessmentScheduleRepository.updateAtomic(scheduleId, null, false, START.plusSeconds(60), null);

        AssessmentSchedule stored = reload(scheduleId);
        assertThat(stored.getUpdatedBy()).isEqualTo(userId.toString());
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(createdAt.truncatedTo(ChronoUnit.MICROS));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void logIn(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.builder().id(userId).build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
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
