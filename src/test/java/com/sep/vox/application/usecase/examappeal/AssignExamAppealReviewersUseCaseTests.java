package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewersCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.AssignExamAppealReviewersUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

public class AssignExamAppealReviewersUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamAppealReviewerRepository examAppealReviewerRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamAppealAccessService examAppealAccessService;
    private AssignExamAppealReviewersUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID teacher1 = UUID.randomUUID();
    private final UUID teacher2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examAppealReviewerRepository = mock(ExamAppealReviewerRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new AssignExamAppealReviewersUseCase(
            examResultAppealRepository,
            examAppealReviewerRepository,
            examCandidateResultRepository,
            examAppealAccessService
        );

        when(examAppealAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examAppealAccessService.isTeacherOfSchool(any(), any())).thenReturn(true);
    }

    private AppealContext context(ExamAppealStatus status) {
        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(status);
        var candidateResult = new ExamCandidateResult();
        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);
        return new AppealContext(appeal, candidateResult, new ExamSession(), schoolId, studentId, "Exam");
    }

    @Test
    void should_assign_reviewers_and_move_appeal_to_grading() {
        var context = context(ExamAppealStatus.APPROVED);
        when(examAppealAccessService.load(appealId)).thenReturn(context);

        useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of(teacher1, teacher2)));

        assertThat(context.appeal().getStatus()).isEqualTo(ExamAppealStatus.GRADING);
        assertThat(context.candidateResult().getStatus()).isEqualTo(ExamCandidateResultStatus.RE_GRADING);
        verify(examAppealReviewerRepository).saveAll(anyList());
        verify(examCandidateResultRepository).save(context.candidateResult());
    }

    @Test
    void should_accept_a_single_reviewer() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.APPROVED));

        useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of(teacher1)));

        verify(examAppealReviewerRepository).saveAll(anyList());
    }

    @Test
    void should_reject_when_no_reviewer_given() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.APPROVED));

        assertThatThrownBy(() -> useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ít nhất 1");

        verify(examAppealReviewerRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_when_more_than_five_reviewers() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.APPROVED));
        var six = IntStream.range(0, 6).mapToObj(index -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> useCase.execute(new AssignExamAppealReviewersCommand(appealId, six)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tối đa 5");

        verify(examAppealReviewerRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_duplicate_reviewers() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.APPROVED));

        assertThatThrownBy(() ->
            useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of(teacher1, teacher1))))
            .isInstanceOf(DuplicatedException.class);
    }

    @Test
    void should_reject_reviewer_from_another_school() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.APPROVED));
        when(examAppealAccessService.isTeacherOfSchool(teacher1, schoolId)).thenReturn(false);

        assertThatThrownBy(() ->
            useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of(teacher1))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cùng trường");
    }

    @Test
    void should_reject_assigning_the_student_as_reviewer() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.APPROVED));

        assertThatThrownBy(() ->
            useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of(studentId))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chính thí sinh");
    }

    @Test
    void should_reject_when_appeal_not_approved() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        assertThatThrownBy(() ->
            useCase.execute(new AssignExamAppealReviewersCommand(appealId, List.of(teacher1))))
            .isInstanceOf(IllegalStateException.class);

        verify(examAppealReviewerRepository, never()).saveAll(anyList());
    }
}
