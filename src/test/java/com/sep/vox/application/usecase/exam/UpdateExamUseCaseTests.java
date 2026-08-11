package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.UpdateExamCommand;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ExamAssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateExamUseCaseTests {

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamScheduleRepository examScheduleRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private UpdateExamUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private final Instant open = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();
    private final Instant close = OffsetDateTime.parse("2026-07-10T11:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        useCase = new UpdateExamUseCase(
            examRepository, examMemberRepository, examScheduleRepository,
            schoolUserRepository, userRoleQueryRepository,
            new ExamAssessmentPolicyValidator(mock(AssessmentPolicyRepository.class)),
            new ExamStreamConfigResolver(),
            userContextPort,
            classTestTokenQuotaGuardService);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.existsSubmittedSessionByExamId(examId)).thenReturn(false);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- CLASS_TEST: khung mở/đóng mới bị đồng bộ xuống ca thi, phải đủ dài ---

    @Test
    void should_reject_class_test_window_shorter_than_exam_time() {
        var exam = classTest(3 * 3600);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // Thu khung xuống 1 tiếng trong khi thời gian làm bài là 3 tiếng.
        assertThatThrownBy(() -> useCase.execute(command(open.toString(), open.plus(1, ChronoUnit.HOURS).toString())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("thời gian đóng bài");
        verify(examScheduleRepository, never()).save(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_allow_renaming_class_test_without_revalidating_window() {
        // Guard chỉ được chạy khi thao tác thực sự đụng tới thời gian; đổi tên trên một bài đang có
        // khung quá ngắn không được phép bị chặn (nhất quán với nhánh kỳ thi thường).
        var exam = classTest(3 * 3600);
        exam.setOpenAt(open);
        exam.setCloseAt(open.plus(1, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, "Tên mới", null, null, null, null, null, null, null, null, null, null));

        verify(examRepository).save(exam);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_sync_class_test_schedules_when_window_valid() {
        var exam = classTest(3600);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.PUBLISHED, open, close);
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));

        useCase.execute(command(open.toString(), close.plus(1, ChronoUnit.HOURS).toString()));

        assertThat(schedule.getStartDate()).isEqualTo(open);
        assertThat(schedule.getEndDate()).isEqualTo(close.plus(1, ChronoUnit.HOURS));
        verify(examScheduleRepository).save(schedule);
    }

    // --- Bài trên lớp đã publish: sửa duration/maxAttempt phải soi lại token quota ---

    @Test
    void should_reject_editing_scheduled_class_test_when_token_quota_exceeded() {
        var exam = classTest(3600);
        exam.setStatus(ExamStatus.SCHEDULED);
        exam.setOpenAt(open);
        exam.setCloseAt(close);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        doThrow(new PlanLimitExceededException("Đã vượt quá hạn mức"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(exam);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamCommand(
                examId, null, null, null, null, null, null, 2 * 3600, null, null, null, null)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("vượt quá hạn mức");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_not_revalidate_quota_when_editing_draft_class_test() {
        // Chưa publish thì chưa có ước lượng cũ nào cần soi lại -- publish sau này sẽ tự kiểm.
        var exam = classTest(3 * 3600);
        exam.setOpenAt(open);
        exam.setCloseAt(open.plus(6, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, null, null, null, null, null, null, 2 * 3600, null, null, null, null));

        verifyNoInteractions(classTestTokenQuotaGuardService);
        verify(examRepository).save(exam);
    }

    @Test
    void should_not_revalidate_quota_when_scheduled_class_test_edit_does_not_touch_duration_or_attempt() {
        var exam = classTest(3600);
        exam.setStatus(ExamStatus.SCHEDULED);
        exam.setOpenAt(open);
        exam.setCloseAt(close);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, "Tên mới", null, null, null, null, null, null, null, null, null, null));

        verifyNoInteractions(classTestTokenQuotaGuardService);
        verify(examRepository).save(exam);
    }

    // --- Kỳ thi thường: ca thi độc lập, đổi khung kỳ thi phải kiểm lại ca thi đã có ---

    @Test
    void should_reject_when_new_exam_window_leaves_schedules_outside() {
        var exam = centralized(null);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId))
            .thenReturn(List.of(schedule(ExamScheduleStatus.PUBLISHED, open, close)));

        // Dời khung kỳ thi sang ngày hôm sau, ca thi cũ rơi ra ngoài.
        assertThatThrownBy(() -> useCase.execute(
                command(open.plus(1, ChronoUnit.DAYS).toString(), close.plus(1, ChronoUnit.DAYS).toString())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("nằm ngoài");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_when_new_exam_time_duration_exceeds_existing_schedules() {
        var exam = centralized(null);
        exam.setOpenAt(open);
        exam.setCloseAt(close);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        // Ca thi dài 1 tiếng, đặt thời gian làm bài thành 2 tiếng.
        when(examScheduleRepository.findByExamId(examId))
            .thenReturn(List.of(schedule(ExamScheduleStatus.DRAFT, open, open.plus(1, ChronoUnit.HOURS))));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamCommand(
                examId, null, null, null, null, null, null, 2 * 3600, null, null, null, null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("vượt quá thời lượng");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_ignore_completed_and_cancelled_schedules() {
        var exam = centralized(null);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(ExamScheduleStatus.COMPLETED, open, close),
            schedule(ExamScheduleStatus.CANCELLED, open, close),
            schedule(ExamScheduleStatus.MOVED, open, close)));

        var result = useCase.execute(command(open.plus(1, ChronoUnit.DAYS).toString(), close.plus(1, ChronoUnit.DAYS).toString()));

        assertThat(result).isNotNull();
        verify(examRepository).save(any(Exam.class));
    }

    @Test
    void should_allow_updating_name_only_without_touching_schedules() {
        var exam = centralized(null);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, "Tên mới", null, null, null, null, null, null, null, null, null, null));

        // Không gửi field thời gian nào -> không cần truy vấn ca thi.
        verify(examScheduleRepository, never()).findByExamId(any());
        verify(examRepository).save(any(Exam.class));
    }

    // --- Kỳ thi đã bắt đầu thì khoá chỉnh sửa thông tin ---

    @Test
    void should_reject_updating_centralized_exam_after_it_started() {
        var exam = centralized(null);
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamCommand(
                examId, "Tên mới", null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã bắt đầu");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_updating_centralized_exam_after_results_published() {
        var exam = centralized(null);
        exam.setStatus(ExamStatus.RESULTS_PUBLISHED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamCommand(
                examId, "Tên mới", null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(IllegalStateException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_allow_updating_centralized_exam_while_scheduled() {
        var exam = centralized(null);
        exam.setStatus(ExamStatus.SCHEDULED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, "Tên mới", null, null, null, null, null, null, null, null, null, null));

        verify(examRepository).save(exam);
    }

    // --- Cấu hình giám sát (stream) ---

    @Test
    void should_update_stream_config_when_types_provided() {
        var exam = centralized(null);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(streamCommand(List.of("CAMERA", "SCREEN"), "ANY"));

        assertThat(exam.getRequiredStreamType()).isEqualTo(ExamRequiredStreamType.CAMERA_AND_SCREEN);
        assertThat(exam.getStreamTypePermission()).isEqualTo(ExamStreamTypePermission.ANY);
        verify(examRepository).save(exam);
    }

    @Test
    void should_keep_stream_config_when_types_null() {
        // Sửa tên không được làm mất cấu hình giám sát: null = "không đụng tới", không phải "tắt".
        var exam = centralized(null);
        exam.setRequiredStreamType(ExamRequiredStreamType.CAMERA);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, "Tên mới", null, null, null, null, null, null, null, null, null, null));

        assertThat(exam.getRequiredStreamType()).isEqualTo(ExamRequiredStreamType.CAMERA);
    }

    @Test
    void should_disable_monitoring_when_stream_types_empty() {
        var exam = centralized(null);
        exam.setRequiredStreamType(ExamRequiredStreamType.CAMERA_AND_SCREEN);
        exam.setStreamTypePermission(ExamStreamTypePermission.ALL);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(streamCommand(List.of(), null));

        assertThat(exam.getRequiredStreamType()).isNull();
        assertThat(exam.getStreamTypePermission()).isNull();
    }

    @Test
    void should_reject_permission_without_two_stream_types() {
        var exam = centralized(null);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(streamCommand(List.of("CAMERA"), "ALL")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_stream_update_after_exam_started() {
        var exam = centralized(null);
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(streamCommand(List.of("CAMERA"), null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã bắt đầu");
        verify(examRepository, never()).save(any());
    }

    // --- Chủ tịch hội đồng sửa được thông tin kỳ thi tập trung như quản trị trường ---

    @Test
    void should_allow_the_chair_to_update_a_centralized_exam() {
        var exam = centralized(null);
        exam.setName("Cũ");
        givenCallerIsChairInsteadOfSchoolAdmin();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        useCase.execute(new UpdateExamCommand(
            examId, "Mới", null, null, null, null, null, null, null, null, null, null));

        assertThat(exam.getName()).isEqualTo("Mới");
        verify(examRepository).save(exam);
    }

    @Test
    void should_reject_updating_centralized_exam_when_caller_is_neither_school_admin_nor_chair() {
        var exam = centralized(null);
        givenCallerIsChairInsteadOfSchoolAdmin();
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamCommand(
            examId, "Mới", null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
        verify(examRepository, never()).save(any());
    }

    /** Hạ người gọi từ quản trị trường xuống giáo viên chỉ giữ vai trò CHAIR của chính kỳ thi đó. */
    private void givenCallerIsChairInsteadOfSchoolAdmin() {
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
    }

    private UpdateExamCommand streamCommand(List<String> requiredStreamTypes, String streamTypePermission) {
        return new UpdateExamCommand(
            examId, null, null, null, null, null, null, null, null, null,
            requiredStreamTypes, streamTypePermission);
    }

    private UpdateExamCommand command(String openAt, String closeAt) {
        return new UpdateExamCommand(examId, null, null, openAt, closeAt, null, null, null, null, null, null, null);
    }

    private Exam classTest(Integer examTimeDurationSecond) {
        var exam = baseExam(ExamKind.CLASS_TEST, examTimeDurationSecond);
        exam.setStatus(ExamStatus.DRAFT);
        return exam;
    }

    private Exam centralized(Integer examTimeDurationSecond) {
        var exam = baseExam(ExamKind.CENTRALIZED, examTimeDurationSecond);
        exam.setStatus(ExamStatus.DRAFT);
        var schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(schoolId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        var role = new UserRoleInfo(
            UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường");
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(role));
        return exam;
    }

    private Exam baseExam(ExamKind kind, Integer examTimeDurationSecond) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(kind);
        exam.setExamTimeDurationSecond(examTimeDurationSecond);
        return exam;
    }

    private ExamSchedule schedule(ExamScheduleStatus status, Instant start, Instant end) {
        var schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(examId);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        schedule.setStatus(status);
        return schedule;
    }
}
