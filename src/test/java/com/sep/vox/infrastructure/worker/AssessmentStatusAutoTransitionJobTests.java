package com.sep.vox.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Most class tests close through this path rather than by someone clicking by hand, so its side-effect must match
 * {@code UpdateAssessmentStatusUseCase.CLOSE} -- including the part that carries the sessions along.
 */
class AssessmentStatusAutoTransitionJobTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentStatusAutoTransitionJob job;

    private final UUID assessmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);

        when(assessmentRepository.findByStatusAndOpenAtBefore(any(), any())).thenReturn(List.of());
        when(assessmentRepository.findByStatusAndCloseAtBefore(any(), any())).thenReturn(List.of());

        job = jobWith(new AssessmentScheduleClosureService(assessmentScheduleRepository, assessmentSessionRepository));
    }

    @Test
    void should_close_schedules_when_auto_closing_a_class_test() {
        Assessment assessment = classTest();
        AssessmentSchedule ended = schedule(AssessmentScheduleStatus.PUBLISHED,
            Instant.now().minus(3, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));
        when(assessmentRepository.findByStatusAndCloseAtBefore(eq(AssessmentStatus.IN_PROGRESS), any())).thenReturn(List.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(ended));

        job.run();

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.CLOSED);
        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        verify(assessmentScheduleRepository).save(ended);
    }

    /**
     * A defensive branch: skewed data makes closeAt arrive before the session ends. The job skips
     * this round instead of cutting the assessment off in the middle -- the next tick (60s) tries again.
     */
    @Test
    void should_defer_closing_when_a_student_is_still_working() {
        Assessment assessment = classTest();
        AssessmentSchedule ongoing = schedule(AssessmentScheduleStatus.PUBLISHED,
            Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS));
        when(assessmentRepository.findByStatusAndCloseAtBefore(eq(AssessmentStatus.IN_PROGRESS), any())).thenReturn(List.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(assessmentSessionRepository.countActiveByAssessmentId(assessmentId)).thenReturn(1L);

        job.run();

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
        verify(assessmentRepository, never()).save(any(Assessment.class));
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * The closure guard rejects with IllegalArgumentException; the job must postpone closing and carry on with the
     * next assessment instead of letting the exception abort the whole tick.
     */
    @Test
    void should_postpone_closing_and_continue_when_the_closure_guard_rejects() {
        Assessment blocked = classTest();
        Assessment closable = classTest();
        closable.setId(UUID.randomUUID());
        AssessmentScheduleClosureService closureService = mock(AssessmentScheduleClosureService.class);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Still has an active session"))
            .when(closureService).requireNoActiveSessionInOngoingSchedule(eq(blocked.getId()), any());
        when(assessmentRepository.findByStatusAndCloseAtBefore(eq(AssessmentStatus.IN_PROGRESS), any()))
            .thenReturn(List.of(blocked, closable));
        AssessmentStatusAutoTransitionJob jobWithMockedGuard = jobWith(closureService);

        jobWithMockedGuard.run();

        assertThat(blocked.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
        assertThat(closable.getStatus()).isEqualTo(AssessmentStatus.CLOSED);
        verify(assessmentRepository, never()).save(blocked);
        verify(closureService, never()).closeSchedulesForAssessment(eq(blocked.getId()), any());
    }

    /**
     * A centralized assessment does not close itself -- this job deliberately only accepts class tests.
     */
    @Test
    void should_skip_centralized_assessment() {
        Assessment assessment = classTest();
        assessment.setKind(AssessmentKind.CENTRALIZED);
        when(assessmentRepository.findByStatusAndCloseAtBefore(eq(AssessmentStatus.IN_PROGRESS), any())).thenReturn(List.of(assessment));

        job.run();

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    private AssessmentStatusAutoTransitionJob jobWith(AssessmentScheduleClosureService closureService) {
        return new AssessmentStatusAutoTransitionJob(
            assessmentRepository,
            mock(AssessmentPaperRepository.class),
            mock(AssessmentQuestionSecureLockService.class),
            mock(ZeroScoreAssessmentResultService.class),
            mock(ClassTestGradingAssignmentService.class),
            mock(AssessmentHumanGradingNotificationService.class),
            closureService);
    }

    private Assessment classTest() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status, Instant from, Instant to) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(assessmentId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
