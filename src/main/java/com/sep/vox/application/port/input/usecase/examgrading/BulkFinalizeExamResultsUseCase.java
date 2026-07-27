package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkFinalizeExamResultsCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Chốt sổ: đưa toàn bộ kết quả còn dở của một kỳ thi về {@code FINAL}.
 *
 * <p>Đây là lối thoát cho tình trạng đúng MỘT bài chưa ai chấm mà chặn cả kỳ thi
 * không công bố được (review BE-5): {@code finalizeForPublish} bỏ qua bài
 * {@code PENDING_REVIEW}, nên trước đây kỳ thi kẹt vô thời hạn và không có nút nào gỡ.
 *
 * <p>Ba lớp bảo vệ để nó không thành nút "công bố bừa":
 * <ol>
 *   <li>Đơn phúc khảo đang mở là điều kiện chặn CỨNG, không cờ nào bỏ qua được — chốt
 *       sổ không được phép quyết thay một tranh chấp điểm đang treo.
 *   <li>Preview bắt buộc — nếu còn bài chặn mà admin chưa xác nhận
 *       {@code releasePendingWithAiScores}, use case từ chối và nói rõ còn bao nhiêu bài.
 *   <li>Mọi lần đổi trạng thái đều vào nhật ký với nguồn {@code ADMIN_BULK_FINALIZE},
 *       nên về sau tra ra được bài nào được công bố theo điểm AI chứ không phải điểm người.
 * </ol>
 *
 * <p>Bài {@code INVALID} giữ nguyên: chúng đã có kết luận, và
 * {@code finalizeForPublish} sẽ tự đưa về {@code FAILED} khi kỳ thi công bố.
 */
@Service
public class BulkFinalizeExamResultsUseCase
        implements IUseCase<BulkFinalizeExamResultsCommand, Integer> {

    private static final String BULK_REASON = "Chốt sổ hàng loạt theo kỳ thi.";
    private static final String BULK_AI_REASON =
        "Chốt sổ hàng loạt: công bố theo điểm AI vì chưa có người chấm.";

    private final ExamRepository examRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    public BulkFinalizeExamResultsUseCase(
            ExamRepository examRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder) {
        this.examRepository = examRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
    }

    @Override
    @Transactional
    public Integer execute(BulkFinalizeExamResultsCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        examGradingAccessService.authorizeSchoolAdmin(exam.getSchoolId(), currentUserId);

        var preview = examGradingQueryRepository.previewBulkFinalize(exam.getSchoolId(), command.examId());
        // Đơn phúc khảo đang mở thì LUÔN chặn, kể cả khi admin đã tick "công bố theo
        // điểm hiện có": cờ đó chỉ nói "chấp nhận điểm AI cho bài chưa ai chấm", không
        // nói "nuốt luôn tranh chấp điểm của học sinh". Nuốt đơn còn để lại hai hậu
        // quả: đơn treo mở vĩnh viễn, và học sinh rút đơn sau đó kéo bài từ FINAL
        // ngược về RELEASED.
        if (preview.openAppeals() > 0) {
            throw new IllegalStateException(
                "Kỳ thi còn " + preview.openAppeals() + " đơn phúc khảo chưa xong. "
                    + "Hãy xử lý hoặc từ chối các đơn này trước khi chốt sổ.");
        }
        if (!preview.isClean() && !command.releasePendingWithAiScores()) {
            throw new IllegalStateException(
                "Kỳ thi còn " + preview.pendingUnassigned() + " bài chưa ai chấm và "
                    + preview.pendingAssigned() + " bài đang chấm dở. "
                    + "Hãy xử lý nốt, hoặc xác nhận công bố theo điểm hiện có.");
        }

        var results = examCandidateResultRepository.findByExamId(command.examId());
        var now = OffsetDateTime.now();
        var histories = new ArrayList<ExamResultStatusHistory>();
        var changed = 0;

        for (var result : results) {
            var from = result.getStatus();
            if (from != ExamCandidateResultStatus.PENDING_REVIEW
                    && from != ExamCandidateResultStatus.RELEASED
                    && from != ExamCandidateResultStatus.APPEALED
                    && from != ExamCandidateResultStatus.RE_GRADING) {
                continue;
            }

            result.setStatus(ExamCandidateResultStatus.FINAL);
            result.setFinalizedAt(now);
            result.setUpdatedAt(now);
            result.setUpdatedBy(currentUserId);
            examCandidateResultRepository.save(result);
            changed++;

            histories.add(new ExamResultStatusHistory(
                result.getId(),
                from,
                ExamCandidateResultStatus.FINAL,
                result.getTotalScore(),
                result.getTotalScore(),
                ResultStatusChangeSource.ADMIN_BULK_FINALIZE,
                currentUserId,
                from == ExamCandidateResultStatus.PENDING_REVIEW ? BULK_AI_REASON : BULK_REASON,
                now
            ));
        }

        // Phân công còn mở của kỳ thi này không còn việc để làm — đóng lại để chúng
        // không treo vĩnh viễn trong hàng đợi của giáo viên.
        var openAssignments = examGradingAssignmentRepository.findOpenByCandidateResultIdIn(
            results.stream().map(result -> result.getId()).toList());
        for (var assignment : openAssignments) {
            assignment.complete(GradingOutcome.DECLINED, BULK_REASON, now);
            examGradingAssignmentRepository.save(assignment);
        }

        resultStatusHistoryRecorder.recordAll(List.copyOf(histories));
        return changed;
    }
}
