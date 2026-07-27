package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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

import com.sep.vox.application.port.input.command.BulkFinalizeExamResultsCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.examgrading.BulkFinalizeExamResultsUseCase;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;

/**
 * Lối thoát cho tình trạng một bài treo chặn cả kỳ thi (review BE-5) — nhưng nó chỉ đưa
 * bài về {@code RELEASED}, KHÔNG bao giờ sinh {@code FINAL}, và không được đụng vào tranh
 * chấp điểm đang treo. Ba điều đó quan trọng ngang nhau.
 */
class BulkFinalizeExamResultsUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ExamResultStatusHistoryRepository examResultStatusHistoryRepository;
    private BulkFinalizeExamResultsUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        examResultStatusHistoryRepository = mock(ExamResultStatusHistoryRepository.class);
        useCase = new BulkFinalizeExamResultsUseCase(
            examRepository, examCandidateResultRepository, examGradingAssignmentRepository,
            examGradingQueryRepository, examGradingAccessService,
            new ResultStatusHistoryRecorder(examResultStatusHistoryRepository));

        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
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
        when(examGradingQueryRepository.previewBulkFinalize(schoolId, examId)).thenReturn(preview);
    }

    private void givenResults(ExamCandidateResult... results) {
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(results));
    }

    private ExamCandidateResult result(ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        result.setStatus(status);
        return result;
    }

    private BulkFinalizeExamResultsCommand confirmed() {
        return new BulkFinalizeExamResultsCommand(examId, true);
    }

    @SuppressWarnings("unchecked")
    private List<ExamResultStatusHistory> captureHistories() {
        var captor = (ArgumentCaptor<List<ExamResultStatusHistory>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(examResultStatusHistoryRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_refuse_when_papers_are_still_in_flight_and_the_admin_has_not_confirmed() {
        givenPreview(pendingBlocked());
        givenResults(result(ExamCandidateResultStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> useCase.execute(new BulkFinalizeExamResultsCommand(examId, false)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa ai chấm");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_while_an_appeal_is_still_open_even_when_the_admin_confirmed() {
        // Cờ "công bố theo điểm AI" chỉ nói về bài chưa ai chấm. Nuốt luôn một tranh
        // chấp điểm đang treo thì đơn kẹt mở vĩnh viễn.
        givenPreview(new BulkFinalizePreviewInfo(3, 2, 0, 0, 1, 0, List.of(UUID.randomUUID())));
        givenResults(result(ExamCandidateResultStatus.RELEASED));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("phúc khảo");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_when_a_paper_is_still_under_appeal() {
        givenResults(
            result(ExamCandidateResultStatus.APPEALED),
            result(ExamCandidateResultStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đơn phúc khảo");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_when_a_paper_is_being_regraded_for_an_appeal() {
        // Một giáo viên đang cầm bài chấm dở ở vòng APPEAL — chốt sổ không quyết thay.
        givenResults(result(ExamCandidateResultStatus.RE_GRADING));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chấm phúc khảo");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_name_every_blocking_status_in_one_message() {
        // Báo từng thứ một là bắt admin bấm lại nhiều lần mới biết còn gì phải xử lý.
        givenResults(
            result(ExamCandidateResultStatus.APPEALED),
            result(ExamCandidateResultStatus.RE_GRADING));

        assertThatThrownBy(() -> useCase.execute(confirmed()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContainingAll("đơn phúc khảo", "chấm phúc khảo");
    }

    @Test
    void should_release_pending_papers_on_ai_scores_once_the_admin_confirms() {
        givenPreview(pendingBlocked());
        var pending = result(ExamCandidateResultStatus.PENDING_REVIEW);
        givenResults(pending);

        var changed = useCase.execute(confirmed());

        assertThat(changed).isEqualTo(1);
        assertThat(pending.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(pending.getReleasedAt()).isNotNull();
        // Bài mới công bố thì chưa chung thẩm: publish mới là chỗ chốt PASSED/FAILED.
        assertThat(pending.getFinalizedAt()).isNull();
    }

    @Test
    void should_never_produce_final() {
        // FINAL là trạng thái HẬU publish (finalizeForPublish sinh ra khi policy không có
        // passingScore). Sinh nó ở đây thì requirePublishReadiness từ chối cả kỳ thi, mà
        // lối ra duy nhất khỏi FINAL lại đòi kỳ thi đã publish — deadlock kín.
        givenPreview(pendingBlocked());
        var pending = result(ExamCandidateResultStatus.PENDING_REVIEW);
        var released = result(ExamCandidateResultStatus.RELEASED);
        givenResults(pending, released);

        useCase.execute(confirmed());

        assertThat(pending.getStatus()).isNotEqualTo(ExamCandidateResultStatus.FINAL);
        assertThat(released.getStatus()).isNotEqualTo(ExamCandidateResultStatus.FINAL);
    }

    @Test
    void should_leave_released_invalid_and_already_decided_results_alone() {
        var released = result(ExamCandidateResultStatus.RELEASED);
        var invalid = result(ExamCandidateResultStatus.INVALID);
        var passed = result(ExamCandidateResultStatus.PASSED);
        givenResults(released, invalid, passed);

        var changed = useCase.execute(confirmed());

        // RELEASED đã sẵn sàng publish; INVALID đã có kết luận và sẽ tự thành FAILED
        // (điểm ép về 0) lúc kỳ thi công bố; PASSED đã chốt.
        assertThat(changed).isZero();
        assertThat(released.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(invalid.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(passed.getStatus()).isEqualTo(ExamCandidateResultStatus.PASSED);
    }

    @Test
    void should_keep_the_original_released_at_when_a_paper_already_had_one() {
        // Bài từng RELEASED rồi bị kéo về PENDING_REVIEW (gỡ vô hiệu) không được đặt lại
        // mốc công bố: cửa sổ phúc khảo tính từ mốc đó.
        givenPreview(pendingBlocked());
        var pending = result(ExamCandidateResultStatus.PENDING_REVIEW);
        var originalReleasedAt = OffsetDateTime.now().minusDays(3);
        pending.setReleasedAt(originalReleasedAt);
        givenResults(pending);

        useCase.execute(confirmed());

        assertThat(pending.getReleasedAt()).isEqualTo(originalReleasedAt);
    }

    @Test
    void should_record_which_papers_were_published_on_ai_scores() {
        givenPreview(pendingBlocked());
        givenResults(result(ExamCandidateResultStatus.PENDING_REVIEW));

        useCase.execute(confirmed());

        var histories = captureHistories();
        assertThat(histories).hasSize(1);
        assertThat(histories).allSatisfy(history -> {
            assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.ADMIN_BULK_FINALIZE);
            assertThat(history.getToStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
            // Phải phân biệt được về sau: bài này công bố theo điểm AI, không phải điểm người.
            assertThat(history.getReason()).contains("điểm AI");
        });
    }

    @Test
    void should_close_assignments_on_the_papers_it_released() {
        givenPreview(pendingBlocked());
        var pending = result(ExamCandidateResultStatus.PENDING_REVIEW);
        givenResults(pending);
        var open = ExamGradingAssignment.open(pending.getId(), UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, OffsetDateTime.now(), adminId, null);
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
            .thenReturn(List.of(open));

        useCase.execute(confirmed());

        // Bài đã rời PENDING_REVIEW nên vòng INITIAL không còn xử lý được nó nữa; không
        // đóng thì phân công treo vĩnh viễn trong hàng đợi của giáo viên.
        assertThat(open.isCompleted()).isTrue();
        assertThat(open.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(open.getActiveResultId()).isNull();
    }

    @Test
    void should_leave_assignments_on_papers_it_did_not_touch() {
        // Vòng SPOT_CHECK trên bài RELEASED và vòng REMEDIATION trên bài INVALID vẫn còn
        // nguyên việc để làm — đóng chúng là xoá một quyết định chưa được đưa ra.
        givenResults(
            result(ExamCandidateResultStatus.RELEASED),
            result(ExamCandidateResultStatus.INVALID));

        useCase.execute(confirmed());

        verify(examGradingAssignmentRepository, never()).findOpenByCandidateResultIdIn(anyCollection());
        verify(examGradingAssignmentRepository, never()).save(any());
    }
}
