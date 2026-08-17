package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.ExamAssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleRoomValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
import com.sep.vox.application.port.input.usecase.exam.CreateClassTestUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.support.SubscriptionPeriodGuards;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

/**
 * Bài trên lớp KHÔNG gắn assessment policy thì không sinh được kết quả nào
 * ({@code ExamSessionResultCalculator} ném ngay), tức là không có gì để chấm. Nên
 * policy phải được chốt ngay lúc tạo, không để trôi sang bước sửa.
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
    private CreateClassTestUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        userContextPort = mock(UserContextPort.class);
        var examRepository = mock(ExamRepository.class);
        var examScheduleRepository = mock(ExamScheduleRepository.class);
        var examCandidateRepository = mock(ExamCandidateRepository.class);

        useCase = new CreateClassTestUseCase(
            schoolClassRepository,
            schoolClassUserRepository,
            userRoleQueryRepository,
            examRepository,
            examScheduleRepository,
            mock(ExamScheduleProctorRepository.class),
            mock(ExamMemberRepository.class),
            examCandidateRepository,
            new ExamAssessmentPolicyValidator(assessmentPolicyRepository),
            new ExamStreamConfigResolver(),
            mock(ExamScheduleRoomValidator.class),
            mock(ExamScheduleProctorConflictValidator.class),
            SubscriptionPeriodGuards.alwaysWithinPeriod(),
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(TEACHER_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), TEACHER_ID, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass()));
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(TEACHER_ID, CLASS_ID))
            .thenReturn(Optional.of(new SchoolClassUser(TEACHER_ID, CLASS_ID, true, Instant.now(), Instant.now(), TEACHER_ID)));

        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam exam = inv.getArgument(0);
            exam.setId(UUID.randomUUID());
            return exam;
        });
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> {
            ExamSchedule schedule = inv.getArgument(0);
            schedule.setId(UUID.randomUUID());
            return schedule;
        });
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
        when(schoolClassUserRepository.findBySchoolClassId(CLASS_ID, 1, 2000))
            .thenReturn(new PageResult<>(List.of(), 1, 2000, 0, 0));
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

    /** Policy hệ thống ({@code schoolId = null}) dùng được cho mọi trường. */
    @Test
    void should_accept_system_wide_assessment_policy() {
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy(null, AssessmentPolicyStatus.PUBLISHED)));

        var result = useCase.execute(command(POLICY_ID));

        assertThat(result.exam().assessmentPolicyId()).isEqualTo(POLICY_ID);
    }

    private CreateClassTestCommand command(UUID assessmentPolicyId) {
        return new CreateClassTestCommand(
            CLASS_ID,
            "Kiểm tra 15 phút",
            null,
            "2026-08-10T08:00:00Z",
            "2026-08-10T09:00:00Z",
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
        var policy = new AssessmentPolicy();
        policy.setId(POLICY_ID);
        policy.setSchoolId(schoolId);
        policy.setStatus(status);
        return policy;
    }
}
