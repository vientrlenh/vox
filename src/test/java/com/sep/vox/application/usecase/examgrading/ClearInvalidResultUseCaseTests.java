package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.ExamResultInvalidClearedEvent;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingActionSupport.PreparedAction;
import com.sep.vox.application.port.input.usecase.examgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Hành động phức tạp nhất trong bốn: nó vừa gỡ chặn thí sinh, vừa đổi trạng thái bài,
 * vừa tự mở một vòng chấm mới — và cả ba việc phải xong trong cùng một transaction.
 */
class ClearInvalidResultUseCaseTests {

    private GradingActionSupport gradingActionSupport;
    private ExamCandidateRepository examCandidateRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private EventPublisherPort eventPublisherPort;
    private ClearInvalidResultUseCase useCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    private ExamCandidateResult result;
    private ExamCandidate candidate;

    @BeforeEach
    void setUp() {
        gradingActionSupport = mock(GradingActionSupport.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new ClearInvalidResultUseCase(gradingActionSupport, examCandidateRepository,
            examGradingAssignmentRepository, eventPublisherPort);

        result = new ExamCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setStatus(ExamCandidateResultStatus.INVALID);

        candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        candidate.setBlockedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        var assignment = ExamGradingAssignment.open(candidateResultId, teacherId,
            GradingRoundType.REMEDIATION, null, null, Instant.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        var context = new GradingContext(
            assignment, result, new ExamSession(), UUID.randomUUID(), "IELTS Mock");
        when(gradingActionSupport.prepare(any(), any(), any())).thenReturn(new PreparedAction(
            context, teacherId, GradingRoundType.REMEDIATION, GradingOutcome.CLEARED_INVALID,
            "Xem lại video, không có vi phạm", null));
        when(gradingActionSupport.resolveStudentId(any())).thenReturn(studentId);
        when(examGradingAssignmentRepository.save(any())).thenAnswer(invocation -> {
            var saved = (ExamGradingAssignment) invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    private GradingDecisionCommand command() {
        return new GradingDecisionCommand(assignmentId, "Xem lại video, không có vi phạm");
    }

    private ExamGradingAssignment captureReopened() {
        var captor = ArgumentCaptor.forClass(ExamGradingAssignment.class);
        verify(examGradingAssignmentRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_unblock_the_candidate() {
        useCase.execute(command());

        // resolveDefaultStatus ép INVALID khi thí sinh còn bị chặn; không gỡ thì mọi
        // lần tính lại điểm sau này sẽ âm thầm huỷ quyết định của giáo viên.
        assertThat(candidate.getBlockedAt()).isNull();
        verify(examCandidateRepository).save(candidate);
    }

    @Test
    void should_open_exactly_one_initial_round_for_the_same_teacher() {
        var response = useCase.execute(command());

        var reopened = captureReopened();
        assertThat(reopened.getRoundType()).isEqualTo(GradingRoundType.INITIAL);
        assertThat(reopened.getTeacherId()).isEqualTo(teacherId);
        assertThat(reopened.getCandidateResultId()).isEqualTo(candidateResultId);
        // Bài không rơi vào pool auto-assign trống chủ -> bịt kịch bản review BE-1.
        assertThat(reopened.getActiveResultId()).isEqualTo(candidateResultId);
        assertThat(response.nextAssignmentId()).isEqualTo(reopened.getId());
    }

    @Test
    void should_close_the_remediation_round_before_opening_the_next_one() {
        useCase.execute(command());

        // Thứ tự bắt buộc: finish() nhả active_result_id của dòng cũ, rồi mới chèn
        // dòng mới — ngược lại là vi phạm unique index.
        var inOrder = org.mockito.Mockito.inOrder(gradingActionSupport, examGradingAssignmentRepository);
        inOrder.verify(gradingActionSupport).finish(any(), any());
        inOrder.verify(examGradingAssignmentRepository).save(any());
    }

    @Test
    void should_notify_the_student_that_the_paper_is_restored() {
        useCase.execute(command());

        var captor = ArgumentCaptor.forClass(ExamResultInvalidClearedEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue().studentId()).isEqualTo(studentId);
        assertThat(captor.getValue().reason()).isEqualTo("Xem lại video, không có vi phạm");
    }

    @Test
    void should_not_touch_the_candidate_when_it_was_never_blocked() {
        candidate.setBlockedAt(null);

        useCase.execute(command());

        verify(examCandidateRepository, never()).save(any());
    }
}
