package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.AssignExamPapersCommand;
import com.sep.vox.application.port.input.command.ExamPaperAssignmentItem;
import com.sep.vox.application.port.input.usecase.examcandidate.AssignExamPapersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AssignExamPapersUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssignExamPapersUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidate1 = UUID.randomUUID();
    private final UUID candidate2 = UUID.randomUUID();
    private final UUID paper1 = UUID.randomUUID();
    private final UUID paper2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new AssignExamPapersUseCase(
            examRepository, examCandidateRepository, examPaperRepository, examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    }

    @Test
    void should_assign_papers_on_happy_path() {
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(
            paper(paper1, ExamPaperStatus.LOCKED), paper(paper2, ExamPaperStatus.LOCKED)));
        when(examCandidateRepository.findByIdInAndExamId(anyCollection(), org.mockito.ArgumentMatchers.eq(examId)))
            .thenReturn(List.of(candidate(candidate1), candidate(candidate2)));
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });

        var result = useCase.execute(new AssignExamPapersCommand(examId, List.of(
            new ExamPaperAssignmentItem(candidate1, paper1),
            new ExamPaperAssignmentItem(candidate2, paper2))));

        assertThat(result.updated()).isEqualTo(2);
        verify(examCandidateRepository).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_any_exam_paper_not_locked_even_outside_batch() {
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(
            paper(paper1, ExamPaperStatus.LOCKED), paper(paper2, ExamPaperStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(new AssignExamPapersCommand(examId, List.of(
            new ExamPaperAssignmentItem(candidate1, paper1)))))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_candidate_not_in_exam() {
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(paper(paper1, ExamPaperStatus.LOCKED)));
        when(examCandidateRepository.findByIdInAndExamId(anyCollection(), org.mockito.ArgumentMatchers.eq(examId)))
            .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AssignExamPapersCommand(examId, List.of(
            new ExamPaperAssignmentItem(candidate1, paper1)))))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_paper_not_part_of_exam() {
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(paper(paper1, ExamPaperStatus.LOCKED)));
        when(examCandidateRepository.findByIdInAndExamId(anyCollection(), org.mockito.ArgumentMatchers.eq(examId)))
            .thenReturn(List.of(candidate(candidate1)));

        assertThatThrownBy(() -> useCase.execute(new AssignExamPapersCommand(examId, List.of(
            new ExamPaperAssignmentItem(candidate1, UUID.randomUUID())))))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    private ExamCandidate candidate(UUID id) {
        var c = new ExamCandidate();
        c.setId(id);
        c.setExamId(examId);
        c.setStudentId(UUID.randomUUID());
        c.setStatus(ExamCandidateStatus.ASSIGNED);
        return c;
    }

    private ExamPaper paper(UUID id, ExamPaperStatus status) {
        var p = new ExamPaper();
        p.setId(id);
        p.setExamId(examId);
        p.setStatus(status);
        return p;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
