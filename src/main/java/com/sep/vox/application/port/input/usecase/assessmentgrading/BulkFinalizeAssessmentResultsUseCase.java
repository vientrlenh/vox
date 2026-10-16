package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.BulkFinalizeAssessmentResultsCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.AssessmentGradingOutcome;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

/**
 * Chốt sổ: đưa bài còn chờ người chấm ({@code PENDING_REVIEW}) của một kỳ thi về
 * {@code RELEASED} theo điểm AI đang có, để kỳ thi công bố được.
 *
 * <p>Đây là lối thoát cho tình trạng đúng MỘT bài chưa ai chấm mà chặn cả kỳ thi
 * không công bố được (review BE-5): {@code finalizeForPublish} bỏ qua bài
 * {@code PENDING_REVIEW}, nên trước đây kỳ thi kẹt vô thời hạn và không có nút nào gỡ.
 *
 * <p><strong>Use case này KHÔNG sinh {@code FINAL}.</strong> {@code FINAL} là trạng thái
 * <em>hậu</em> publish — {@code finalizeForPublish} đặt nó khi policy không có
 * {@code passingScore} và nhà trường phải tự quyết đậu/rớt. Sinh nó ở đây thì
 * {@code requirePublishReadiness} (chỉ nhận {@code RELEASED}/{@code INVALID}) từ chối cả
 * kỳ thi, mà lối ra duy nhất khỏi {@code FINAL} —
 * {@code DecideAssessmentCandidateResultOutcomeUseCase} — lại đòi kỳ thi đã publish: deadlock
 * kín, chỉ gỡ được bằng SQL tay. Chốt sổ vì thế chỉ đưa bài về đúng hai trạng thái mà
 * publish chấp nhận, rồi để publish tự lo phần chung thẩm.
 *
 * <p>Ba lớp bảo vệ để nó không thành nút "công bố bừa":
 * <ol>
 *   <li>Tranh chấp điểm đang treo là điều kiện chặn CỨNG, không cờ nào bỏ qua được:
 *       đơn phúc khảo còn mở, và bài đang ở {@code APPEALED}/{@code RE_GRADING} — ở
 *       {@code RE_GRADING} còn có một giáo viên đang cầm bài chấm dở.
 *   <li>Preview bắt buộc — nếu còn bài chặn mà admin chưa xác nhận
 *       {@code releasePendingWithAiScores}, use case từ chối và nói rõ còn bao nhiêu bài.
 *   <li>Mọi lần đổi trạng thái đều vào nhật ký với nguồn {@code ADMIN_BULK_FINALIZE},
 *       nên về sau tra ra được bài nào được công bố theo điểm AI chứ không phải điểm người.
 * </ol>
 *
 * <p>Bài {@code RELEASED} đã sẵn sàng publish nên không phải đụng tới. Bài {@code INVALID}
 * giữ nguyên: chúng đã có kết luận, và {@code finalizeForPublish} sẽ tự đưa về
 * {@code FAILED} (điểm ép về 0) khi kỳ thi công bố — kéo chúng qua bất kỳ trạng thái nào
 * khác là mở đường cho một bài vi phạm đi ra với kết quả {@code PASSED}.
 */
@Service
public class BulkFinalizeAssessmentResultsUseCase
        implements IUseCase<BulkFinalizeAssessmentResultsCommand, Integer> {

    private static final String BULK_REASON = "Chốt sổ hàng loạt theo kỳ thi.";
    private static final String BULK_AI_REASON =
        "Chốt sổ hàng loạt: công bố theo điểm AI vì chưa có người chấm.";

    /**
     * Tranh chấp điểm đang treo — chặn cả lượt chốt sổ, không cờ nào bỏ qua được.
     *
     * <p>Với dữ liệu nhất quán thì {@code preview.openAppeals()} đã chặn trước rồi (bài chỉ
     * ở hai trạng thái này khi có đơn đang mở). Kiểm ở đây là lưới cho dữ liệu lệch theo
     * chiều còn lại — đơn đã đóng mà bài không được kéo về {@code RELEASED} — thứ trước đây
     * bị {@code FINAL} lấp liếm thay vì để lộ ra.
     */
    private static final Set<AssessmentCandidateResultStatus> BLOCKING_STATUSES = EnumSet.of(
        AssessmentCandidateResultStatus.APPEALED,
        AssessmentCandidateResultStatus.RE_GRADING);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    public BulkFinalizeAssessmentResultsUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
    }

    @Override
    @Transactional
    public Integer execute(BulkFinalizeAssessmentResultsCommand command) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
        // Bài kiểm tra trên lớp: nhà trường không chấm, nên nếu chỉ school admin chốt
        // sổ được thì giáo viên kẹt cứng — công bố kết quả đòi MỌI bài phải RELEASED
        // hoặc INVALID (UpdateAssessmentStatusUseCase.requirePublishReadiness).
        assessmentGradingAccessService.authorizeSchoolAdminOrClassTestChair(
            assessment.getSchoolId(), assessment.getId(), currentUserId);

        var preview = assessmentGradingQueryRepository.previewBulkFinalize(assessment.getSchoolId(), command.assessmentId());
        // Đơn phúc khảo đang mở thì LUÔN chặn, kể cả khi admin đã tick "công bố theo
        // điểm hiện có": cờ đó chỉ nói "chấp nhận điểm AI cho bài chưa ai chấm", không
        // nói "nuốt luôn tranh chấp điểm của học sinh". Nuốt đơn còn để lại hai hậu
        // quả: đơn treo mở vĩnh viễn, và học sinh rút đơn sau đó lại kéo bài về RELEASED
        // trong khi kỳ thi đã công bố xong.
        if (preview.openAppeals() > 0) {
            throw new IllegalStateException(
                "Kỳ thi còn " + preview.openAppeals() + " đơn phúc khảo chưa xong. "
                    + "Hãy xử lý hoặc từ chối các đơn này trước khi chốt sổ.");
        }

        var results = assessmentCandidateResultRepository.findByAssessmentId(command.assessmentId());
        var blocking = countBlocking(results);
        if (!blocking.isEmpty()) {
            throw new IllegalStateException(describeBlocking(blocking));
        }
        if (!preview.isClean() && !command.releasePendingWithAiScores()) {
            throw new IllegalStateException(
                "Kỳ thi còn " + preview.pendingUnassigned() + " bài chưa ai chấm và "
                    + preview.pendingAssigned() + " bài đang chấm dở. "
                    + "Hãy xử lý nốt, hoặc xác nhận công bố theo điểm AI hiện có.");
        }

        var now = Instant.now();
        var histories = new ArrayList<AssessmentResultStatusHistory>();
        var released = new ArrayList<AssessmentCandidateResult>();

        for (var result : results) {
            if (result.getStatus() != AssessmentCandidateResultStatus.PENDING_REVIEW) {
                continue;
            }

            result.setStatus(AssessmentCandidateResultStatus.RELEASED);
            // Bài từng RELEASED rồi bị kéo về PENDING_REVIEW (gỡ vô hiệu) giữ nguyên mốc
            // công bố cũ: cửa sổ phúc khảo của học sinh tính từ mốc đó.
            if (result.getReleasedAt() == null) {
                result.setReleasedAt(now);
            }
            result.setUpdatedAt(now);
            result.setUpdatedBy(currentUserId);
            assessmentCandidateResultRepository.save(result);
            released.add(result);

            histories.add(new AssessmentResultStatusHistory(
                result.getId(),
                AssessmentCandidateResultStatus.PENDING_REVIEW,
                AssessmentCandidateResultStatus.RELEASED,
                result.getTotalScore(),
                result.getTotalScore(),
                AssessmentResultStatusChangeSource.ADMIN_BULK_FINALIZE,
                currentUserId,
                BULK_AI_REASON,
                now
            ));
        }

        closeSettledAssignments(released, now);
        resultStatusHistoryRecorder.recordAll(List.copyOf(histories));
        return released.size();
    }

    /**
     * Đóng phân công còn mở trên các bài VỪA được công bố. Bài đã rời
     * {@code PENDING_REVIEW} nên vòng {@code INITIAL} không còn xử lý được nó nữa
     * ({@code GradingRoundPolicy.isAssignable} trả false); không đóng thì phân công treo
     * vĩnh viễn trong hàng đợi của giáo viên.
     *
     * <p>Chỉ đụng đúng những bài use case này chuyển trạng thái. Vòng {@code SPOT_CHECK}
     * trên bài {@code RELEASED} và vòng {@code REMEDIATION} trên bài {@code INVALID} vẫn
     * còn nguyên việc để làm — đóng chúng là xoá một quyết định chưa được đưa ra.
     */
    private void closeSettledAssignments(List<AssessmentCandidateResult> released, Instant now) {
        if (released.isEmpty()) {
            return;
        }
        var openAssignments = assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(
            released.stream().map(result -> result.getId()).toList());
        for (var assignment : openAssignments) {
            assignment.complete(AssessmentGradingOutcome.DECLINED, BULK_REASON, now);
            assessmentGradingAssignmentRepository.save(assignment);
        }
    }

    /** {@code EnumMap} để thứ tự kể trong thông báo bám theo thứ tự khai báo của enum. */
    private Map<AssessmentCandidateResultStatus, Integer> countBlocking(List<AssessmentCandidateResult> results) {
        var counts = new EnumMap<AssessmentCandidateResultStatus, Integer>(AssessmentCandidateResultStatus.class);
        for (var result : results) {
            var status = result.getStatus();
            if (BLOCKING_STATUSES.contains(status)) {
                var current = counts.get(status);
                counts.put(status, current == null ? 1 : current + 1);
            }
        }
        return counts;
    }

    /**
     * Kể HẾT mọi thứ đang chặn trong một thông báo: báo từng thứ một là bắt admin bấm lại
     * nhiều lần mới biết còn gì phải xử lý.
     */
    private String describeBlocking(Map<AssessmentCandidateResultStatus, Integer> counts) {
        var parts = new ArrayList<String>();
        counts.forEach((status, count) -> parts.add(switch (status) {
            case APPEALED -> count + " bài có đơn phúc khảo chờ xử lý";
            case RE_GRADING -> count + " bài đang được chấm phúc khảo";
            default -> count + " bài ở trạng thái " + status.name();
        }));
        return "Kỳ thi còn " + String.join(", ", parts)
            + ". Chốt sổ không quyết thay tranh chấp điểm — hãy xử lý dứt điểm các bài này trước.";
    }
}
