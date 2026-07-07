package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ImportExamCandidatesFromGradeCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.ImportExamCandidatesFromGradeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class ImportExamCandidatesFromGradeUseCaseTests {

    private static final int MAX_CLASSES = 200;
    private static final int MAX_ROSTER = 1000;

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ImportExamCandidatesFromGradeUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
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
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ImportExamCandidatesFromGradeUseCase(
            examRepository, examCandidateRepository, schoolGradeRepository, schoolGradeLevelRepository,
            schoolClassRepository, schoolClassUserRepository, examMemberRepository, schoolUserRepository,
            userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(gradeLevelId)));
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(gradeLevel(schoolId)));
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
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of(existingStudent));
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        var result = useCase.execute(new ImportExamCandidatesFromGradeCommand(examId, gradeId));

        var studentIds = result.stream().map(c -> c.studentId()).collect(Collectors.toSet());
        assertThat(studentIds).containsExactlyInAnyOrder(activeStudent, sharedStudent, student2);
    }

    @Test
    void should_throw_when_grade_not_found() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ImportExamCandidatesFromGradeCommand(examId, gradeId)))
            .isInstanceOf(com.sep.vox.application.exception.NotFoundException.class);
    }

    @Test
    void should_throw_when_grade_belongs_to_other_school() {
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(gradeLevel(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(new ImportExamCandidatesFromGradeCommand(examId, gradeId)))
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
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of(existingStudent));

        var result = useCase.execute(new ImportExamCandidatesFromGradeCommand(examId, gradeId));

        assertThat(result).isEmpty();
        verify(examCandidateRepository, never()).saveAll(any());
    }

    private SchoolClassUser classUser(UUID studentId, UUID classId, boolean active) {
        return new SchoolClassUser(UUID.randomUUID(), studentId, classId, active, OffsetDateTime.now(), null, userId);
    }

    private SchoolClass schoolClass(UUID id) {
        var schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        return schoolClass;
    }

    private SchoolGrade grade(UUID levelId) {
        var grade = new SchoolGrade();
        grade.setId(gradeId);
        grade.setSchoolGradeLevelId(levelId);
        return grade;
    }

    private SchoolGradeLevel gradeLevel(UUID ownerSchoolId) {
        var level = new SchoolGradeLevel();
        level.setId(gradeLevelId);
        level.setSchoolId(ownerSchoolId);
        return level;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
