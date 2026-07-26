package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.ExamResultRegradedEvent;
import com.sep.vox.application.event.ExamResultReleasedEvent;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;

/**
 * Phần dùng chung của bốn hành động: kiểm luật ở đầu, và đổi trạng thái + audit +
 * thông báo ở cuối. Đây là chỗ duy nhất ghi nhật ký, nên nếu nó bỏ sót thì cả tính
 * năng audit coi như không tồn tại.
 */
class GradingActionSupportTests {

    private ExamGradingAccessService examGradingAccessService;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamResultStatusHistoryRepository examResultStatusHistoryRepository;
    private EventPublisherPort eventPublisherPort;
    private GradingActionSupport support;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingAccessService = mock(ExamGradingAccessService.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examResultStatusHistoryRepository = mock(ExamResultStatusHistoryRepository.class);
        eventPublisherPort = mock(EventPublisherPort.class);

        // Recorder dùng bản THẬT: "có ghi audit không" là chính thứ đang kiểm.
        support = new GradingActionSupport(
            examGradingAccessService,
            examCandidateResultRepository,
            examCandidateRepository,
            examGradingAssignmentRepository,
            mock(ExamResultAppealRepository.class),
            new ResultStatusHistoryRecorder(examResultStatusHistoryRepository),
            eventPublisherPort);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);

        var candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
    }

    private ExamCandidateResult result(ExamCandidateResultStatus status, String totalScore) {
        var result = new ExamCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setStatus(status);
        result.setTotalScore(totalScore == null ? null : new BigDecimal(totalScore));
        return result;
    }

    private GradingContext context(GradingRoundType roundType, ExamCandidateResult result) {
        var assignment = ExamGradingAssignment.open(candidateResultId, teacherId, roundType, null,
            result.getTotalScore(), OffsetDateTime.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        return new GradingContext(assignment, result, new ExamSession(), UUID.randomUUID(), "IELTS Mock");
    }

    private void given(GradingRoundType roundType, ExamCandidateResult result) {
        when(examGradingAccessService.load(assignmentId)).thenReturn(context(roundType, result));
    }

    private ExamResultStatusHistory captureHistory() {
        var captor = ArgumentCaptor.forClass(ExamResultStatusHistory.class);
        verify(examResultStatusHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- prepare: kiểm luật trước khi động vào dữ liệu ----------------------

    @Test
    void should_reject_an_action_that_does_not_belong_to_the_round() {
        given(GradingRoundType.INITIAL, result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00"));

        assertThatThrownBy(() ->
            support.prepare(assignmentId, GradingOutcome.CLEARED_INVALID, "lý do"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ với vòng chấm");
    }

    @Test
    void should_reject_when_the_result_has_left_the_rounds_status() {
        // Bài bị luồng khác kéo đi (phúc khảo, chốt sổ) trong lúc giáo viên mở màn chấm.
        given(GradingRoundType.INITIAL, result(ExamCandidateResultStatus.RELEASED, "6.00"));

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.UPHELD, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không còn ở trạng thái");
    }

    @Test
    void should_reject_a_completed_assignment() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        var context = context(GradingRoundType.INITIAL, result);
        context.assignment().complete(GradingOutcome.UPHELD, null, OffsetDateTime.now());
        when(examGradingAccessService.load(assignmentId)).thenReturn(context);

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.UPHELD, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chốt");
    }

    @Test
    void should_require_a_reason_where_the_policy_demands_one() {
        given(GradingRoundType.INITIAL, result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00"));

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.INVALIDATED, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");
    }

    // ---- finish: trạng thái, audit, sự kiện --------------------------------

    @Test
    void should_release_the_result_and_close_the_assignment() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(result.getReleasedAt()).isNotNull();
        var assignment = prepared.context().assignment();
        assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.COMPLETED);
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.UPHELD);
        // Nhả chỗ để bài nhận được vòng tiếp theo — đây là cơ chế của unique index.
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_publish_a_first_release_notification_but_not_a_change_one() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        var captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ExamResultReleasedEvent.class);
        assertThat(((ExamResultReleasedEvent) captor.getValue()).studentId()).isEqualTo(studentId);
    }

    @Test
    void should_not_notify_when_a_spot_check_keeps_the_score() {
        var result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        // Hậu kiểm giữ nguyên điểm: làm phiền học sinh bằng "điểm vừa thay đổi" là sai.
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_notify_when_a_spot_check_changes_a_published_score() {
        var result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.REGRADED, null);
        result.setTotalScore(new BigDecimal("7.50"));

        support.finish(prepared, result);

        var captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ExamResultRegradedEvent.class);
        var event = (ExamResultRegradedEvent) captor.getValue();
        assertThat(event.scoreBefore()).isEqualByComparingTo("6.00");
        assertThat(event.scoreAfter()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_record_the_status_change_with_the_round_as_source() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, "ghi chú");

        support.finish(prepared, result);

        var history = captureHistory();
        assertThat(history.getFromStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
        assertThat(history.getToStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.TEACHER_INITIAL);
        assertThat(history.getActorId()).isEqualTo(teacherId);
        assertThat(history.getReason()).isEqualTo("ghi chú");
    }

    @Test
    void should_still_record_an_upheld_decision_that_changes_nothing() {
        var result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, "AI chấm đúng");

        support.finish(prepared, result);

        // Không đổi trạng thái, không đổi điểm — nhưng giáo viên ĐÃ ra quyết định, và
        // quyết định đó là thứ chứng minh bài đã được người xem.
        var history = captureHistory();
        assertThat(history.getFromStatus()).isEqualTo(history.getToStatus());
        assertThat(history.getReason()).isEqualTo("AI chấm đúng");
    }

    @Test
    void should_record_the_invalidation_reason() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.INVALIDATED, "Gian lận: đọc bài mẫu");

        support.finish(prepared, result);

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(result.getFinalizedAt()).isNotNull();
        assertThat(captureHistory().getReason()).isEqualTo("Gian lận: đọc bài mẫu");
    }

    @Test
    void should_save_the_result_only_when_the_status_actually_changes() {
        var result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        var prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        verify(examCandidateResultRepository, never()).save(eq(result));
    }
}
