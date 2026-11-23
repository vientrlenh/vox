package com.sep.vox.application.usecase.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.stream.IssueStudentStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.stream.IssueStudentStreamTokenUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.stream.IssueStudentStreamTokenResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

class IssueStudentStreamTokenUseCaseTests {

    private UserContextPort userContextPort;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private StreamTokenProvider streamTokenProvider;
    private IssueStudentStreamTokenUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    private Assessment assessment;
    private AssessmentSession session;

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        streamTokenProvider = mock(StreamTokenProvider.class);
        useCase = new IssueStudentStreamTokenUseCase(
            userContextPort,
            assessmentScheduleRepository,
            assessmentRepository,
            assessmentCandidateRepository,
            assessmentSessionRepository,
            streamTokenProvider
        );

        assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setRequiredStreamType(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);

        session = new AssessmentSession();
        session.setId(sessionId);
        session.setAssessmentId(assessmentId);
        session.setCandidateId(candidateId);
        session.setStatus(AssessmentSessionStatus.IN_PROGRESS);

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(userId);
        candidate.setScheduleId(scheduleId);

        Instant now = Instant.now();
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setStartDate(now.minus(1, ChronoUnit.HOURS));
        schedule.setEndDate(now.plus(1, ChronoUnit.HOURS));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(assessmentSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.of(session));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentScheduleRepository.findByIdAndInSchedule(eq(scheduleId), any())).thenReturn(Optional.of(schedule));
        when(assessmentSessionRepository.lockChosenStreamType(eq(sessionId), any())).thenReturn(1);
        when(streamTokenProvider.generateStreamToken(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("jwt");
    }

    private IssueStudentStreamTokenCommand command(AssessmentRequiredStreamType streamType) {
        return new IssueStudentStreamTokenCommand(sessionId, streamType);
    }

    @Test
    void should_fall_back_to_assessment_required_stream_type_when_request_omits_it() {
        IssueStudentStreamTokenResponse response = useCase.execute(command(null));

        assertThat(response.streamTypes()).containsExactly("camera", "screen");
        verify(assessmentSessionRepository).lockChosenStreamType(sessionId, AssessmentRequiredStreamType.CAMERA_AND_SCREEN);
    }

    @Test
    void should_let_student_pick_a_single_stream_when_permission_is_any() {
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);

        IssueStudentStreamTokenResponse response = useCase.execute(command(AssessmentRequiredStreamType.SCREEN));

        assertThat(response.streamTypes()).containsExactly("screen");
        verify(assessmentSessionRepository).lockChosenStreamType(sessionId, AssessmentRequiredStreamType.SCREEN);
    }

    @Test
    void should_reject_a_single_stream_when_permission_is_all() {
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ALL);

        assertThatThrownBy(() -> useCase.execute(command(AssessmentRequiredStreamType.CAMERA)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentSessionRepository, never()).lockChosenStreamType(any(), any());
    }

    @Test
    void should_reuse_the_locked_choice_on_a_later_token_request() {
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);
        session.setChosenStreamType(AssessmentRequiredStreamType.SCREEN);

        IssueStudentStreamTokenResponse response = useCase.execute(command(null));

        assertThat(response.streamTypes()).containsExactly("screen");
        verify(assessmentSessionRepository, never()).lockChosenStreamType(any(), any());
    }

    /**
     * Cốt lõi của việc khóa: không cho phép bắt đầu bằng camera + màn hình rồi phát lại token với
     * mỗi camera để tắt phần ghi màn hình giữa kỳ thi.
     */
    @Test
    void should_reject_switching_stream_type_after_the_session_locked_one() {
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);
        session.setChosenStreamType(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);

        assertThatThrownBy(() -> useCase.execute(command(AssessmentRequiredStreamType.CAMERA)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_follow_the_winner_when_it_loses_the_lock_race() {
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);
        AssessmentSession winner = new AssessmentSession();
        winner.setId(sessionId);
        winner.setChosenStreamType(AssessmentRequiredStreamType.SCREEN);
        when(assessmentSessionRepository.lockChosenStreamType(eq(sessionId), any())).thenReturn(0);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(winner));

        IssueStudentStreamTokenResponse response = useCase.execute(command(null));

        assertThat(response.streamTypes()).containsExactly("screen");
    }

    /**
     * Việc chốt loại stream là một thao tác GHI, nên nó phải nằm sau kiểm tra quyền sở hữu phiên
     * thi - nếu không, người không sở hữu phiên vẫn kịp chốt hộ trước khi bị từ chối.
     */
    @Test
    void should_not_lock_a_stream_type_for_a_session_the_caller_does_not_own() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(command(AssessmentRequiredStreamType.CAMERA)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentSessionRepository, never()).lockChosenStreamType(any(), any());
    }
}
