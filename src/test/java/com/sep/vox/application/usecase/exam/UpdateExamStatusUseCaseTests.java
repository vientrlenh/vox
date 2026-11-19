package com.sep.vox.application.usecase.exam;

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

import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ExamHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamStatusUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Tập trung vào guard khung giờ ca thi ở action SCHEDULE. validatePlanLimits chạy trước nên phải
 * stub đủ subscription/plan/quota để đi qua được.
 */
class UpdateExamStatusUseCaseTests {

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamSessionRepository examSessionRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private UserContextPort userContextPort;
    private UpdateExamStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    private final Instant open = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new UpdateExamStatusUseCase(
            examRepository,
            examMemberRepository,
            examPaperRepository,
            examScheduleRepository,
            examScheduleProctorRepository,
            examCandidateRepository,
            examSessionRepository,
            mock(ExamCandidateResultRepository.class),
            mock(AssessmentPolicyRepository.class),
            mock(ExamCandidateResultFinalizationService.class),
            mock(ZeroScoreExamResultService.class),
            schoolUserRepository,
            userRoleQueryRepository,
            mock(ExamQuestionSecureLockService.class),
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            userContextPort,
            mock(EventPublisherPort.class),
            mock(ClassTestGradingAssignmentService.class),
            mock(ExamHumanGradingNotificationService.class),
            classTestTokenQuotaGuardService,
            // Service thật (không mock) trên cùng repo giả: guard và cascade ca thi được kiểm luôn
            // ở đây; bảng ánh xạ trạng thái đầy đủ nằm ở ExamScheduleClosureServiceTests.
            new ExamScheduleClosureService(examScheduleRepository, examSessionRepository));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        givenGenerousPlan();
    }

    @Test
    void should_reject_schedule_action_when_class_test_window_shorter_than_exam_time() {
        // An open/close window of 30 minutes, with the computed exam duration being 60 minutes.
        Exam exam = classTest(3600, open, open.plus(30, ChronoUnit.MINUTES));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("thời gian đóng bài");
        verify(examScheduleRepository, never()).save(any());
    }

    /**
     * Scheduling a class test NO LONGER publishes its sessions automatically: the session must
     * already have been published by hand beforehand (UpdateExamScheduleStatusUseCase), and this
     * action writes nothing to the session -- same as a centralized exam.
     */
    @Test
    void should_schedule_class_test_when_all_schedules_published() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository, never()).save(any());
    }

    /** Twin of should_reject_schedule_when_centralized_exam_has_draft_schedule. */
    @Test
    void should_reject_schedule_when_class_test_has_draft_schedule() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Còn 1 ca thi chưa được công bố");
        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.DRAFT);
        verify(examScheduleRepository, never()).save(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_schedule_when_token_quota_exceeded() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);
        doThrow(new PlanLimitExceededException("Đã vượt quá hạn mức"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(exam);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("vượt quá hạn mức");
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_schedule_when_class_test_schedule_has_no_room() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        schedule.setSchoolRoomId(null);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ca thi chưa được chọn phòng");
    }

    @Test
    void should_reject_schedule_when_class_test_schedule_has_no_proctor() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ca thi chưa có giám khảo");
    }

    @Test
    void should_reject_schedule_when_class_test_candidate_has_no_schedule() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        ExamCandidate unassigned = assignedCandidate(schedule.getId());
        unassigned.setScheduleId(null);
        when(examCandidateRepository.findByExamId(examId)).thenReturn(List.of(unassigned));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Còn 1 học sinh chưa được xếp vào ca thi");
    }

    @Test
    void should_reject_schedule_when_class_test_candidate_has_no_paper() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        ExamCandidate withoutPaper = assignedCandidate(schedule.getId());
        withoutPaper.setAssignedPaperId(null);
        when(examCandidateRepository.findByExamId(examId)).thenReturn(List.of(withoutPaper));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Còn 1 học sinh chưa được gán đề");
    }

    // --- Kỳ thi tập trung: trước đây lên lịch được khi chưa có gì, bài "đã lên lịch" mà không ai thi được ---

    @Test
    void should_reject_schedule_when_centralized_exam_has_no_schedule() {
        givenCentralizedExam();
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa có ca thi");
    }

    @Test
    void should_reject_schedule_when_centralized_exam_has_no_candidate() {
        givenCentralizedExam();
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examCandidateRepository.countByExamId(examId)).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa có thí sinh");
    }

    @Test
    void should_reject_schedule_when_centralized_exam_has_no_paper() {
        givenCentralizedExam();
        ExamSchedule schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa có mã đề");
    }

    /**
     * A DRAFT session is invisible to students and proctors -- an exam that is "scheduled" with a
     * session nobody can enter. Publishing each session is a separate action, so it must be done
     * before scheduling.
     */
    @Test
    void should_reject_schedule_when_centralized_exam_has_draft_schedule() {
        givenCentralizedExam();
        ExamSchedule draft = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(draft, published));
        when(examScheduleProctorRepository.countByScheduleId(draft.getId())).thenReturn(1L);
        when(examScheduleProctorRepository.countByScheduleId(published.getId())).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(new ExamPaper()));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Còn 1 ca thi chưa được công bố");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_schedule_centralized_exam_when_ready() {
        givenCentralizedExam();
        ExamSchedule schedule = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(new ExamPaper()));

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
    }

    /**
     * The council chair runs the whole centralized-exam workflow, so they are also the one who
     * clicks schedule. Previously the CENTRALIZED branch only accepted the school admin, so the
     * chair teacher finished every step and was then stuck at the last button.
     */
    @Test
    void should_schedule_centralized_exam_when_caller_is_the_chair() {
        givenCentralizedExam();
        givenCallerIsChairInsteadOfSchoolAdmin();
        ExamSchedule schedule = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(new ExamPaper()));

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
    }

    @Test
    void should_reject_status_change_when_caller_is_neither_school_admin_nor_chair() {
        givenCentralizedExam();
        givenCallerIsChairInsteadOfSchoolAdmin();
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
        verify(examRepository, never()).save(any());
    }

    // --- Ca đã huỷ/dời không còn diễn ra, nên không được tham gia bất kỳ điều kiện lên lịch nào ---

    /**
     * A session can be cancelled straight from DRAFT, before any room or proctor is assigned. If
     * its room/proctors were still inspected, the exam would be blocked by a session that will
     * never take place, with a message that does not say which session -- and the only way out
     * would be assigning a room to the cancelled session, or removing all proctors and then
     * deleting the session.
     */
    @Test
    void should_ignore_cancelled_schedule_when_checking_room_and_proctor() {
        givenCentralizedExam();
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        ExamSchedule cancelled = schedule(ExamScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        cancelled.setSchoolRoomId(null);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(published, cancelled));
        when(examScheduleProctorRepository.countByScheduleId(published.getId())).thenReturn(1L);
        when(examScheduleProctorRepository.countByScheduleId(cancelled.getId())).thenReturn(0L);

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
    }

    @Test
    void should_ignore_moved_schedule_when_checking_room_and_proctor() {
        givenCentralizedExam();
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        ExamSchedule moved = schedule(ExamScheduleStatus.MOVED, open, open.plus(2, ChronoUnit.HOURS));
        moved.setSchoolRoomId(null);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(published, moved));
        when(examScheduleProctorRepository.countByScheduleId(published.getId())).thenReturn(1L);
        when(examScheduleProctorRepository.countByScheduleId(moved.getId())).thenReturn(0L);

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
    }

    /**
     * If the cancelled sessions are set aside and no session remains, the exam is empty, not
     * "ready".
     */
    @Test
    void should_reject_schedule_when_every_centralized_schedule_is_cancelled() {
        givenCentralizedExam();
        ExamSchedule cancelled = schedule(ExamScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(cancelled));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa có ca thi");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_ignore_cancelled_schedule_when_class_test_schedule_ready() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        ExamSchedule cancelled = schedule(ExamScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        cancelled.setSchoolRoomId(null);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(published, cancelled));
        when(examScheduleProctorRepository.countByScheduleId(cancelled.getId())).thenReturn(0L);
        givenScheduleReady(published);

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
    }

    @Test
    void should_reject_schedule_when_every_class_test_schedule_is_cancelled() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        ExamSchedule cancelled = schedule(ExamScheduleStatus.CANCELLED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(cancelled));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa có lịch");
        verify(examRepository, never()).save(any());
    }

    /**
     * The status must be checked before the plan quota: pressing SCHEDULE on a test that is no
     * longer DRAFT and getting "plan limit exceeded" would send the user to fix the wrong thing.
     */
    @Test
    void should_report_invalid_status_before_plan_limit_when_exam_not_draft() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ cho action này");
    }

    @Test
    void should_reject_cancel_when_exam_already_published_results() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.RESULTS_PUBLISHED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ cho action này");
        assertThat(exam.getStatus()).isEqualTo(ExamStatus.RESULTS_PUBLISHED);
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_cancel_when_exam_already_cancelled() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.CANCELLED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ cho action này");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_allow_cancel_from_draft() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamDto result = useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(result).isNotNull();
        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CANCELLED);
    }

    @Test
    void should_allow_cancel_from_in_progress() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CANCELLED);
    }

    /**
     * Closing the test but leaving its sessions behind means a session stays PUBLISHED forever --
     * there is no way to move it to COMPLETED unless someone clicks each session by hand.
     */
    @Test
    void should_close_schedules_when_exam_is_closed() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        // A session that ended yesterday and a session next week that has not started yet.
        ExamSchedule ended = schedule(ExamScheduleStatus.PUBLISHED,
            open.minus(1, ChronoUnit.DAYS), open.minus(22, ChronoUnit.HOURS));
        ExamSchedule notStarted = schedule(ExamScheduleStatus.PUBLISHED,
            Instant.now().plus(7, ChronoUnit.DAYS), Instant.now().plus(8, ChronoUnit.DAYS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ended, notStarted));

        useCase.execute(new UpdateExamStatusCommand(examId, "CLOSE", null));

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CLOSED);
        assertThat(ended.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(notStarted.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        verify(examScheduleRepository).save(ended);
        verify(examScheduleRepository).save(notStarted);
    }

    @Test
    void should_reject_close_when_ongoing_schedule_still_has_active_session() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        ExamSchedule ongoing = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CLOSE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("2 học sinh đang làm bài");
        assertThat(ongoing.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository, never()).save(any());
    }

    /**
     * Even when the whole class has submitted, the teacher must still be able to close early --
     * this is why there is no hard block by time window.
     */
    @Test
    void should_allow_close_while_schedule_is_ongoing_but_nobody_is_working() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        ExamSchedule ongoing = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(0L);

        useCase.execute(new UpdateExamStatusCommand(examId, "CLOSE", null));

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CLOSED);
        assertThat(ongoing.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
    }

    /**
     * A PUBLISHED session still slips through isVisibleToStudent()/allowsAttendance() and the
     * queries that hard-code status = 'PUBLISHED' -- cancelling the exam without taking its
     * sessions along leaves a session "ready".
     */
    @Test
    void should_cancel_schedules_when_exam_is_cancelled() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.SCHEDULED);
        ExamSchedule draft = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(draft, published));

        useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(draft.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        assertThat(published.getStatus()).isEqualTo(ExamScheduleStatus.CANCELLED);
        assertThat(published.getUpdatedBy()).isEqualTo(userId);
        verify(examScheduleRepository).save(draft);
        verify(examScheduleRepository).save(published);
    }

    @Test
    void should_not_cancel_completed_or_moved_schedules() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.CLOSED);
        ExamSchedule completed = schedule(ExamScheduleStatus.COMPLETED, open, open.plus(2, ChronoUnit.HOURS));
        ExamSchedule moved = schedule(ExamScheduleStatus.MOVED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(completed, moved));

        useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(completed.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(moved.getStatus()).isEqualTo(ExamScheduleStatus.MOVED);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_not_change_schedules_when_cancel_transition_is_rejected() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.RESULTS_PUBLISHED);
        ExamSchedule published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(published));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null)))
            .isInstanceOf(IllegalStateException.class);
        assertThat(published.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository, never()).save(any());
    }

    /**
     * The centralized exam is built in DRAFT, the caller is a SCHOOL_ADMIN of the same school, and
     * by default it already has enough candidates + paper variants -- each test breaks exactly one
     * condition itself. For the council chair branch see
     * {@link #givenCallerIsChairInsteadOfSchoolAdmin()}.
     */
    private void givenCentralizedExam() {
        Exam exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setKind(ExamKind.CENTRALIZED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(schoolId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));

        when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(new ExamPaper()));
    }

    /** Hạ người gọi từ quản trị trường xuống giáo viên chỉ giữ vai trò CHAIR của chính kỳ thi đó. */
    private void givenCallerIsChairInsteadOfSchoolAdmin() {
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
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

        when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
        // classTestTokenQuotaGuardService is a plain mock: requireWithinTokenQuota() no-ops by
        // default, i.e. "quota is sufficient". The calculation details for
        // GRADING/CLASS_TEST/personal quota are tested separately in
        // ClassTestTokenQuotaGuardServiceTests; here it is enough to confirm that
        // UpdateExamStatusUseCase calls the guard and respects the exception the guard throws (see
        // should_reject_schedule_when_token_quota_exceeded).
    }

    private Exam classTest(Integer examTimeDurationSecond, Instant openAt, Instant closeAt) {
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setMaxAttempt(1);
        exam.setExamTimeDurationSecond(examTimeDurationSecond);
        exam.setOpenAt(openAt);
        exam.setCloseAt(closeAt);
        return exam;
    }

    private ExamSchedule schedule(ExamScheduleStatus status, Instant from, Instant to) {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(examId);
        schedule.setSchoolRoomId(roomId);
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }

    /** Ca thi đã có giám khảo và toàn bộ học sinh đã được xếp ca + gán đề. */
    private void givenScheduleReady(ExamSchedule schedule) {
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examCandidateRepository.findByExamId(examId)).thenReturn(List.of(assignedCandidate(schedule.getId())));
    }

    private ExamCandidate assignedCandidate(UUID scheduleId) {
        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setScheduleId(scheduleId);
        candidate.setAssignedPaperId(UUID.randomUUID());
        return candidate;
    }
}
