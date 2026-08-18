package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.CreateExamCommand;
import com.sep.vox.application.port.input.service.ExamAssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.usecase.exam.CreateExamUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Kỳ thi tập trung trước đây nhận thẳng {@code assessmentPolicyId} không kiểm gì — bài chạy xong mới
 * lộ ra là không sinh được kết quả nào để chấm, và policy của trường khác cũng gắn được. Bỏ trống vẫn
 * hợp lệ ở bước tạo (chọn sau ở bước sửa), nhưng đã gửi lên thì phải đúng.
 */
class CreateExamAssessmentPolicyTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID OTHER_SCHOOL_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private ExamRepository examRepository;
    private SchoolSubscriptionActiveGuardService schoolSubscriptionActiveGuardService;
    private CreateExamUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        examRepository = mock(ExamRepository.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        var userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        var userContextPort = mock(UserContextPort.class);
        schoolSubscriptionActiveGuardService = mock(SchoolSubscriptionActiveGuardService.class);

        useCase = new CreateExamUseCase(
            examRepository,
            mock(ExamBlueprintRepository.class),
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort,
            new ExamStreamConfigResolver(),
            new ExamAssessmentPolicyValidator(assessmentPolicyRepository),
            schoolSubscriptionActiveGuardService
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        var schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(ADMIN_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), ADMIN_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam exam = inv.getArgument(0);
            exam.setId(UUID.randomUUID());
            return exam;
        });
    }

    @Test
    void should_reject_when_school_has_no_active_subscription() {
        doThrow(new PlanLimitExceededException("Trường chưa có gói subscription đang hoạt động, không thể tạo Bài kiểm tra tập trung."))
            .when(schoolSubscriptionActiveGuardService).requireActiveForSchool(eq(SCHOOL_ID), any());

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("chưa có gói subscription đang hoạt động");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_when_assessment_policy_not_found() {
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Không tìm thấy bộ tiêu chí đánh giá");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_when_assessment_policy_is_not_published() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(SCHOOL_ID, AssessmentPolicyStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Chỉ được dùng bộ tiêu chí đã xuất bản");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_when_assessment_policy_belongs_to_another_school() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(OTHER_SCHOOL_ID, AssessmentPolicyStatus.PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Bộ tiêu chí không thuộc trường của bạn");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_accept_published_assessment_policy_of_same_school() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(SCHOOL_ID, AssessmentPolicyStatus.PUBLISHED)));

        var result = useCase.execute(command(POLICY_ID));

        assertThat(result.assessmentPolicyId()).isEqualTo(POLICY_ID);
    }

    /** Bỏ trống vẫn tạo được: kỳ thi tập trung chọn policy ở bước sửa. */
    @Test
    void should_accept_missing_assessment_policy_at_creation() {
        var result = useCase.execute(command(null));

        assertThat(result).isNotNull();
        assertThat(result.assessmentPolicyId()).isNull();
    }

    /**
     * Các bộ parse của JDK ném DateTimeParseException — không phải IllegalArgumentException — nên nếu
     * không đổi loại thì lọt xuống handler chung và ra 500 thay vì 400. {@code DateMapper.toInstant}
     * chịu trách nhiệm đổi loại đó.
     */
    @Test
    void should_reject_malformed_open_at_with_bad_request() {
        assertThatThrownBy(() -> useCase.execute(command(null, "2026-08-10 08:00", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thời gian phải kèm múi giờ");
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_reject_malformed_close_at_with_bad_request() {
        assertThatThrownBy(() -> useCase.execute(command(null, "2026-08-10T08:00:00Z", "hôm qua")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thời gian phải kèm múi giờ");
        verify(examRepository, never()).save(any());
    }

    /** Ô datetime để trống gửi lên chuỗi rỗng chứ không bỏ hẳn field — phải coi như không có. */
    @Test
    void should_treat_blank_open_at_as_absent() {
        var result = useCase.execute(command(null, "", ""));

        assertThat(result.openAt()).isNull();
        assertThat(result.closeAt()).isNull();
    }

    private CreateExamCommand command(UUID assessmentPolicyId) {
        return command(assessmentPolicyId, null, null);
    }

    private CreateExamCommand command(UUID assessmentPolicyId, String openAt, String closeAt) {
        return new CreateExamCommand(
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
        var policy = new AssessmentPolicy();
        policy.setId(POLICY_ID);
        policy.setSchoolId(schoolId);
        policy.setStatus(status);
        return policy;
    }
}
