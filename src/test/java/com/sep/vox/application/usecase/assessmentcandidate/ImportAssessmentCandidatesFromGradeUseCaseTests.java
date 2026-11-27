package com.sep.vox.application.usecase.assessmentcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentcandidate.ImportAssessmentCandidatesFromGradeCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService.AssessmentDirectoryScope;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ImportAssessmentCandidatesFromGradeUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

class ImportAssessmentCandidatesFromGradeUseCaseTests {

    private static final int MAX_CLASSES = 200;
    private static final int MAX_ROSTER = 1000;

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private ImportAssessmentCandidatesFromGradeUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();
    private final UUID class1Id = UUID.randomUUID();
    private final UUID class2Id = UUID.randomUUID();

    private final UUID activeStudent = UUID.randomUUID();
    private final UUID sharedStudent = UUID.randomUUID();
    private final UUID student2 = UUID.randomUUID();
    private final UUID existingStudent = UUID.randomUUID();
    private final UUID inactiveStudent = UUID.randomUUID();
    private final UUID teacherUser = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        assessmentDirectoryAccessService = mock(AssessmentDirectoryAccessService.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        useCase = new ImportAssessmentCandidatesFromGradeUseCase(
            assessmentRepository, assessmentCandidateRepository, schoolGradeRepository,
            schoolClassRepository, schoolClassUserRepository, userRoleQueryRepository,
            assessmentDirectoryAccessService, classTestTokenQuotaGuardService);

        Assessment assessment = assessment();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentDirectoryAccessService.resolve(assessment))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(gradeLevelId, schoolId)));
    }

    @Test
    void should_reject_grade_import_for_class_test_chair() {
        // Importing by school year gathers every class of the school year -- beyond the scope of the CHAIR of a class test.
        when(assessmentDirectoryAccessService.resolve(any(Assessment.class)))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, false));

        assertThatThrownBy(() -> useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("A class test does not support importing candidates by school year");
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_import_new_active_students_across_classes_and_dedupe() {
        when(schoolClassRepository.findBySchoolId(schoolId, null, null, null, gradeId, 1, MAX_CLASSES))
            .thenReturn(new PageResult<>(List.of(schoolClass(class1Id), schoolClass(class2Id)), 1, MAX_CLASSES, 2, 1));
        when(schoolClassUserRepository.findBySchoolClassId(class1Id, 1, MAX_ROSTER)).thenReturn(new PageResult<>(List.of(
            classUser(activeStudent, class1Id, true),
            classUser(sharedStudent, class1Id, true),
            classUser(existingStudent, class1Id, true),
            classUser(inactiveStudent, class1Id, false),
            classUser(teacherUser, class1Id, true)
        ), 0, MAX_ROSTER, 5, 1));
        when(schoolClassUserRepository.findBySchoolClassId(class2Id, 1, MAX_ROSTER)).thenReturn(new PageResult<>(List.of(
            classUser(sharedStudent, class2Id, true),
            classUser(student2, class2Id, true)
        ), 0, MAX_ROSTER, 2, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent, sharedStudent, student2, existingStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of(existingStudent));
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        List<AssessmentCandidateDto> result = useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId));

        Set<UUID> studentIds = result.stream().map(c -> c.studentId()).collect(Collectors.toSet());
        assertThat(studentIds).containsExactlyInAnyOrder(activeStudent, sharedStudent, student2);
    }

    @Test
    void should_recheck_token_quota_when_assessment_already_scheduled() {
        Assessment scheduledAssessment = assessment();
        scheduledAssessment.setStatus(AssessmentStatus.SCHEDULED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(scheduledAssessment));
        when(assessmentDirectoryAccessService.resolve(scheduledAssessment))
            .thenReturn(new AssessmentDirectoryScope(userId, schoolId, true));
        when(schoolClassRepository.findBySchoolId(schoolId, null, null, null, gradeId, 1, MAX_CLASSES))
            .thenReturn(new PageResult<>(List.of(schoolClass(class1Id)), 1, MAX_CLASSES, 1, 1));
        when(schoolClassUserRepository.findBySchoolClassId(class1Id, 1, MAX_ROSTER)).thenReturn(new PageResult<>(List.of(
            classUser(activeStudent, class1Id, true)
        ), 0, MAX_ROSTER, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of());
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId));

        // Importing by grade into an ALREADY published assessment is the strongest way to add candidates
        // -- the quota must be re-checked right after saving, exactly like
        // AddAssessmentCandidateUseCase/ImportAssessmentCandidatesFromClassUseCase.
        verify(classTestTokenQuotaGuardService).requireWithinTokenQuota(scheduledAssessment);
    }

    @Test
    void should_not_recheck_token_quota_when_assessment_still_draft() {
        when(schoolClassRepository.findBySchoolId(schoolId, null, null, null, gradeId, 1, MAX_CLASSES))
            .thenReturn(new PageResult<>(List.of(schoolClass(class1Id)), 1, MAX_CLASSES, 1, 1));
        when(schoolClassUserRepository.findBySchoolClassId(class1Id, 1, MAX_ROSTER)).thenReturn(new PageResult<>(List.of(
            classUser(activeStudent, class1Id, true)
        ), 0, MAX_ROSTER, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of());
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId));

        verify(classTestTokenQuotaGuardService, never()).requireWithinTokenQuota(any());
    }

    @Test
    void should_throw_when_grade_not_found() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId)))
            .isInstanceOf(com.sep.vox.application.exception.ResourceNotFoundException.class);
    }

    @Test
    void should_throw_when_grade_belongs_to_other_school() {
        when(schoolGradeRepository.findById(gradeId))
            .thenReturn(Optional.of(grade(gradeLevelId, UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_return_empty_when_nothing_to_import() {
        when(schoolClassRepository.findBySchoolId(schoolId, null, null, null, gradeId, 1, MAX_CLASSES))
            .thenReturn(new PageResult<>(List.of(schoolClass(class1Id)), 1, MAX_CLASSES, 1, 1));
        when(schoolClassUserRepository.findBySchoolClassId(class1Id, 1, MAX_ROSTER)).thenReturn(new PageResult<>(List.of(
            classUser(existingStudent, class1Id, true)
        ), 0, MAX_ROSTER, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(existingStudent));
        when(assessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId)).thenReturn(Set.of(existingStudent));

        List<AssessmentCandidateDto> result = useCase.execute(new ImportAssessmentCandidatesFromGradeCommand(assessmentId, gradeId));

        assertThat(result).isEmpty();
        verify(assessmentCandidateRepository, never()).saveAll(any());
    }

    private SchoolClassUser classUser(UUID studentId, UUID classId, boolean active) {
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

    private SchoolClass schoolClass(UUID id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        return schoolClass;
    }

    private SchoolGrade grade(UUID levelId, UUID ownerSchoolId) {
        SchoolGrade grade = new SchoolGrade();
        grade.setId(gradeId);
        grade.setGradeLevelId(levelId);
        grade.setSchoolId(ownerSchoolId);
        return grade;
    }


    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }
}
