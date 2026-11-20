package com.sep.vox.application.usecase.examsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamSessionRemainingTimeCommand;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionRemainingTimeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

class UpdateExamSessionRemainingTimeUseCaseTests {

    private UserContextPort userContextPort;
    private ExamSessionRepository examSessionRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamPaperRepository examPaperRepository;
    private UpdateExamSessionRemainingTimeUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    private ExamPaper paper;
    private ExamSession session;

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        useCase = new UpdateExamSessionRemainingTimeUseCase(
            userContextPort,
            examSessionRepository,
            examCandidateRepository,
            examPaperRepository
        );

        paper = new ExamPaper();
        paper.setId(paperId);
        paper.setExamId(examId);
        paper.setTimeDurationSeconds(1800);

        session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setPaperId(paperId);
        session.setCandidateId(candidateId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);

        var candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(userId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.of(session));
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(examPaperRepository.findById(paperId)).thenReturn(Optional.of(paper));
        when(examSessionRepository.checkpointRemainingSeconds(eq(sessionId), anyInt())).thenReturn(1);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    }

    private UpdateExamSessionRemainingTimeCommand command(int remainingSeconds) {
        return new UpdateExamSessionRemainingTimeCommand(sessionId, remainingSeconds);
    }

    @Test
    void should_checkpoint_a_plausible_value_unchanged() {
        useCase.execute(command(900));

        verify(examSessionRepository).checkpointRemainingSeconds(sessionId, 900);
    }

    /**
     * Giá trị do máy học viên gửi lên, nên một con số lớn bất thường không được phép trở thành thời
     * gian thi thật.
     */
    @Test
    void should_clamp_a_value_above_the_assigned_paper_duration() {
        useCase.execute(command(999_999));

        verify(examSessionRepository).checkpointRemainingSeconds(sessionId, 1800);
    }

    @Test
    void should_clamp_a_negative_value_to_zero() {
        useCase.execute(command(-5));

        verify(examSessionRepository).checkpointRemainingSeconds(sessionId, 0);
    }

    /**
     * Mã đề chưa có thời lượng thì không có chặn trên để áp; ràng buộc "chỉ đi lùi" ở tầng DB vẫn
     * giữ cho giá trị không tăng.
     */
    @Test
    void should_skip_the_ceiling_when_the_paper_has_no_duration() {
        paper.setTimeDurationSeconds(null);

        useCase.execute(command(5_000));

        verify(examSessionRepository).checkpointRemainingSeconds(sessionId, 5_000);
    }

    @Test
    void should_return_the_value_the_server_actually_holds() {
        var stored = new ExamSession();
        stored.setId(sessionId);
        stored.setRemainingSeconds(120);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(stored));

        assertThat(useCase.execute(command(900))).isEqualTo(120);
    }

    @Test
    void should_reject_a_checkpoint_for_a_session_the_caller_does_not_own() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(command(900)))
            .isInstanceOf(ForbiddenException.class);
        verify(examSessionRepository, never()).checkpointRemainingSeconds(any(), anyInt());
    }

    @Test
    void should_reject_a_checkpoint_for_a_session_that_already_ended() {
        when(examSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(900)))
            .isInstanceOf(NotFoundException.class);
        verify(examSessionRepository, never()).checkpointRemainingSeconds(any(), anyInt());
    }
}
