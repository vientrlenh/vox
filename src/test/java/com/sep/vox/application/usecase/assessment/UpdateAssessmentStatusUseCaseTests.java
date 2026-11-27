package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentStatusCommand;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Focuses on the assessment schedule time-window guard of the SCHEDULE action. validatePlanLimits runs first, so
 * enough subscription/plan/quota must be stubbed to get past it.
 */
class UpdateAssessmentStatusUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private UserContextPort userContextPort;
    private UpdateAssessmentStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    private final Instant open = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new UpdateAssessmentStatusUseCase(
            assessmentRepository,
            assessmentMemberRepository,
            assessmentPaperRepository,
            assessmentScheduleRepository,
            assessmentScheduleProctorRepository,
            assessmentCandidateRepository,
            assessmentSessionRepository,
            mock(AssessmentCandidateResultRepository.class),
            mock(AssessmentPolicyRepository.class),
            mock(AssessmentCandidateResultFinalizationService.class),
            mock(ZeroScoreAssessmentResultService.class),
            schoolUserRepository,
            userRoleQueryRepository,
            mock(AssessmentQuestionSecureLockService.class),
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            userContextPort,
            mock(EventPublisherPort.class),
            mock(ClassTestGradingAssignmentService.class),
            mock(AssessmentHumanGradingNotificationService.class),
            classTestTokenQuotaGuardService,
            // The real service (not mocked) on the same fake repo: the guard and the session cascade are checked
            // here too; the full status mapping table lives in AssessmentScheduleClosureServiceTests.
            new AssessmentScheduleClosureService(assessmentScheduleRepository, assessmentSessionRepository));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));
        givenGenerousPlan();
    }

    @Test
    void should_reject_schedule_action_when_class_test_window_shorter_than_assessment_time() {
        // An open/close window of 30 minutes, with the computed assessment duration being 60 minutes.
        Assessment assessment = classTest(3600, open, open.plus(30, ChronoUnit.MINUTES));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("closing time");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * Scheduling a class test NO LONGER publishes its sessions automatically: the session must
     * already have been published by hand beforehand (UpdateAssessmentScheduleStatusUseCase), and this
     * action writes nothing to the session -- same as a centralized assessment.
     */
    @Test
    void should_schedule_class_test_when_all_schedules_published() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.SCHEDULED);
        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /** Twin of should_reject_schedule_when_centralized_assessment_has_draft_schedule. */
    @Test
    void should_reject_schedule_when_class_test_has_draft_schedule() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("There are still 1 assessment schedules not published");
        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.DRAFT);
        verify(assessmentScheduleRepository, never()).save(any());
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_schedule_when_token_quota_exceeded() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);
        doThrow(new BusinessRuleViolationException("The quota has been exceeded"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(assessment);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("quota has been exceeded");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_schedule_when_class_test_schedule_has_no_room() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        schedule.setSchoolRoomId(null);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The assessment schedule has no room selected");
    }

    @Test
    void should_reject_schedule_when_class_test_schedule_has_no_proctor() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The assessment schedule has no proctor");
    }

    @Test
    void should_reject_schedule_when_class_test_candidate_has_no_schedule() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        AssessmentCandidate unassigned = assignedCandidate(schedule.getId());
        unassigned.setScheduleId(null);
        when(assessmentCandidateRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(unassigned));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("There are still 1 students not placed in an assessment schedule");
    }

    @Test
    void should_reject_schedule_when_class_test_candidate_has_no_paper() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        AssessmentCandidate withoutPaper = assignedCandidate(schedule.getId());
        withoutPaper.setAssignedPaperId(null);
        when(assessmentCandidateRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(withoutPaper));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("There are still 1 students not assigned a paper");
    }

    // --- Centralized assessment: it used to be schedulable with nothing in it, a "scheduled" assessment nobody could take ---

    @Test
    void should_reject_schedule_when_centralized_assessment_has_no_schedule() {
        givenCentralizedAssessment();
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no assessment schedule");
    }

    @Test
    void should_reject_schedule_when_centralized_assessment_has_no_candidate() {
        givenCentralizedAssessment();
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(assessmentCandidateRepository.countByAssessmentId(assessmentId)).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no candidate");
    }

    @Test
    void should_reject_schedule_when_centralized_assessment_has_no_paper() {
        givenCentralizedAssessment();
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no paper");
    }

    /**
     * A DRAFT session is invisible to students and proctors -- an assessment that is "scheduled" with a
     * session nobody can enter. Publishing each session is a separate action, so it must be done
     * before scheduling.
     */
    @Test
    void should_reject_schedule_when_centralized_assessment_has_draft_schedule() {
        givenCentralizedAssessment();
        AssessmentSchedule draft = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(draft, published));
        when(assessmentScheduleProctorRepository.countByScheduleId(draft.getId())).thenReturn(1L);
        when(assessmentScheduleProctorRepository.countByScheduleId(published.getId())).thenReturn(1L);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(new AssessmentPaper()));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("There are still 1 assessment schedules not published");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_schedule_centralized_assessment_when_ready() {
        givenCentralizedAssessment();
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(new AssessmentPaper()));

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.SCHEDULED);
    }

    /**
     * The council chair runs the whole centralized-assessment workflow, so they are also the one who
     * clicks schedule. Previously the CENTRALIZED branch only accepted the school admin, so the
     * chair teacher finished every step and was then stuck at the last button.
     */
    @Test
    void should_schedule_centralized_assessment_when_caller_is_the_chair() {
        givenCentralizedAssessment();
        givenCallerIsChairInsteadOfSchoolAdmin();
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(new AssessmentPaper()));

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.SCHEDULED);
    }

    @Test
    void should_reject_status_change_when_caller_is_neither_school_admin_nor_chair() {
        givenCentralizedAssessment();
        givenCallerIsChairInsteadOfSchoolAdmin();
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Access denied");
        verify(assessmentRepository, never()).save(any());
    }

    // --- A cancelled/moved assessment schedule no longer takes place, so it must not count in any scheduling condition ---

    /**
     * A session can be cancelled straight from DRAFT, before any room or proctor is assigned. If
     * its room/proctors were still inspected, the assessment would be blocked by a session that will
     * never take place, with a message that does not say which session -- and the only way out
     * would be assigning a room to the cancelled session, or removing all proctors and then
     * deleting the session.
     */
    @Test
    void should_ignore_cancelled_schedule_when_checking_room_and_proctor() {
        givenCentralizedAssessment();
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        AssessmentSchedule cancelled = schedule(AssessmentScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        cancelled.setSchoolRoomId(null);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(published, cancelled));
        when(assessmentScheduleProctorRepository.countByScheduleId(published.getId())).thenReturn(1L);
        when(assessmentScheduleProctorRepository.countByScheduleId(cancelled.getId())).thenReturn(0L);

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.SCHEDULED);
    }

    @Test
    void should_ignore_moved_schedule_when_checking_room_and_proctor() {
        givenCentralizedAssessment();
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        AssessmentSchedule moved = schedule(AssessmentScheduleStatus.MOVED, open, open.plus(2, ChronoUnit.HOURS));
        moved.setSchoolRoomId(null);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(published, moved));
        when(assessmentScheduleProctorRepository.countByScheduleId(published.getId())).thenReturn(1L);
        when(assessmentScheduleProctorRepository.countByScheduleId(moved.getId())).thenReturn(0L);

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.SCHEDULED);
    }

    /**
     * If the cancelled sessions are set aside and no session remains, the assessment is empty, not
     * "ready".
     */
    @Test
    void should_reject_schedule_when_every_centralized_schedule_is_cancelled() {
        givenCentralizedAssessment();
        AssessmentSchedule cancelled = schedule(AssessmentScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(cancelled));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no assessment schedule");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_ignore_cancelled_schedule_when_class_test_schedule_ready() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        AssessmentSchedule cancelled = schedule(AssessmentScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        cancelled.setSchoolRoomId(null);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(published, cancelled));
        when(assessmentScheduleProctorRepository.countByScheduleId(cancelled.getId())).thenReturn(0L);
        givenScheduleReady(published);

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.SCHEDULED);
    }

    @Test
    void should_reject_schedule_when_every_class_test_schedule_is_cancelled() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule cancelled = schedule(AssessmentScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(cancelled));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no schedule");
        verify(assessmentRepository, never()).save(any());
    }

    /**
     * The status must be checked before the plan quota: pressing SCHEDULE on a test that is no
     * longer DRAFT and getting "plan limit exceeded" would send the user to fix the wrong thing.
     */
    @Test
    void should_report_invalid_status_before_plan_limit_when_assessment_not_draft() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "SCHEDULE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid for this action");
    }

    @Test
    void should_reject_cancel_when_assessment_already_published_results() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.RESULTS_PUBLISHED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid for this action");
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.RESULTS_PUBLISHED);
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_cancel_when_assessment_already_cancelled() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.CANCELLED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid for this action");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_allow_cancel_from_draft() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        AssessmentDto result = useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null));

        assertThat(result).isNotNull();
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.CANCELLED);
    }

    @Test
    void should_allow_cancel_from_in_progress() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null));

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.CANCELLED);
    }

    /**
     * Closing the test but leaving its sessions behind means a session stays PUBLISHED forever --
     * there is no way to move it to COMPLETED unless someone clicks each session by hand.
     */
    @Test
    void should_close_schedules_when_assessment_is_closed() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        // A session that ended yesterday and a session next week that has not started yet.
        AssessmentSchedule ended = schedule(AssessmentScheduleStatus.PUBLISHED,
            open.minus(1, ChronoUnit.DAYS), open.minus(22, ChronoUnit.HOURS));
        AssessmentSchedule notStarted = schedule(AssessmentScheduleStatus.PUBLISHED,
            Instant.now().plus(7, ChronoUnit.DAYS), Instant.now().plus(8, ChronoUnit.DAYS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(ended, notStarted));

        useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CLOSE", null));

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.CLOSED);
        assertThat(ended.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(notStarted.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        verify(assessmentScheduleRepository).save(ended);
        verify(assessmentScheduleRepository).save(notStarted);
    }

    @Test
    void should_reject_close_when_ongoing_schedule_still_has_active_session() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        AssessmentSchedule ongoing = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(assessmentSessionRepository.countActiveByAssessmentId(assessmentId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CLOSE", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2 students taking the assessment");
        assertThat(ongoing.getStatus()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * Even when the whole class has submitted, the teacher must still be able to close early --
     * this is why there is no hard block by time window.
     */
    @Test
    void should_allow_close_while_schedule_is_ongoing_but_nobody_is_working() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        AssessmentSchedule ongoing = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(ongoing));
        when(assessmentSessionRepository.countActiveByAssessmentId(assessmentId)).thenReturn(0L);

        useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CLOSE", null));

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.CLOSED);
        assertThat(ongoing.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
    }

    /**
     * A PUBLISHED session still slips through isVisibleToStudent()/allowsAttendance() and the
     * queries that hard-code status = 'PUBLISHED' -- cancelling the assessment without taking its
     * sessions along leaves a session "ready".
     */
    @Test
    void should_cancel_schedules_when_assessment_is_cancelled() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.SCHEDULED);
        AssessmentSchedule draft = schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(draft, published));

        useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null));

        assertThat(draft.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        assertThat(published.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        verify(assessmentScheduleRepository).save(draft);
        verify(assessmentScheduleRepository).save(published);
    }

    @Test
    void should_not_cancel_completed_or_moved_schedules() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.CLOSED);
        AssessmentSchedule completed = schedule(AssessmentScheduleStatus.COMPLETED, open, open.plus(2, ChronoUnit.HOURS));
        AssessmentSchedule moved = schedule(AssessmentScheduleStatus.MOVED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(completed, moved));

        useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null));

        assertThat(completed.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(moved.getStatus()).isEqualTo(AssessmentScheduleStatus.MOVED);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_not_change_schedules_when_cancel_transition_is_rejected() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setStatus(AssessmentStatus.RESULTS_PUBLISHED);
        AssessmentSchedule published = schedule(AssessmentScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(published));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "CANCEL", null)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(published.getStatus()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * The centralized assessment is built in DRAFT, the caller is a SCHOOL_ADMIN of the same school, and
     * by default it already has enough candidates + paper variants -- each test breaks exactly one
     * condition itself. For the council chair branch see
     * {@link #givenCallerIsChairInsteadOfSchoolAdmin()}.
     */
    private void givenCentralizedAssessment() {
        Assessment assessment = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        assessment.setKind(AssessmentKind.CENTRALIZED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(schoolId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));

        when(assessmentCandidateRepository.countByAssessmentId(assessmentId)).thenReturn(1L);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(new AssessmentPaper()));
    }

    /** Demotes the caller from school admin to a teacher who only holds the CHAIR role of that very assessment. */
    private void givenCallerIsChairInsteadOfSchoolAdmin() {
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
    }

    private void givenGenerousPlan() {
        SchoolSubscription subscription = mock(SchoolSubscription.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getSubscriptionPlanId()).thenReturn(planId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));

        SubscriptionPlan plan = mock(SubscriptionPlan.class);
        when(plan.getMaxTimePerAttemptMin()).thenReturn(null);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        when(assessmentCandidateRepository.countByAssessmentId(assessmentId)).thenReturn(1L);
        // classTestTokenQuotaGuardService is a plain mock: requireWithinTokenQuota() no-ops by
        // default, i.e. "quota is sufficient". The calculation details for
        // GRADING/CLASS_TEST/personal quota are tested separately in
        // ClassTestTokenQuotaGuardServiceTests; here it is enough to confirm that
        // UpdateAssessmentStatusUseCase calls the guard and respects the exception the guard throws (see
        // should_reject_schedule_when_token_quota_exceeded).
    }

    private Assessment classTest(Integer assessmentTimeDurationSecond, Instant openAt, Instant closeAt) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.DRAFT);
        assessment.setMaxAttempt(1);
        assessment.setAssessmentTimeDurationSecond(assessmentTimeDurationSecond);
        assessment.setOpenAt(openAt);
        assessment.setCloseAt(closeAt);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status, Instant from, Instant to) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(assessmentId);
        schedule.setSchoolRoomId(roomId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }

    /** Every assessment schedule has a proctor and every student has been placed in a schedule and assigned a paper. */
    private void givenScheduleReady(AssessmentSchedule schedule) {
        when(assessmentScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(assessmentCandidateRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(assignedCandidate(schedule.getId())));
    }

    private AssessmentCandidate assignedCandidate(UUID scheduleId) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(assessmentId);
        candidate.setScheduleId(scheduleId);
        candidate.setAssignedPaperId(UUID.randomUUID());
        return candidate;
    }
}
