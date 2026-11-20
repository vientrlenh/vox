package com.sep.vox.application.usecase.assessmentcandidate;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.AddAssessmentCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService.AssessmentDirectoryScope;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AddAssessmentCandidateUseCase;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AddAssessmentCandidateUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private AddAssessmentCandidateUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        assessmentDirectoryAccessService = mock(AssessmentDirectoryAccessService.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        useCase = new AddAssessmentCandidateUseCase(
            assessmentRepository, assessmentCandidateRepository, schoolUserRepository,
            schoolClassUserRepository, userRoleQueryRepository, assessmentDirectoryAccessService,
            classTestTokenQuotaGuardService);

        Assessment assessment = assessment();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
    }

    @Test
    void should_add_candidate_on_happy_path() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(false);
        when(assessmentCandidateRepository.save(any(AssessmentCandidate.class))).thenAnswer(inv -> {
            AssessmentCandidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        AssessmentCandidateDto result = useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId));

        assertThat(result.studentId()).isEqualTo(studentId);
        assertThat(result.assessmentId()).isEqualTo(assessmentId);
        assertThat(result.scheduleId()).isNull();
        assertThat(result.assignedPaperId()).isNull();
        assertThat(result.status()).isEqualTo("ASSIGNED");
        verify(assessmentCandidateRepository).save(any(AssessmentCandidate.class));
    }

    @Test
    void should_reject_adding_candidate_to_scheduled_class_test_when_token_quota_exceeded() {
        Assessment assessment = scheduledClassTest();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment)).thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(false);
        when(assessmentCandidateRepository.save(any(AssessmentCandidate.class))).thenAnswer(inv -> {
            AssessmentCandidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        doThrow(new PlanLimitExceededException("Đã vượt quá hạn mức"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(assessment);

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("vượt quá hạn mức");
    }

    @Test
    void should_not_check_quota_when_adding_candidate_to_draft_class_test() {
        // Not published yet, so the later publish (UpdateAssessmentStatusUseCase) will check against the
        // final number of candidates.
        Assessment assessment = assessment();
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.DRAFT);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment)).thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(false);
        when(assessmentCandidateRepository.save(any(AssessmentCandidate.class))).thenAnswer(inv -> {
            AssessmentCandidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId));

        verifyNoInteractions(classTestTokenQuotaGuardService);
    }

    @Test
    void should_reject_when_student_not_in_school() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_when_user_is_not_student() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_when_candidate_already_exists() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId)))
            .isInstanceOf(DuplicatedException.class);
        verify(assessmentCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_class_test_chair_when_student_is_outside_their_classes() {
        givenClassTestScope();
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(assessmentDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(classId));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), List.of(classId)))
            .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Học sinh không thuộc lớp bạn phụ trách");
        verify(assessmentCandidateRepository, never()).save(any());
    }

    @Test
    void should_allow_class_test_chair_for_student_in_their_class() {
        givenClassTestScope();
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(assessmentDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(classId));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), List.of(classId)))
            .thenReturn(List.of(activeMembership()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(false);
        when(assessmentCandidateRepository.save(any(AssessmentCandidate.class))).thenAnswer(inv -> {
            AssessmentCandidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        AssessmentCandidateDto result = useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId));

        assertThat(result.studentId()).isEqualTo(studentId);
    }

    @Test
    void should_reject_class_test_chair_who_teaches_no_class() {
        givenClassTestScope();
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(assessmentDirectoryAccessService.callerClassIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentCandidateCommand(assessmentId, studentId)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentCandidateRepository, never()).save(any());
    }

    private void givenClassTestScope() {
        when(assessmentDirectoryAccessService.resolve(any(Assessment.class)))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, false));
    }

    private SchoolClassUser activeMembership() {
        SchoolClassUser membership = new SchoolClassUser();
        membership.setUserId(studentId);
        membership.setSchoolClassId(classId);
        membership.setActive(true);
        return membership;
    }

    private UserRoleInfo studentRole() {
        return new UserRoleInfo(UUID.randomUUID(), studentId, UUID.randomUUID(), Instant.now(),
            "STUDENT", "Student");
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private Assessment scheduledClassTest() {
        Assessment assessment = assessment();
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.SCHEDULED);
        return assessment;
    }
}
