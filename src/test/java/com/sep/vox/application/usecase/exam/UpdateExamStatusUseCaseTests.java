package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

/**
 * Tập trung vào guard khung giờ ca thi ở action SCHEDULE. validatePlanLimits chạy trước nên phải
 * stub đủ subscription/plan/quota để đi qua được.
 */
class UpdateExamStatusUseCaseTests {

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private SubscriptionQuotaRepository subscriptionQuotaRepository;
    private UserContextPort userContextPort;
    private UpdateExamStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();

    private final Instant open = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        subscriptionQuotaRepository = mock(SubscriptionQuotaRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new UpdateExamStatusUseCase(
            examRepository,
            examMemberRepository,
            mock(ExamPaperRepository.class),
            examScheduleRepository,
            examCandidateRepository,
            mock(ExamSessionRepository.class),
            mock(ExamCandidateResultRepository.class),
            mock(AssessmentPolicyRepository.class),
            mock(ExamCandidateResultFinalizationService.class),
            mock(ZeroScoreExamResultService.class),
            mock(SchoolUserRepository.class),
            mock(UserRoleQueryRepository.class),
            mock(ExamQuestionSecureLockService.class),
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            subscriptionQuotaRepository,
            userContextPort,
            mock(ClassTestGradingAssignmentService.class));

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

    @Test
    void should_publish_class_test_schedules_when_window_valid() {
        var exam = classTest(3600, open, open.plus(2, ChronoUnit.HOURS));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var schedule = schedule(ExamScheduleStatus.DRAFT, open, open.plus(2, ChronoUnit.HOURS));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(schedule));

        var result = useCase.execute(new UpdateExamStatusCommand(examId, "SCHEDULE", null));

        assertThat(result).isNotNull();
        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        verify(examScheduleRepository).save(schedule);
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

        var quota = mock(SubscriptionQuota.class);
        when(quota.getTotalAllocated()).thenReturn(Integer.MAX_VALUE);
        when(quota.getUsedQuantity()).thenReturn(0);
        when(subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, QuotaType.GRADING))
            .thenReturn(Optional.of(quota));
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
        schedule.setStartDate(from);
        schedule.setEndDate(to);
        schedule.setStatus(status);
        return schedule;
    }
}
