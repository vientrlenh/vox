package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ConflictException;
import com.sep.vox.application.port.input.command.AddExamCandidateCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.AddExamCandidateUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AddExamCandidateUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AddExamCandidateUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new AddExamCandidateUseCase(
            examRepository, examCandidateRepository, examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
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
            .isInstanceOf(ConflictException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    private UserRoleInfo studentRole() {
        return new UserRoleInfo(UUID.randomUUID(), studentId, UUID.randomUUID(), OffsetDateTime.now(),
            "STUDENT", "Student");
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
