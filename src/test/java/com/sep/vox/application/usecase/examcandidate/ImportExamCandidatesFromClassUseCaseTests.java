package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.ImportExamCandidatesFromClassCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.ImportExamCandidatesFromClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class ImportExamCandidatesFromClassUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ImportExamCandidatesFromClassUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    private final UUID activeStudent = UUID.randomUUID();
    private final UUID existingStudent = UUID.randomUUID();
    private final UUID inactiveStudent = UUID.randomUUID();
    private final UUID teacherUser = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ImportExamCandidatesFromClassUseCase(
            examRepository, examCandidateRepository, schoolClassRepository, schoolClassUserRepository,
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass()));
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
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of(existingStudent));
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        var result = useCase.execute(new ImportExamCandidatesFromClassCommand(examId, classId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(activeStudent);
    }

    @Test
    void should_return_empty_when_nothing_to_import() {
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000)).thenReturn(new PageResult<>(List.of(
            classUser(existingStudent, true)
        ), 0, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(existingStudent));
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of(existingStudent));

        var result = useCase.execute(new ImportExamCandidatesFromClassCommand(examId, classId));

        assertThat(result).isEmpty();
    }

    private SchoolClassUser classUser(UUID studentId, boolean active) {
        return new SchoolClassUser(UUID.randomUUID(), studentId, classId, active, Instant.now(), null, userId);
    }

    private SchoolClass schoolClass() {
        var schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setSchoolId(schoolId);
        return schoolClass;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
