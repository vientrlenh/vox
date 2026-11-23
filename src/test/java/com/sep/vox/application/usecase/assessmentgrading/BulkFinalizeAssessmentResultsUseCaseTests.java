package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.BulkFinalizeAssessmentResultsCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.assessmentgrading.BulkFinalizeAssessmentResultsUseCase;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentResultStatusHistoryRepository;

/**
 * Lối thoát cho tình trạng một bài treo chặn cả kỳ thi (review BE-5) — nhưng nó chỉ đưa
 * bài về {@code RELEASED}, KHÔNG bao giờ sinh {@code FINAL}, và không được đụng vào tranh
 * chấp điểm đang treo. Ba điều đó quan trọng ngang nhau.
 */
class BulkFinalizeAssessmentResultsUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private AssessmentResultStatusHistoryRepository assessmentResultStatusHistoryRepository;
    private BulkFinalizeAssessmentResultsUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        assessmentResultStatusHistoryRepository = mock(AssessmentResultStatusHistoryRepository.class);
        useCase = new BulkFinalizeAssessmentResultsUseCase(
            assessmentRepository, assessmentCandidateResultRepository, assessmentGradingAssignmentRepository,
            assessmentGradingQueryRepository, assessmentGradingAccessService,
            new ResultStatusHistoryRecorder(assessmentResultStatusHistoryRepository));

        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
            .thenReturn(List.of());
        givenPreview(clean());
        givenResults();
    }

    private BulkFinalizePreviewInfo clean() {
        return new BulkFinalizePreviewInfo(3, 3, 0, 0, 0, 0, List.of());
    }

    private BulkFinalizePreviewInfo pendingBlocked() {
        return new BulkFinalizePreviewInfo(3, 1, 1, 1, 0, 0, List.of(UUID.randomUUID()));
    }

    private void givenPreview(BulkFinalizePreviewInfo preview) {
        when(assessmentGradingQueryRepository.previewBulkFinalize(schoolId, assessmentId)).thenReturn(preview);
    }

    private void givenResults(AssessmentCandidateResult... results) {
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(results));
    }

    private AssessmentCandidateResult result(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(UUID.randomUUID());
        result.setStatus(status);
        return result;
    }

    private BulkFinalizeAssessmentResultsCommand confirmed() {
        return new BulkFinalizeAssessmentResultsCommand(assessmentId, true);
    }

    @SuppressWarnings("unchecked")
    private List<AssessmentResultStatusHistory> captureHistories() {
        ArgumentCaptor<List<AssessmentResultStatusHistory>> captor = (ArgumentCaptor<List<AssessmentResultStatusHistory>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(assessmentResultStatusHistoryRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_refuse_when_papers_are_still_in_flight_and_the_admin_has_not_confirmed() {
        givenPreview(pendingBlocked());
        givenResults(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> useCase.execute(new BulkFinalizeAssessmentResultsCommand(assessmentId, false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chưa ai chấm");

        verify(assessmentCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_while_an_appeal_is_still_open_even_when_the_admin_confirmed() {
        // Cờ "công bố theo điểm AI" chỉ nói về bài chưa ai chấm. Nuốt luôn một tranh
        // chấp điểm đang treo thì đơn kẹt mở vĩnh viễn.
        givenPreview(new BulkFinalizePreviewInfo(3, 2, 0, 0, 1, 0, List.of(UUID.randomUUID())));
        givenResults(result(AssessmentCandidateResultStatus.RELEASED));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phúc khảo");

        verify(assessmentCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_when_a_paper_is_still_under_appeal() {
        givenResults(
            result(AssessmentCandidateResultStatus.APPEALED),
            result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đơn phúc khảo");

        verify(assessmentCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_when_a_paper_is_being_regraded_for_an_appeal() {
        // Một giáo viên đang cầm bài chấm dở ở vòng APPEAL — chốt sổ không quyết thay.
        givenResults(result(AssessmentCandidateResultStatus.RE_GRADING));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chấm phúc khảo");

        verify(assessmentCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_name_every_blocking_status_in_one_message() {
        // Báo từng thứ một là bắt admin bấm lại nhiều lần mới biết còn gì phải xử lý.
        givenResults(
            result(AssessmentCandidateResultStatus.APPEALED),
            result(AssessmentCandidateResultStatus.RE_GRADING));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContainingAll("đơn phúc khảo", "chấm phúc khảo");
    }

    @Test
    void should_release_pending_papers_on_ai_scores_once_the_admin_confirms() {
        givenPreview(pendingBlocked());
        AssessmentCandidateResult pending = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        givenResults(pending);

        Integer changed = useCase.execute(confirmed());

        assertThat(changed).isEqualTo(1);
        assertThat(pending.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        assertThat(pending.getReleasedAt()).isNotNull();
        // A newly published submission is not at final review yet: publishing is where
        // PASSED/FAILED is decided.
        assertThat(pending.getFinalizedAt()).isNull();
    }

    @Test
    void should_never_produce_final() {
        // FINAL is a POST-publish status (produced by finalizeForPublish when the policy has no
        // passingScore). Producing it here would make requirePublishReadiness reject the whole
        // assessment, while the only way out of FINAL demands that the assessment has been published -- a
        // closed deadlock.
        givenPreview(pendingBlocked());
        AssessmentCandidateResult pending = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        AssessmentCandidateResult released = result(AssessmentCandidateResultStatus.RELEASED);
        givenResults(pending, released);

        useCase.execute(confirmed());

        assertThat(pending.getStatus()).isNotEqualTo(AssessmentCandidateResultStatus.FINAL);
        assertThat(released.getStatus()).isNotEqualTo(AssessmentCandidateResultStatus.FINAL);
    }

    @Test
    void should_leave_released_invalid_and_already_decided_results_alone() {
        AssessmentCandidateResult released = result(AssessmentCandidateResultStatus.RELEASED);
        AssessmentCandidateResult invalid = result(AssessmentCandidateResultStatus.INVALID);
        AssessmentCandidateResult passed = result(AssessmentCandidateResultStatus.PASSED);
        givenResults(released, invalid, passed);

        Integer changed = useCase.execute(confirmed());

        // RELEASED is ready to publish; INVALID already has its conclusion and will turn into
        // FAILED by itself (score forced to 0) when the assessment is published; PASSED is already final.
        assertThat(changed).isZero();
        assertThat(released.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        assertThat(invalid.getStatus()).isEqualTo(AssessmentCandidateResultStatus.INVALID);
        assertThat(passed.getStatus()).isEqualTo(AssessmentCandidateResultStatus.PASSED);
    }

    @Test
    void should_keep_the_original_released_at_when_a_paper_already_had_one() {
        // A submission that was once RELEASED and then pulled back to PENDING_REVIEW (invalidation
        // lifted) must not get its release timestamp reset: the appeal window is counted from that
        // timestamp.
        givenPreview(pendingBlocked());
        AssessmentCandidateResult pending = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        Instant originalReleasedAt = Instant.now().minus(3, ChronoUnit.DAYS);
        pending.setReleasedAt(originalReleasedAt);
        givenResults(pending);

        useCase.execute(confirmed());

        assertThat(pending.getReleasedAt()).isEqualTo(originalReleasedAt);
    }

    @Test
    void should_record_which_papers_were_published_on_ai_scores() {
        givenPreview(pendingBlocked());
        givenResults(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        useCase.execute(confirmed());

        List<AssessmentResultStatusHistory> histories = captureHistories();
        assertThat(histories).hasSize(1);
        assertThat(histories).allSatisfy(history -> {
            assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.ADMIN_BULK_FINALIZE);
            assertThat(history.getToStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
            // It must be distinguishable later: this submission was published on the AI score, not
            // on a human score.
            assertThat(history.getReason()).contains("điểm AI");
        });
    }

    @Test
    void should_close_assignments_on_the_papers_it_released() {
        givenPreview(pendingBlocked());
        AssessmentCandidateResult pending = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        givenResults(pending);
        AssessmentGradingAssignment open = AssessmentGradingAssignment.open(pending.getId(), UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, Instant.now(), adminId, null);
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
            .thenReturn(List.of(open));

        useCase.execute(confirmed());

        // The submission has left PENDING_REVIEW, so the INITIAL round can no longer handle it; if
        // it is not closed, the assignment hangs forever in the teacher's queue.
        assertThat(open.isCompleted()).isTrue();
        assertThat(open.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(open.getActiveResultId()).isNull();
    }

    @Test
    void should_leave_assignments_on_papers_it_did_not_touch() {
        // Vòng SPOT_CHECK trên bài RELEASED và vòng REMEDIATION trên bài INVALID vẫn còn
        // nguyên việc để làm — đóng chúng là xoá một quyết định chưa được đưa ra.
        givenResults(
            result(AssessmentCandidateResultStatus.RELEASED),
            result(AssessmentCandidateResultStatus.INVALID));

        useCase.execute(confirmed());

        verify(assessmentGradingAssignmentRepository, never()).findOpenByCandidateResultIdIn(anyCollection());
        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }
}
