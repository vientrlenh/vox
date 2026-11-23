package com.sep.vox.application.usecase.assessmentsession;

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
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentSessionRemainingTimeCommand;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionRemainingTimeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

class UpdateAssessmentSessionRemainingTimeUseCaseTests {

    private UserContextPort userContextPort;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private UpdateAssessmentSessionRemainingTimeUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    private AssessmentPaper paper;
    private AssessmentSession session;

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        useCase = new UpdateAssessmentSessionRemainingTimeUseCase(
            userContextPort,
            assessmentSessionRepository,
            assessmentCandidateRepository,
            assessmentPaperRepository
        );

        paper = new AssessmentPaper();
        paper.setId(paperId);
        paper.setAssessmentId(assessmentId);
        paper.setTimeDurationSeconds(1800);

        session = new AssessmentSession();
        session.setId(sessionId);
        session.setAssessmentId(assessmentId);
        session.setPaperId(paperId);
        session.setCandidateId(candidateId);
        session.setStatus(AssessmentSessionStatus.IN_PROGRESS);

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(userId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(assessmentSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.of(session));
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentPaperRepository.findById(paperId)).thenReturn(Optional.of(paper));
        when(assessmentSessionRepository.checkpointRemainingSeconds(eq(sessionId), anyInt())).thenReturn(1);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    }

    private UpdateAssessmentSessionRemainingTimeCommand command(int remainingSeconds) {
        return new UpdateAssessmentSessionRemainingTimeCommand(sessionId, remainingSeconds);
    }

    @Test
    void should_checkpoint_a_plausible_value_unchanged() {
        useCase.execute(command(900));

        verify(assessmentSessionRepository).checkpointRemainingSeconds(sessionId, 900);
    }

    /**
     * Giá trị do máy học viên gửi lên, nên một con số lớn bất thường không được phép trở thành thời
     * gian thi thật.
     */
    @Test
    void should_clamp_a_value_above_the_assigned_paper_duration() {
        useCase.execute(command(999_999));

        verify(assessmentSessionRepository).checkpointRemainingSeconds(sessionId, 1800);
    }

    @Test
    void should_clamp_a_negative_value_to_zero() {
        useCase.execute(command(-5));

        verify(assessmentSessionRepository).checkpointRemainingSeconds(sessionId, 0);
    }

    /**
     * Mã đề chưa có thời lượng thì không có chặn trên để áp; ràng buộc "chỉ đi lùi" ở tầng DB vẫn
     * giữ cho giá trị không tăng.
     */
    @Test
    void should_skip_the_ceiling_when_the_paper_has_no_duration() {
        paper.setTimeDurationSeconds(null);

        useCase.execute(command(5_000));

        verify(assessmentSessionRepository).checkpointRemainingSeconds(sessionId, 5_000);
    }

    @Test
    void should_return_the_value_the_server_actually_holds() {
        AssessmentSession stored = new AssessmentSession();
        stored.setId(sessionId);
        stored.setRemainingSeconds(120);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(stored));

        assertThat(useCase.execute(command(900))).isEqualTo(120);
    }

    @Test
    void should_reject_a_checkpoint_for_a_session_the_caller_does_not_own() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(command(900)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentSessionRepository, never()).checkpointRemainingSeconds(any(), anyInt());
    }

    @Test
    void should_reject_a_checkpoint_for_a_session_that_already_ended() {
        when(assessmentSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(900)))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(assessmentSessionRepository, never()).checkpointRemainingSeconds(any(), anyInt());
    }
}
