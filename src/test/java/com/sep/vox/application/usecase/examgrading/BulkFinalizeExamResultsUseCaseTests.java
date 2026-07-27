package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * Lối thoát cho tình trạng một bài treo chặn cả kỳ thi (review BE-5) — nhưng nó không
 * được biến thành nút "công bố bừa", nên hai lớp bảo vệ ở đây quan trọng ngang nhau.
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
    }

    private BulkFinalizePreviewInfo clean() {
        return new BulkFinalizePreviewInfo(3, 3, 0, 0, 0, 0, List.of());
    }

    private BulkFinalizePreviewInfo blocked() {
        return new BulkFinalizePreviewInfo(3, 1, 1, 1, 0, 0, List.of(UUID.randomUUID()));
    }

    private void givenPreview(BulkFinalizePreviewInfo preview) {
        when(examGradingQueryRepository.previewBulkFinalize(schoolId, examId)).thenReturn(preview);
    }

    private ExamCandidateResult result(ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        result.setStatus(status);
        return result;
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
        givenPreview(blocked());

        assertThatThrownBy(() -> useCase.execute(new BulkFinalizeExamResultsCommand(examId, false)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa ai chấm");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_refuse_while_an_appeal_is_still_open_even_when_the_admin_confirmed() {
        givenPreview(new BulkFinalizePreviewInfo(3, 2, 0, 0, 1, 0, List.of(UUID.randomUUID())));

        // Cờ "công bố theo điểm hiện có" chỉ nói về bài chưa ai chấm. Nuốt luôn một
        // tranh chấp điểm đang treo thì đơn kẹt mở vĩnh viễn, và học sinh rút đơn sau
        // đó kéo bài từ FINAL ngược về RELEASED.
        assertThatThrownBy(() -> useCase.execute(new BulkFinalizeExamResultsCommand(examId, true)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("phúc khảo");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_proceed_once_the_admin_confirms_publishing_ai_scores() {
        givenPreview(blocked());
        when(examCandidateResultRepository.findByExamId(examId))
            .thenReturn(List.of(result(ExamCandidateResultStatus.PENDING_REVIEW)));

        var changed = useCase.execute(new BulkFinalizeExamResultsCommand(examId, true));

        assertThat(changed).isEqualTo(1);
    }

    @Test
    void should_move_open_statuses_to_final() {
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(
            result(ExamCandidateResultStatus.PENDING_REVIEW),
            result(ExamCandidateResultStatus.RELEASED),
            result(ExamCandidateResultStatus.APPEALED)));

        assertThat(useCase.execute(new BulkFinalizeExamResultsCommand(examId, true))).isEqualTo(3);
    }

    @Test
    void should_leave_invalid_and_already_decided_results_alone() {
        var invalid = result(ExamCandidateResultStatus.INVALID);
        var passed = result(ExamCandidateResultStatus.PASSED);
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(invalid, passed));

        var changed = useCase.execute(new BulkFinalizeExamResultsCommand(examId, true));

        // INVALID đã có kết luận và sẽ tự thành FAILED lúc kỳ thi công bố; PASSED đã chốt.
        assertThat(changed).isZero();
        assertThat(invalid.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(passed.getStatus()).isEqualTo(ExamCandidateResultStatus.PASSED);
    }

    @Test
    void should_record_which_papers_were_published_on_ai_scores() {
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(
            result(ExamCandidateResultStatus.PENDING_REVIEW),
            result(ExamCandidateResultStatus.RELEASED)));

        useCase.execute(new BulkFinalizeExamResultsCommand(examId, true));

        var histories = captureHistories();
        assertThat(histories).hasSize(2);
        assertThat(histories).allSatisfy(history ->
            assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.ADMIN_BULK_FINALIZE));
        // Bài chưa ai chấm phải phân biệt được về sau: nó được công bố theo điểm AI.
        assertThat(histories).anySatisfy(history ->
            assertThat(history.getReason()).contains("điểm AI"));
    }

    @Test
    void should_close_assignments_that_have_nothing_left_to_do() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW);
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(result));
        var open = ExamGradingAssignment.open(result.getId(), UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, java.time.OffsetDateTime.now(), adminId, null);
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
            .thenReturn(List.of(open));

        useCase.execute(new BulkFinalizeExamResultsCommand(examId, true));

        // Không đóng thì phân công treo vĩnh viễn trong hàng đợi của giáo viên.
        assertThat(open.isCompleted()).isTrue();
        assertThat(open.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(open.getActiveResultId()).isNull();
    }
}
