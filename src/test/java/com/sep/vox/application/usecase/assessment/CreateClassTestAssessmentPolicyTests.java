package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessment.CreateClassTestResponse;
import com.sep.vox.application.support.SubscriptionPeriodGuards;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

/**
 * A class test with NO assessment policy attached cannot produce any result
 * ({@code AssessmentSessionResultCalculator} throws right away), i.e. there is nothing to grade. So the
 * policy must be fixed at creation time, not left to drift to the edit step.
 */
class CreateClassTestAssessmentPolicyTests {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID OTHER_SCHOOL_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();

    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AssessmentPolicyRepository assessmentPolicyRepository;
    private UserContextPort userContextPort;
    private SchoolSubscriptionActiveGuardService schoolSubscriptionActiveGuardService;
    private CreateClassTestUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionActiveGuardService = mock(SchoolSubscriptionActiveGuardService.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        AssessmentScheduleRepository assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        AssessmentCandidateRepository assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);

        useCase = new CreateClassTestUseCase(
            schoolClassRepository,
            schoolClassUserRepository,
            userRoleQueryRepository,
            assessmentRepository,
            assessmentScheduleRepository,
            mock(AssessmentScheduleProctorRepository.class),
            mock(AssessmentMemberRepository.class),
            assessmentCandidateRepository,
            new AssessmentPolicyValidator(assessmentPolicyRepository),
            new AssessmentStreamConfigResolver(),
            mock(AssessmentScheduleRoomValidator.class),
            mock(AssessmentScheduleProctorConflictValidator.class),
            SubscriptionPeriodGuards.alwaysWithinPeriod(),
            userContextPort,
            schoolSubscriptionActiveGuardService
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(TEACHER_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), TEACHER_ID, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass()));
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(TEACHER_ID, CLASS_ID))
            .thenReturn(Optional.of(new SchoolClassUser(TEACHER_ID, CLASS_ID, true, Instant.now(), Instant.now(), TEACHER_ID)));

        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment assessment = inv.getArgument(0);
            assessment.setId(UUID.randomUUID());
            return assessment;
        });
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> {
            AssessmentSchedule schedule = inv.getArgument(0);
            schedule.setId(UUID.randomUUID());
            return schedule;
        });
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
        when(schoolClassUserRepository.findBySchoolClassId(CLASS_ID, 1, 2000))
            .thenReturn(new PageResult<>(List.of(), 1, 2000, 0, 0));
    }

    @Test
    void should_reject_when_school_has_no_active_subscription() {
        doThrow(new PlanLimitExceededException("Trường chưa có gói subscription đang hoạt động, không thể tạo Class Test."))
            .when(schoolSubscriptionActiveGuardService).requireActiveForSchool(eq(SCHOOL_ID), any());

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("chưa có gói subscription đang hoạt động");
    }

    @Test
    void should_reject_when_assessment_policy_is_missing() {
        assertThatThrownBy(() -> useCase.execute(command(null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Bộ tiêu chí đánh giá là bắt buộc");
    }

    @Test
    void should_reject_when_assessment_policy_not_found() {
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Không tìm thấy bộ tiêu chí đánh giá");
    }

    @Test
    void should_reject_when_assessment_policy_is_not_published() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(SCHOOL_ID, AssessmentPolicyStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Chỉ được dùng bộ tiêu chí đã xuất bản");
    }

    @Test
    void should_reject_when_assessment_policy_belongs_to_another_school() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(OTHER_SCHOOL_ID, AssessmentPolicyStatus.PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(command(POLICY_ID)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Bộ tiêu chí không thuộc trường của bạn");
    }

    /** A system policy ({@code schoolId = null}) can be used by every school. */
    @Test
    void should_accept_system_wide_assessment_policy() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(null, AssessmentPolicyStatus.PUBLISHED)));

        CreateClassTestResponse result = useCase.execute(command(POLICY_ID));

        assertThat(result.assessment().assessmentPolicyId()).isEqualTo(POLICY_ID);
    }

    private CreateClassTestCommand command(UUID assessmentPolicyId) {
        return new CreateClassTestCommand(
            CLASS_ID,
            "Kiểm tra 15 phút",
            null,
            Instant.parse("2026-08-10T08:00:00Z"),
            Instant.parse("2026-08-10T09:00:00Z"),
            assessmentPolicyId,
            1,
            600,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private SchoolClass schoolClass() {
        return new SchoolClass(
            CLASS_ID,
            SCHOOL_ID,
            UUID.randomUUID(),
            UUID.randomUUID(),
            new ClassCode("10A1"),
            "10A1",
            null,
            SchoolClassStatus.ACTIVE,
            Instant.now(),
            Instant.now(),
            TEACHER_ID,
            TEACHER_ID
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
