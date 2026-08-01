package com.sep.vox.application.usecase.examappeal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.command.RejectExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.ApproveExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.RejectExamAppealUseCase;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;

public class ApproveRejectExamAppealUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamAppealAccessService examAppealAccessService;
    private OutboxRepository outboxRepository;

    private ApproveExamAppealUseCase approveUseCase;
    private RejectExamAppealUseCase rejectUseCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        outboxRepository = mock(OutboxRepository.class);

        approveUseCase = new ApproveExamAppealUseCase(examResultAppealRepository, examAppealAccessService);
        rejectUseCase = new RejectExamAppealUseCase(
            examResultAppealRepository, examCandidateResultRepository, examAppealAccessService,
            outboxRepository, OutboxTestSupport.jsonSerializationPort());

        when(examAppealAccessService.requireActiveUserId()).thenReturn(adminId);
    }

    private AppealContext context(ExamAppealStatus status) {
        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(status);
        var candidateResult = new ExamCandidateResult();
        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);
        return new AppealContext(appeal, candidateResult, new ExamSession(),
            UUID.randomUUID(), studentId, "IELTS Speaking Mock");
    }

    // ---- approve -----------------------------------------------------------

    @Test
    void should_approve_and_set_deadline_and_approved_at() {
        var context = context(ExamAppealStatus.PENDING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);
        var deadline = Instant.now().plus(7, ChronoUnit.DAYS);

        approveUseCase.execute(new ApproveExamAppealCommand(appealId, deadline));

        assertThat(context.appeal().getStatus()).isEqualTo(ExamAppealStatus.APPROVED);
        assertThat(context.appeal().getDeadline()).isEqualTo(deadline);
        assertThat(context.appeal().getApprovedAt()).isNotNull();
    }

    @Test
    void should_reject_approve_when_deadline_in_the_past() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        assertThatThrownBy(() -> approveUseCase.execute(
            new ApproveExamAppealCommand(appealId, Instant.now().minus(1, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tương lai");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_approve_when_appeal_not_pending() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));

        assertThatThrownBy(() -> approveUseCase.execute(
            new ApproveExamAppealCommand(appealId, Instant.now().plus(7, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalStateException.class);
    }

    // ---- reject ------------------------------------------------------------

    @Test
    void should_reject_appeal_and_restore_result_to_released() {
        var context = context(ExamAppealStatus.PENDING);
        when(examAppealAccessService.load(appealId)).thenReturn(context);

        rejectUseCase.execute(new RejectExamAppealCommand(appealId, "Không đủ căn cứ"));

        assertThat(context.appeal().getStatus()).isEqualTo(ExamAppealStatus.REJECTED);
        assertThat(context.appeal().getDecisionNote()).isEqualTo("Không đủ căn cứ");
        assertThat(context.appeal().getResolvedAt()).isNotNull();
        assertThat(context.appeal().getResolvedBy()).isEqualTo(adminId);
        assertThat(context.candidateResult().getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_write_rejected_event_to_outbox_to_notify_student() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        rejectUseCase.execute(new RejectExamAppealCommand(appealId, "Không đủ căn cứ"));

        var payload = OutboxTestSupport.capturePayload(
            outboxRepository, EventTypeConstant.EXAM_APPEAL_REJECTED, ExamAppealRejectedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
        assertThat(payload.reason()).isEqualTo("Không đủ căn cứ");
    }

    @Test
    void should_reject_when_reason_is_blank() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        assertThatThrownBy(() -> rejectUseCase.execute(new RejectExamAppealCommand(appealId, "  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");

        verify(examResultAppealRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_reject_reject_when_appeal_not_pending() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));

        assertThatThrownBy(() -> rejectUseCase.execute(new RejectExamAppealCommand(appealId, "lý do")))
            .isInstanceOf(IllegalStateException.class);
    }
}
