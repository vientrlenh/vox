package com.sep.vox.application.usecase.assessmentcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.command.assessmentcandidate.ImportAssessmentCandidatesFromClassCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService.AssessmentDirectoryScope;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ImportAssessmentCandidatesFromClassUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

class ImportAssessmentCandidatesFromClassUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private ImportAssessmentCandidatesFromClassUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    private final UUID activeStudent = UUID.randomUUID();
    private final UUID existingStudent = UUID.randomUUID();
    private final UUID inactiveStudent = UUID.randomUUID();
    private final UUID teacherUser = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        assessmentDirectoryAccessService = mock(AssessmentDirectoryAccessService.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        useCase = new ImportAssessmentCandidatesFromClassUseCase(
            assessmentRepository, assessmentCandidateRepository, schoolClassRepository, schoolClassUserRepository,
            userRoleQueryRepository, assessmentDirectoryAccessService, classTestTokenQuotaGuardService);

        Assessment assessment = assessment();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass()));
    }

    @Test
    void should_reject_class_test_chair_importing_a_class_they_do_not_teach() {
        when(assessmentDirectoryAccessService.resolve(any(Assessment.class)))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, false));
        when(assessmentDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> useCase.execute(new ImportAssessmentCandidatesFromClassCommand(assessmentId, classId)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("You are not in charge of this class");
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_allow_class_test_chair_importing_their_own_class() {
        when(assessmentDirectoryAccessService.resolve(any(Assessment.class)))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, false));
        when(assessmentDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(classId));
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000))
            .thenReturn(new PageResult<>(List.of(classUser(activeStudent, true)), 1, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of());
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        List<AssessmentCandidateDto> result = useCase.execute(new ImportAssessmentCandidatesFromClassCommand(assessmentId, classId));

        assertThat(result).hasSize(1);
    }

    @Test
    void should_import_only_new_active_students_and_skip_existing() {
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000)).thenReturn(new PageResult<>(List.of(
            classUser(activeStudent, true),
            classUser(existingStudent, true),
            classUser(inactiveStudent, false),
            classUser(teacherUser, true)
        ), 0, 1000, 4, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent, existingStudent, inactiveStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of(existingStudent));
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        List<AssessmentCandidateDto> result = useCase.execute(new ImportAssessmentCandidatesFromClassCommand(assessmentId, classId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(activeStudent);
    }

    @Test
    void should_reject_importing_class_into_scheduled_class_test_when_token_quota_exceeded() {
        Assessment assessment = scheduledClassTest();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment)).thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000))
            .thenReturn(new PageResult<>(List.of(classUser(activeStudent, true)), 1, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of());
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });
        doThrow(new BusinessRuleViolationException("The quota has been exceeded"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(assessment);

        assertThatThrownBy(() -> useCase.execute(new ImportAssessmentCandidatesFromClassCommand(assessmentId, classId)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("quota has been exceeded");
    }

    @Test
    void should_not_check_quota_when_importing_into_draft_class_test() {
        // Not published yet, so the later publish (UpdateAssessmentStatusUseCase) will check against the
        // final number of candidates.
        Assessment assessment = assessment();
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.DRAFT);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment)).thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000))
            .thenReturn(new PageResult<>(List.of(classUser(activeStudent, true)), 1, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of());
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        useCase.execute(new ImportAssessmentCandidatesFromClassCommand(assessmentId, classId));

        verifyNoInteractions(classTestTokenQuotaGuardService);
    }

    @Test
    void should_return_empty_when_nothing_to_import() {
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000)).thenReturn(new PageResult<>(List.of(
            classUser(existingStudent, true)
        ), 0, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(existingStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of(existingStudent));

        List<AssessmentCandidateDto> result = useCase.execute(new ImportAssessmentCandidatesFromClassCommand(assessmentId, classId));

        assertThat(result).isEmpty();
    }

    private SchoolClassUser classUser(UUID studentId, boolean active) {
        return SchoolClassUser.builder()
            .id(UUID.randomUUID())
            .userId(studentId)
            .schoolClassId(classId)
            .active(active)
            .joinedAt(Instant.now())
            .leftAt(null)
            .assignedBy(userId)
            .build();
    }

    private SchoolClass schoolClass() {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setSchoolId(schoolId);
        return schoolClass;
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
