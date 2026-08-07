package com.sep.vox.application.usecase.examcandidate;

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
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.ImportExamCandidatesFromClassCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService.ExamDirectoryScope;
import com.sep.vox.application.port.input.usecase.examcandidate.ImportExamCandidatesFromClassUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

class ImportExamCandidatesFromClassUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private ExamDirectoryAccessService examDirectoryAccessService;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
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
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        examDirectoryAccessService = mock(ExamDirectoryAccessService.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        useCase = new ImportExamCandidatesFromClassUseCase(
            examRepository, examCandidateRepository, schoolClassRepository, schoolClassUserRepository,
            userRoleQueryRepository, examDirectoryAccessService, classTestTokenQuotaGuardService);

        var exam = exam();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examDirectoryAccessService.resolve(exam))
            .thenReturn(new ExamDirectoryScope(userId, schoolId, true));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass()));
    }

    @Test
    void should_reject_class_test_chair_importing_a_class_they_do_not_teach() {
        when(examDirectoryAccessService.resolve(any(Exam.class)))
            .thenReturn(new ExamDirectoryScope(userId, schoolId, false));
        when(examDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> useCase.execute(new ImportExamCandidatesFromClassCommand(examId, classId)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Bạn không phụ trách lớp học này");
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_allow_class_test_chair_importing_their_own_class() {
        when(examDirectoryAccessService.resolve(any(Exam.class)))
            .thenReturn(new ExamDirectoryScope(userId, schoolId, false));
        when(examDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(classId));
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000))
            .thenReturn(new PageResult<>(List.of(classUser(activeStudent, true)), 1, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of());
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        var result = useCase.execute(new ImportExamCandidatesFromClassCommand(examId, classId));

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
    void should_reject_importing_class_into_scheduled_class_test_when_token_quota_exceeded() {
        var exam = scheduledClassTest();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examDirectoryAccessService.resolve(exam)).thenReturn(new ExamDirectoryScope(userId, schoolId, true));
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000))
            .thenReturn(new PageResult<>(List.of(classUser(activeStudent, true)), 1, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of());
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });
        doThrow(new PlanLimitExceededException("Đã vượt quá hạn mức"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(exam);

        assertThatThrownBy(() -> useCase.execute(new ImportExamCandidatesFromClassCommand(examId, classId)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("vượt quá hạn mức");
    }

    @Test
    void should_not_check_quota_when_importing_into_draft_class_test() {
        // Chưa publish thì publish sau này (UpdateExamStatusUseCase) sẽ tự soi với số thí sinh cuối cùng.
        var exam = exam();
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examDirectoryAccessService.resolve(exam)).thenReturn(new ExamDirectoryScope(userId, schoolId, true));
        when(schoolClassUserRepository.findBySchoolClassId(classId, 1, 1000))
            .thenReturn(new PageResult<>(List.of(classUser(activeStudent, true)), 1, 1000, 1, 1));
        when(userRoleQueryRepository.findUserIdsByRoleCode(anyCollection(), eq(SchoolRoleCodes.STUDENT)))
            .thenReturn(Set.of(activeStudent));
        when(examCandidateRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of());
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return arg.stream().peek(c -> c.setId(UUID.randomUUID())).toList();
        });

        useCase.execute(new ImportExamCandidatesFromClassCommand(examId, classId));

        verifyNoInteractions(classTestTokenQuotaGuardService);
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

    private Exam scheduledClassTest() {
        var exam = exam();
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.SCHEDULED);
        return exam;
    }
}
