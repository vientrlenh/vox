package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamCandidateCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.DeleteExamCandidateUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class DeleteExamCandidateUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private DeleteExamCandidateUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteExamCandidateUseCase(
            examRepository, examCandidateRepository, examSessionRepository, examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.DRAFT)));
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(examId)));
        when(examSessionRepository.findAllByCandidateId(candidateId)).thenReturn(List.of());
    }

    @Test
    void should_delete_candidate_without_any_session() {
        useCase.execute(new DeleteExamCandidateCommand(examId, candidateId));

        verify(examCandidateRepository).deleteById(candidateId);
    }

    @Test
    void should_reject_when_candidate_already_has_session() {
        when(examSessionRepository.findAllByCandidateId(candidateId)).thenReturn(List.of(new ExamSession()));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamCandidateCommand(examId, candidateId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã có bài thi");
        verify(examCandidateRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_exam_already_started() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamCandidateCommand(examId, candidateId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_user_is_neither_school_admin_nor_chair() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new DeleteExamCandidateCommand(examId, candidateId)))
            .isInstanceOf(ForbiddenException.class);
        verify(examCandidateRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_candidate_belongs_to_another_exam() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamCandidateCommand(examId, candidateId)))
            .isInstanceOf(NotFoundException.class);
        verify(examCandidateRepository, never()).deleteById(any());
    }

    private ExamCandidate candidate(UUID ownerExamId) {
        var c = new ExamCandidate();
        c.setId(candidateId);
        c.setExamId(ownerExamId);
        c.setStudentId(UUID.randomUUID());
        c.setStatus(ExamCandidateStatus.ASSIGNED);
        return c;
    }

    private Exam exam(ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setStatus(status);
        return exam;
    }
}
