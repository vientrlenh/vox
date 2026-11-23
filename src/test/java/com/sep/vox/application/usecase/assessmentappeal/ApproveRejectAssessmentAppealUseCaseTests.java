package com.sep.vox.application.usecase.assessmentappeal;

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

import com.sep.vox.application.event.AssessmentAppealRejectedPayloadV1;
import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.RejectAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.RejectAssessmentAppealUseCase;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;

public class ApproveRejectAssessmentAppealUseCaseTests {

    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentAppealAccessService assessmentAppealAccessService;
    private OutboxRepository outboxRepository;

    private ApproveAssessmentAppealUseCase approveUseCase;
    private RejectAssessmentAppealUseCase rejectUseCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentAppealAccessService = mock(AssessmentAppealAccessService.class);
        outboxRepository = mock(OutboxRepository.class);

        approveUseCase = new ApproveAssessmentAppealUseCase(assessmentResultAppealRepository, assessmentAppealAccessService);
        rejectUseCase = new RejectAssessmentAppealUseCase(
            assessmentResultAppealRepository, assessmentCandidateResultRepository, assessmentAppealAccessService,
            outboxRepository, OutboxTestSupport.jsonSerializationPort());

        when(assessmentAppealAccessService.requireActiveUserId()).thenReturn(adminId);
    }

    private AppealContext context(AssessmentAppealStatus status) {
        AssessmentResultAppeal appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(status);
        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);
        return new AppealContext(appeal, candidateResult, new AssessmentSession(),
            UUID.randomUUID(), studentId, "IELTS Speaking Mock");
    }

    // ---- approve -----------------------------------------------------------

    @Test
    void should_approve_and_set_deadline_and_approved_at() {
        AssessmentAppealAccessService.AppealContext context = context(AssessmentAppealStatus.PENDING);
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context);
        Instant deadline = Instant.now().plus(7, ChronoUnit.DAYS);

        approveUseCase.execute(new ApproveAssessmentAppealCommand(appealId, deadline));

        assertThat(context.appeal().getStatus()).isEqualTo(AssessmentAppealStatus.APPROVED);
        assertThat(context.appeal().getDeadline()).isEqualTo(deadline);
        assertThat(context.appeal().getApprovedAt()).isNotNull();
    }

    @Test
    void should_reject_approve_when_deadline_in_the_past() {
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context(AssessmentAppealStatus.PENDING));

        assertThatThrownBy(() -> approveUseCase.execute(
            new ApproveAssessmentAppealCommand(appealId, Instant.now().minus(1, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tương lai");

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_approve_when_appeal_not_pending() {
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context(AssessmentAppealStatus.GRADING));

        assertThatThrownBy(() -> approveUseCase.execute(
            new ApproveAssessmentAppealCommand(appealId, Instant.now().plus(7, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- reject ------------------------------------------------------------

    @Test
    void should_reject_appeal_and_restore_result_to_released() {
        AssessmentAppealAccessService.AppealContext context = context(AssessmentAppealStatus.PENDING);
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context);

        rejectUseCase.execute(new RejectAssessmentAppealCommand(appealId, "Không đủ căn cứ"));

        assertThat(context.appeal().getStatus()).isEqualTo(AssessmentAppealStatus.REJECTED);
        assertThat(context.appeal().getDecisionNote()).isEqualTo("Không đủ căn cứ");
        assertThat(context.appeal().getResolvedAt()).isNotNull();
        assertThat(context.appeal().getResolvedBy()).isEqualTo(adminId);
        assertThat(context.candidateResult().getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_write_rejected_event_to_outbox_to_notify_student() {
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context(AssessmentAppealStatus.PENDING));

        rejectUseCase.execute(new RejectAssessmentAppealCommand(appealId, "Không đủ căn cứ"));

        AssessmentAppealRejectedPayloadV1 payload = OutboxTestSupport.capturePayload(
            outboxRepository, EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, AssessmentAppealRejectedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
        assertThat(payload.reason()).isEqualTo("Không đủ căn cứ");
    }

    @Test
    void should_reject_when_reason_is_blank() {
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context(AssessmentAppealStatus.PENDING));

        assertThatThrownBy(() -> rejectUseCase.execute(new RejectAssessmentAppealCommand(appealId, "  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");

        verify(assessmentResultAppealRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_reject_reject_when_appeal_not_pending() {
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context(AssessmentAppealStatus.GRADING));

        assertThatThrownBy(() -> rejectUseCase.execute(new RejectAssessmentAppealCommand(appealId, "lý do")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
