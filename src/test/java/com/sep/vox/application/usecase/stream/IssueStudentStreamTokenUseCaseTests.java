package com.sep.vox.application.usecase.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.IssueStudentStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.stream.IssueStudentStreamTokenUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

class IssueStudentStreamTokenUseCaseTests {

    private UserContextPort userContextPort;
    private ExamScheduleRepository examScheduleRepository;
    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamSessionRepository examSessionRepository;
    private StreamTokenProvider streamTokenProvider;
    private IssueStudentStreamTokenUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    private Exam exam;
    private ExamSession session;

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        streamTokenProvider = mock(StreamTokenProvider.class);
        useCase = new IssueStudentStreamTokenUseCase(
            userContextPort,
            examScheduleRepository,
            examRepository,
            examCandidateRepository,
            examSessionRepository,
            streamTokenProvider
        );

        exam = new Exam();
        exam.setId(examId);
        exam.setRequiredStreamType(ExamRequiredStreamType.CAMERA_AND_SCREEN);

        session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setCandidateId(candidateId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);

        var candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(userId);
        candidate.setScheduleId(scheduleId);

        var now = OffsetDateTime.now();
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setStartDate(now.minusHours(1));
        schedule.setEndDate(now.plusHours(1));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.of(session));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(examScheduleRepository.findByIdAndInSchedule(eq(scheduleId), any())).thenReturn(Optional.of(schedule));
        when(examSessionRepository.lockChosenStreamType(eq(sessionId), any())).thenReturn(1);
        when(streamTokenProvider.generateStreamToken(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("jwt");
    }

    private IssueStudentStreamTokenCommand command(String streamType) {
        return new IssueStudentStreamTokenCommand(sessionId, streamType);
    }

    @Test
    void should_fall_back_to_exam_required_stream_type_when_request_omits_it() {
        var response = useCase.execute(command(null));

        assertThat(response.streamTypes()).containsExactly("camera", "screen");
        verify(examSessionRepository).lockChosenStreamType(sessionId, ExamRequiredStreamType.CAMERA_AND_SCREEN);
    }

    @Test
    void should_let_student_pick_a_single_stream_when_permission_is_any() {
        exam.setStreamTypePermission(ExamStreamTypePermission.ANY);

        var response = useCase.execute(command("SCREEN"));

        assertThat(response.streamTypes()).containsExactly("screen");
        verify(examSessionRepository).lockChosenStreamType(sessionId, ExamRequiredStreamType.SCREEN);
    }

    @Test
    void should_reject_a_single_stream_when_permission_is_all() {
        exam.setStreamTypePermission(ExamStreamTypePermission.ALL);

        assertThatThrownBy(() -> useCase.execute(command("CAMERA")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examSessionRepository, never()).lockChosenStreamType(any(), any());
    }

    /**
     * vox-streaming và WPF đều dùng "camera"/"screen" chữ thường cho cùng những giá trị này, nên
     * việc phát lại token từ phía client không được fail chỉ vì kiểu chữ.
     */
    @Test
    void should_accept_a_lowercase_stream_type() {
        exam.setStreamTypePermission(ExamStreamTypePermission.ANY);

        var response = useCase.execute(command("camera"));

        assertThat(response.streamTypes()).containsExactly("camera");
        verify(examSessionRepository).lockChosenStreamType(sessionId, ExamRequiredStreamType.CAMERA);
    }

    @Test
    void should_reuse_the_locked_choice_on_a_later_token_request() {
        exam.setStreamTypePermission(ExamStreamTypePermission.ANY);
        session.setChosenStreamType(ExamRequiredStreamType.SCREEN);

        var response = useCase.execute(command(null));

        assertThat(response.streamTypes()).containsExactly("screen");
        verify(examSessionRepository, never()).lockChosenStreamType(any(), any());
    }

    /**
     * Cốt lõi của việc khóa: không cho phép bắt đầu bằng camera + màn hình rồi phát lại token với
     * mỗi camera để tắt phần ghi màn hình giữa kỳ thi.
     */
    @Test
    void should_reject_switching_stream_type_after_the_session_locked_one() {
        exam.setStreamTypePermission(ExamStreamTypePermission.ANY);
        session.setChosenStreamType(ExamRequiredStreamType.CAMERA_AND_SCREEN);

        assertThatThrownBy(() -> useCase.execute(command("CAMERA")))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_follow_the_winner_when_it_loses_the_lock_race() {
        exam.setStreamTypePermission(ExamStreamTypePermission.ANY);
        var winner = new ExamSession();
        winner.setId(sessionId);
        winner.setChosenStreamType(ExamRequiredStreamType.SCREEN);
        when(examSessionRepository.lockChosenStreamType(eq(sessionId), any())).thenReturn(0);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(winner));

        var response = useCase.execute(command(null));

        assertThat(response.streamTypes()).containsExactly("screen");
    }

    /**
     * Việc chốt loại stream là một thao tác GHI, nên nó phải nằm sau kiểm tra quyền sở hữu phiên
     * thi - nếu không, người không sở hữu phiên vẫn kịp chốt hộ trước khi bị từ chối.
     */
    @Test
    void should_not_lock_a_stream_type_for_a_session_the_caller_does_not_own() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(command("CAMERA")))
            .isInstanceOf(ForbiddenException.class);
        verify(examSessionRepository, never()).lockChosenStreamType(any(), any());
    }
}
