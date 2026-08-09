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
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
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
            mock(com.sep.vox.application.port.output.EventPublisherPort.class),
            mock(ClassTestGradingAssignmentService.class),
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
        // Khung mở/đóng 30 phút, thời gian làm bài đã tính ra 60 phút.
        var exam = classTest(3600, open, open.plus(30, ChronoUnit.MINUTES));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("thời gian đóng bài");
        verify(examScheduleRepository, never()).save(any());
    }

    /**
     * Lên lịch bài trên lớp KHÔNG còn tự công bố ca thi: ca phải đã được công bố tay từ trước
     * (UpdateExamScheduleStatusUseCase), và action này không ghi gì lên ca — giống kỳ thi tập trung.
     */
    @Test
    void should_schedule_class_test_when_all_schedules_published() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        givenScheduleReady(schedule);

        var result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository, never()).save(any());
    }

    /** Bản song sinh của should_reject_schedule_when_centralized_exam_has_draft_schedule. */
    @Test
    void should_reject_schedule_when_class_test_has_draft_schedule() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
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
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
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
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        schedule.setSchoolRoomId(null);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ca thi chưa được chọn phòng");
    }

    @Test
    void should_reject_schedule_when_class_test_schedule_has_no_proctor() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ca thi chưa có giám khảo");
    }

    @Test
    void should_reject_schedule_when_class_test_candidate_has_no_schedule() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        var unassigned = assignedCandidate(schedule.getId());
        unassigned.setScheduleId(null);
        when(examCandidateRepository.findByExamId(examId)).thenReturn(List.of(unassigned));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Còn 1 học sinh chưa được xếp vào ca thi");
    }

    @Test
    void should_reject_schedule_when_class_test_candidate_has_no_paper() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        var withoutPaper = assignedCandidate(schedule.getId());
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
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
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
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa có mã đề");
    }

    /**
     * Ca thi còn DRAFT thì học sinh và giám thị chưa nhìn thấy — kỳ thi "đã lên lịch" mà có ca
     * không ai vào được. Công bố từng ca là thao tác riêng nên phải làm xong trước khi lên lịch.
     */
    @Test
    void should_reject_schedule_when_centralized_exam_has_draft_schedule() {
        givenCentralizedExam();
        var draft = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        var published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
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
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(schedule.getId())).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(new ExamPaper()));

        var result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result.status()).isEqualTo(ExamStatus.SCHEDULED.name());
    }

    /**
     * Trạng thái phải được kiểm trước hạn mức gói: bấm SCHEDULE trên bài không còn DRAFT mà báo lỗi
     * "vượt quá giới hạn gói" thì người dùng đi sửa nhầm chỗ.
     */
    @Test
    void should_report_invalid_status_before_plan_limit_when_exam_not_draft() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ cho action này");
    }

    @Test
    void should_reject_cancel_when_exam_already_published_results() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
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
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.CANCELLED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ cho action này");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_allow_cancel_from_draft() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        var result = useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(result).isNotNull();
        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CANCELLED);
    }

    @Test
    void should_allow_cancel_from_in_progress() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CANCELLED);
    }

    /**
     * Đóng bài mà bỏ ca lại là ca ở PUBLISHED vĩnh viễn — không đường nào chuyển nó sang COMPLETED
     * trừ khi có người bấm tay từng ca.
     */
    @Test
    void should_close_schedules_when_exam_is_closed() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        // Ca đã kết thúc từ hôm qua và ca của tuần sau chưa tới giờ.
        var ended = schedule(ExamScheduleStatus.PUBLISHED,
            open.minus(1, ChronoUnit.DAYS), open.minus(22, ChronoUnit.HOURS));
        var notStarted = schedule(ExamScheduleStatus.PUBLISHED,
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
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        var ongoing = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CLOSE", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("2 học sinh đang làm bài");
        assertThat(ongoing.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository, never()).save(any());
    }

    /** Cả lớp nộp xong thì vẫn phải đóng sớm được — đây là lý do không chặn cứng theo khung giờ. */
    @Test
    void should_allow_close_while_schedule_is_ongoing_but_nobody_is_working() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.IN_PROGRESS);
        var ongoing = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamIdAndInSchedule(any(), any())).thenReturn(List.of(ongoing));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(ongoing));
        when(examSessionRepository.countActiveByExamId(examId)).thenReturn(0L);

        useCase.execute(new UpdateExamStatusCommand(examId, "CLOSE", null));

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CLOSED);
        assertThat(ongoing.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
    }

    /**
     * Ca thi còn PUBLISHED thì vẫn lọt qua isVisibleToStudent()/allowsAttendance() và các truy vấn
     * hard-code status = 'PUBLISHED' — huỷ kỳ thi mà không kéo theo ca là để lại ca "sẵn sàng".
     */
    @Test
    void should_cancel_schedules_when_exam_is_cancelled() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.SCHEDULED);
        var draft = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        var published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
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
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.CLOSED);
        var completed = schedule(ExamScheduleStatus.COMPLETED, open, open.plus(2, ChronoUnit.HOURS));
        var moved = schedule(ExamScheduleStatus.MOVED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(completed, moved));

        useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null));

        assertThat(completed.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
        assertThat(moved.getStatus()).isEqualTo(ExamScheduleStatus.MOVED);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_not_change_schedules_when_cancel_transition_is_rejected() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setStatus(ExamStatus.RESULTS_PUBLISHED);
        var published = schedule(ExamScheduleStatus.PUBLISHED, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(published));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "CANCEL", null)))
            .isInstanceOf(IllegalStateException.class);
        assertThat(published.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository, never()).save(any());
    }

    /**
     * Kỳ thi tập trung dựng ở DRAFT, người gọi là SCHOOL_ADMIN cùng trường (authorizeMutation của
     * nhánh CENTRALIZED không xét exam member), và mặc định đã đủ thí sinh + mã đề — từng test tự làm
     * hỏng đúng một điều kiện.
     */
    private void givenCentralizedExam() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        exam.setKind(ExamKind.CENTRALIZED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        var schoolUser = mock(com.sep.vox.domain.model.school.SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(schoolId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new com.sep.vox.application.query.dto.UserRoleInfo(
                UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));

        when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(new ExamPaper()));
    }

    private void givenGenerousPlan() {
        var subscription = mock(SchoolSubscription.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getPlanId()).thenReturn(planId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));

        var plan = mock(SubscriptionPlan.class);
        when(plan.getMaxStudentCount()).thenReturn(null);
        when(plan.getMaxTimePerAttemptMin()).thenReturn(null);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
        // classTestTokenQuotaGuardService is a plain mock: requireWithinTokenQuota() no-ops by default,
        // i.e. "đủ hạn mức". Chi tiết tính toán GRADING/CLASS_TEST/hạn mức cá nhân được test riêng ở
        // ClassTestTokenQuotaGuardServiceTests; ở đây chỉ cần xác nhận UpdateExamStatusUseCase có gọi
        // guard và tôn trọng exception mà guard ném ra (xem should_reject_schedule_when_token_quota_exceeded).
    }

    private Exam classTest(Integer examTimeDurationSecond, Instant openAt, Instant closeAt) {
        var exam = new Exam();
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
        var schedule = new ExamSchedule();
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
        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setScheduleId(scheduleId);
        candidate.setAssignedPaperId(UUID.randomUUID());
        return candidate;
    }
}
