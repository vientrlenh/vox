package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentappeal.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealItemRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentAppealUseCase implements IUseCase<CreateAssessmentAppealCommand, UUID> {

    /**
     * Maximum number of appeal rounds for one result. Publishing an appeal returns the result to
     * RELEASED, so the status no longer blocks a later round by itself; this limit takes over that role.
     */
    static final int MAX_APPEAL_ROUNDS = 2;

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentResultAppealItemRepository assessmentResultAppealItemRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    @Override
    @Transactional
    public UUID execute(CreateAssessmentAppealCommand command) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.loadByCandidateResultId(command.candidateResultId());
        assessmentAppealAccessService.authorizeOwningStudent(context, currentUserId);

        AssessmentCandidateResult candidateResult = context.candidateResult();
        if (candidateResult.getStatus() != AssessmentCandidateResultStatus.RELEASED) {
            throw new IllegalArgumentException("Chỉ có thể phúc khảo kết quả đã được công bố.");
        }
        if (assessmentResultAppealRepository.existsOpenByCandidateResultId(command.candidateResultId())) {
            throw new ResourceDuplicatedException("Đã có đơn phúc khảo đang được xử lý cho kết quả này.");
        }
        if (assessmentResultAppealRepository.countPublishedByCandidateResultId(command.candidateResultId())
                >= MAX_APPEAL_ROUNDS) {
            throw new IllegalArgumentException(
                "Mỗi kết quả chỉ được phúc khảo tối đa " + MAX_APPEAL_ROUNDS + " lần.");
        }
        // Trùng phần thi là lỗi của client, phát hiện trước khi chạm DB.
        List<UUID> requested = command.paperItemIds();
        LinkedHashSet<UUID> selected = null;
        if (requested != null) {
            selected = new LinkedHashSet<>(requested);
            if (selected.size() != requested.size()) {
                throw new IllegalArgumentException("Không được chọn trùng phần thi.");
            }
        }

        // Điểm chấm lại được ghi theo response, nên phải chốt response ngay từ đầu.
        // MỘT lượt duyệt response của phiên thi, tránh N lần gọi repository.
        Map<UUID, AssessmentItemResponse> responsesByPaperItem = assessmentItemResponseRepository.findBySessionId(candidateResult.getSessionId())
            .stream()
            .filter(item -> item.getPaperItemId() != null)
            .collect(Collectors.toMap(
                item -> item.getPaperItemId(), Function.identity(), (left, right) -> left,
                LinkedHashMap::new));   // giữ thứ tự response cho phần tự điền

        final LinkedHashSet<UUID> distinctPaperItemIds;
        if (selected == null || selected.isEmpty()) {
            // Selecting no question = appeal the whole attempt. It still writes assessment_result_appeal_items for
            // each question: the comparison screen of the school administrator takes baselineScores + turns from exactly
            // those rows, and dropping them would lose the comparison data.
            if (responsesByPaperItem.isEmpty()) {
                throw new ResourceNotFoundException("Bài thi này không có câu trả lời nào để phúc khảo.");
            }
            distinctPaperItemIds = new LinkedHashSet<>(responsesByPaperItem.keySet());
        } else {
            distinctPaperItemIds = selected;
            if (!responsesByPaperItem.keySet().containsAll(distinctPaperItemIds)) {
                throw new ResourceNotFoundException("Không tìm thấy câu trả lời của phần thi cần phúc khảo.");
            }
        }

        Instant now = Instant.now();
        AssessmentResultAppeal appeal = AssessmentResultAppeal.builder()
                .candidateResultId(command.candidateResultId())
                .requestedBy(currentUserId)
                .reason(command.reason())
                .requestedAt(now)
                .status(AssessmentAppealStatus.PENDING)
                .scoreBefore(candidateResult.getTotalScore())
                .scoreAfter(null)
                .resolvedBy(null)
                .resolvedAt(null)
                .notes(command.notes())
                .deadline(null)
                .approvedAt(null)
                .decisionNote(null)
                .withdrawnAt(null)
                .reviewerOverrideReason(null)
                .build();
        AssessmentResultAppeal saved = assessmentResultAppealRepository.save(appeal);

        // Id của đơn chỉ có sau khi INSERT trả về, nên phần thi phải lưu ở bước sau.
        assessmentResultAppealItemRepository.saveAll(distinctPaperItemIds.stream()
            .map(paperItemId -> AssessmentResultAppealItem.builder()
                    .appealId(saved.getId())
                    .paperItemId(paperItemId)
                    .responseId(responsesByPaperItem.get(paperItemId).getId())
                    .finalScore(null)
                    .build())
            .toList());

        // Đóng vòng chấm đang mở (thường là hậu kiểm) trước khi kéo bài sang APPEALED.
        // Bỏ qua bước này chính là review BE-4: bài rời khỏi trạng thái mà vòng đó xử
        // lý được, phân công treo vĩnh viễn trong hàng đợi của giáo viên và
        // active_result_id vẫn giữ chỗ nên không giao được người chấm phúc khảo.
        //
        // Đóng bằng DECLINED chứ không phải UPHELD: giáo viên chưa ra quyết định nào,
        // ghi UPHELD là bịa ra một phán quyết mà họ không hề đưa.
        assessmentGradingAssignmentRepository.findOpenByCandidateResultId(command.candidateResultId())
            .ifPresent(open -> {
                open.complete(GradingOutcome.DECLINED,
                    "Bài chuyển sang phúc khảo theo đơn của học sinh.", now);
                assessmentGradingAssignmentRepository.save(open);
            });

        ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);
        assessmentCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(before, candidateResult,
            ResultStatusChangeSource.SYSTEM, currentUserId, "Học sinh nộp đơn phúc khảo.");

        return saved.getId();
    }
}
