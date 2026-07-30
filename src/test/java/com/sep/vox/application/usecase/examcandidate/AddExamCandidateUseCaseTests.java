package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.AddExamCandidateCommand;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService.ExamDirectoryScope;
import com.sep.vox.application.port.input.usecase.examcandidate.AddExamCandidateUseCase;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AddExamCandidateUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private ExamDirectoryAccessService examDirectoryAccessService;
    private AddExamCandidateUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        examDirectoryAccessService = mock(ExamDirectoryAccessService.class);
        useCase = new AddExamCandidateUseCase(
            examRepository, examCandidateRepository, schoolUserRepository,
            schoolClassUserRepository, userRoleQueryRepository, examDirectoryAccessService);

        var exam = exam();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examDirectoryAccessService.resolve(exam))
            .thenReturn(new ExamDirectoryScope(userId, schoolId, true));
    }

    @Test
    void should_add_candidate_on_happy_path() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(examCandidateRepository.existsByExamIdAndStudentId(examId, studentId)).thenReturn(false);
        when(examCandidateRepository.save(any(ExamCandidate.class))).thenAnswer(inv -> {
            ExamCandidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        var result = useCase.execute(new AddExamCandidateCommand(examId, studentId));

        assertThat(result.studentId()).isEqualTo(studentId);
        assertThat(result.examId()).isEqualTo(examId);
        assertThat(result.scheduleId()).isNull();
        assertThat(result.assignedPaperId()).isNull();
        assertThat(result.status()).isEqualTo("ASSIGNED");
        verify(examCandidateRepository).save(any(ExamCandidate.class));
    }

    @Test
    void should_reject_when_student_not_in_school() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AddExamCandidateCommand(examId, studentId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_when_user_is_not_student() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddExamCandidateCommand(examId, studentId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_when_candidate_already_exists() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(examCandidateRepository.existsByExamIdAndStudentId(examId, studentId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AddExamCandidateCommand(examId, studentId)))
            .isInstanceOf(DuplicatedException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_class_test_chair_when_student_is_outside_their_classes() {
        givenClassTestScope();
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(examDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(classId));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), List.of(classId)))
            .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddExamCandidateCommand(examId, studentId)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Học sinh không thuộc lớp bạn phụ trách");
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_allow_class_test_chair_for_student_in_their_class() {
        givenClassTestScope();
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(examDirectoryAccessService.callerClassIds(any())).thenReturn(List.of(classId));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), List.of(classId)))
            .thenReturn(List.of(activeMembership()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(studentId)).thenReturn(List.of(studentRole()));
        when(examCandidateRepository.existsByExamIdAndStudentId(examId, studentId)).thenReturn(false);
        when(examCandidateRepository.save(any(ExamCandidate.class))).thenAnswer(inv -> {
            ExamCandidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        var result = useCase.execute(new AddExamCandidateCommand(examId, studentId));

        assertThat(result.studentId()).isEqualTo(studentId);
    }

    @Test
    void should_reject_class_test_chair_who_teaches_no_class() {
        givenClassTestScope();
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, studentId)).thenReturn(true);
        when(examDirectoryAccessService.callerClassIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddExamCandidateCommand(examId, studentId)))
            .isInstanceOf(ForbiddenException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    private void givenClassTestScope() {
        when(examDirectoryAccessService.resolve(any(Exam.class)))
            .thenReturn(new ExamDirectoryScope(userId, schoolId, false));
    }

    private SchoolClassUser activeMembership() {
        var membership = new SchoolClassUser();
        membership.setUserId(studentId);
        membership.setSchoolClassId(classId);
        membership.setActive(true);
        return membership;
    }

    private UserRoleInfo studentRole() {
        return new UserRoleInfo(UUID.randomUUID(), studentId, UUID.randomUUID(), Instant.now(),
            "STUDENT", "Student");
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
