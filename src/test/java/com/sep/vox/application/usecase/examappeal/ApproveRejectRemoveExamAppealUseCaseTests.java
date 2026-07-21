package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.ExamAppealRejectedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.command.RejectExamAppealCommand;
import com.sep.vox.application.port.input.command.RemoveExamAppealReviewerCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.ApproveExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.RejectExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.RemoveExamAppealReviewerUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamAppealReviewer;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

public class ApproveRejectRemoveExamAppealUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamAppealReviewerRepository examAppealReviewerRepository;
    private ExamAppealAccessService examAppealAccessService;
    private EventPublisherPort eventPublisherPort;

    private ApproveExamAppealUseCase approveUseCase;
    private RejectExamAppealUseCase rejectUseCase;
    private RemoveExamAppealReviewerUseCase removeUseCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacher1 = UUID.randomUUID();
    private final UUID teacher2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examAppealReviewerRepository = mock(ExamAppealReviewerRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        eventPublisherPort = mock(EventPublisherPort.class);

        approveUseCase = new ApproveExamAppealUseCase(examResultAppealRepository, examAppealAccessService);
        rejectUseCase = new RejectExamAppealUseCase(
            examResultAppealRepository, examCandidateResultRepository, examAppealAccessService, eventPublisherPort);
        removeUseCase = new RemoveExamAppealReviewerUseCase(examAppealReviewerRepository, examAppealAccessService);

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
        var deadline = OffsetDateTime.now().plusDays(7);

        approveUseCase.execute(new ApproveExamAppealCommand(appealId, deadline));

        assertThat(context.appeal().getStatus()).isEqualTo(ExamAppealStatus.APPROVED);
        assertThat(context.appeal().getDeadline()).isEqualTo(deadline);
        assertThat(context.appeal().getApprovedAt()).isNotNull();
    }

    @Test
    void should_reject_approve_when_deadline_in_the_past() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        assertThatThrownBy(() -> approveUseCase.execute(
            new ApproveExamAppealCommand(appealId, OffsetDateTime.now().minusDays(1))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tương lai");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_approve_when_appeal_not_pending() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));

        assertThatThrownBy(() -> approveUseCase.execute(
            new ApproveExamAppealCommand(appealId, OffsetDateTime.now().plusDays(7))))
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
    void should_publish_rejected_event_to_notify_student() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        rejectUseCase.execute(new RejectExamAppealCommand(appealId, "Không đủ căn cứ"));

        var captor = ArgumentCaptor.forClass(ExamAppealRejectedEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue().studentId()).isEqualTo(studentId);
        assertThat(captor.getValue().reason()).isEqualTo("Không đủ căn cứ");
    }

    @Test
    void should_reject_when_reason_is_blank() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.PENDING));

        assertThatThrownBy(() -> rejectUseCase.execute(new RejectExamAppealCommand(appealId, "  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");

        verify(examResultAppealRepository, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_reject_reject_when_appeal_not_pending() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.COMPARING));

        assertThatThrownBy(() -> rejectUseCase.execute(new RejectExamAppealCommand(appealId, "lý do")))
            .isInstanceOf(IllegalStateException.class);
    }

    // ---- remove reviewer ---------------------------------------------------

    private ExamAppealReviewer reviewer(UUID reviewerId, ExamAppealReviewerStatus status) {
        var reviewer = new ExamAppealReviewer();
        reviewer.setId(UUID.randomUUID());
        reviewer.setAppealId(appealId);
        reviewer.setReviewerId(reviewerId);
        reviewer.setStatus(status);
        return reviewer;
    }

    @Test
    void should_remove_assigned_reviewer_when_others_remain() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var target = reviewer(teacher1, ExamAppealReviewerStatus.ASSIGNED);
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacher1))
            .thenReturn(Optional.of(target));
        when(examAppealReviewerRepository.findByAppealId(appealId))
            .thenReturn(List.of(target, reviewer(teacher2, ExamAppealReviewerStatus.ASSIGNED)));

        removeUseCase.execute(new RemoveExamAppealReviewerCommand(appealId, teacher1));

        verify(examAppealReviewerRepository).deleteById(target.getId());
    }

    @Test
    void should_reject_removing_the_last_reviewer() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        var target = reviewer(teacher1, ExamAppealReviewerStatus.ASSIGNED);
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacher1))
            .thenReturn(Optional.of(target));
        when(examAppealReviewerRepository.findByAppealId(appealId)).thenReturn(List.of(target));

        assertThatThrownBy(() -> removeUseCase.execute(new RemoveExamAppealReviewerCommand(appealId, teacher1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ít nhất 1");

        verify(examAppealReviewerRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_removing_a_reviewer_who_already_submitted() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacher1))
            .thenReturn(Optional.of(reviewer(teacher1, ExamAppealReviewerStatus.SUBMITTED)));

        assertThatThrownBy(() -> removeUseCase.execute(new RemoveExamAppealReviewerCommand(appealId, teacher1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã nộp báo cáo");

        verify(examAppealReviewerRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_removing_a_reviewer_not_assigned() {
        when(examAppealAccessService.load(appealId)).thenReturn(context(ExamAppealStatus.GRADING));
        when(examAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, teacher1))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> removeUseCase.execute(new RemoveExamAppealReviewerCommand(appealId, teacher1)))
            .isInstanceOf(NotFoundException.class);
    }
}
