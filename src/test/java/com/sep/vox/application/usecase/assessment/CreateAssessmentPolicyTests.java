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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.CreateAssessmentCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.assessment.CreateAssessmentUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Kỳ thi tập trung trước đây nhận thẳng {@code assessmentPolicyId} không kiểm gì — bài chạy xong mới
 * lộ ra là không sinh được kết quả nào để chấm, và policy của trường khác cũng gắn được. Bỏ trống vẫn
 * hợp lệ ở bước tạo (chọn sau ở bước sửa), nhưng đã gửi lên thì phải đúng.
 */
class CreateAssessmentPolicyTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID OTHER_SCHOOL_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private AssessmentRepository assessmentRepository;
    private SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private CreateAssessmentUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        subscriptionPeriodGuardService = mock(SubscriptionPeriodGuardService.class);
        SchoolUserRepository schoolUserRepository = mock(SchoolUserRepository.class);
        UserRoleQueryRepository userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        useCase = new CreateAssessmentUseCase(
            assessmentRepository,
            mock(AssessmentBlueprintRepository.class),
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort,
            new AssessmentStreamConfigResolver(),
            new AssessmentPolicyValidator(assessmentPolicyRepository),
            subscriptionPeriodGuardService
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(ADMIN_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), ADMIN_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment assessment = inv.getArgument(0);
            assessment.setId(UUID.randomUUID());
            return assessment;
        });
    }

    @Test
    void should_reject_when_assessment_policy_not_found() {
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Không tìm thấy bộ tiêu chí đánh giá");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_when_assessment_policy_is_not_published() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(SCHOOL_ID, AssessmentPolicyStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chỉ được dùng bộ tiêu chí đã xuất bản");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_when_assessment_policy_belongs_to_another_school() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(OTHER_SCHOOL_ID, AssessmentPolicyStatus.PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Bộ tiêu chí không thuộc trường của bạn");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_accept_published_assessment_policy_of_same_school() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(SCHOOL_ID, AssessmentPolicyStatus.PUBLISHED)));

        AssessmentDto result = useCase.execute(command(POLICY_ID));

        assertThat(result.assessmentPolicyId()).isEqualTo(POLICY_ID);
    }

    /**
     * Leaving it empty still allows creation: a centralized assessment picks its policy at the edit step.
     */
    @Test
    void should_accept_missing_assessment_policy_at_creation() {
        AssessmentDto result = useCase.execute(command(null));

        assertThat(result).isNotNull();
        assertThat(result.assessmentPolicyId()).isNull();
    }

    @Test
    void should_reject_missing_open_at_and_close_at() {
        assertThatThrownBy(() -> useCase.execute(command(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Kỳ thi phải có thời gian mở bài và đóng bài");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_when_window_falls_outside_subscription_period() {
        doThrow(new IllegalArgumentException("Thời gian đóng bài phải nằm trong hạn gói dịch vụ của trường"))
            .when(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(command(null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hạn gói dịch vụ");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_check_subscription_period_against_school_of_the_admin() {
        useCase.execute(command(null));

        verify(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(
            SCHOOL_ID,
            Instant.parse("2026-08-10T08:00:00Z"),
            Instant.parse("2026-08-10T10:00:00Z"));
    }

    @Test
    void should_reject_when_only_open_at_is_provided() {
        assertThatThrownBy(() -> useCase.execute(command(null, Instant.parse("2026-08-10T08:00:00Z"), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Kỳ thi phải có thời gian mở bài và đóng bài");
        verify(assessmentRepository, never()).save(any());
    }

    private CreateAssessmentCommand command(UUID assessmentPolicyId) {
        return command(
            assessmentPolicyId, Instant.parse("2026-08-10T08:00:00Z"), Instant.parse("2026-08-10T10:00:00Z"));
    }

    private CreateAssessmentCommand command(UUID assessmentPolicyId, Instant openAt, Instant closeAt) {
        return new CreateAssessmentCommand(
            "EX-001",
            "Kỳ thi cuối kỳ",
            null,
            LANGUAGE_ID,
            null,
            openAt,
            closeAt,
            assessmentPolicyId,
            1,
            600,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private AssessmentPolicy policy(UUID schoolId, AssessmentPolicyStatus status) {
        AssessmentPolicy policy = new AssessmentPolicy();
        policy.setId(POLICY_ID);
        policy.setSchoolId(schoolId);
        policy.setStatus(status);
        return policy;
    }
}
