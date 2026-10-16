package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.domain.model.assessment.AssessmentGradingOutcome;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;

@Service
public class CreateAssessmentAppealUseCase implements IUseCase<CreateAssessmentAppealCommand, UUID> {

    /**
     * Số vòng phúc khảo tối đa cho một kết quả. Công bố phúc khảo trả kết quả về
     * RELEASED nên trạng thái không còn tự chặn vòng sau — hạn mức này thay vai trò đó.
     */
    static final int MAX_APPEAL_ROUNDS = 2;

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentResultAppealItemRepository assessmentResultAppealItemRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    public CreateAssessmentAppealUseCase(
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentResultAppealItemRepository assessmentResultAppealItemRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentItemAnswerRepository assessmentItemAnswerRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentAppealAccessService assessmentAppealAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder) {
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentResultAppealItemRepository = assessmentResultAppealItemRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
    }

    @Override
    @Transactional
    public UUID execute(CreateAssessmentAppealCommand command) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var context = assessmentAppealAccessService.loadByCandidateResultId(command.candidateResultId());
        assessmentAppealAccessService.authorizeOwningStudent(context, currentUserId);

        var candidateResult = context.candidateResult();
        if (candidateResult.getStatus() != AssessmentCandidateResultStatus.RELEASED) {
            throw new IllegalStateException("Chỉ có thể phúc khảo kết quả đã được công bố.");
        }
        if (assessmentResultAppealRepository.existsOpenByCandidateResultId(command.candidateResultId())) {
            throw new ResourceDuplicatedException("Đã có đơn phúc khảo đang được xử lý cho kết quả này.");
        }
        if (assessmentResultAppealRepository.countPublishedByCandidateResultId(command.candidateResultId())
                >= MAX_APPEAL_ROUNDS) {
            throw new IllegalStateException(
                "Mỗi kết quả chỉ được phúc khảo tối đa " + MAX_APPEAL_ROUNDS + " lần.");
        }
        // Trùng phần thi là lỗi của client, phát hiện trước khi chạm DB.
        var requested = command.paperItemIds();
        LinkedHashSet<UUID> selected = null;
        if (requested != null) {
            selected = new LinkedHashSet<>(requested);
            if (selected.size() != requested.size()) {
                throw new IllegalArgumentException("Không được chọn trùng phần thi.");
            }
        }

        // Điểm chấm lại được ghi theo response, nên phải chốt response ngay từ đầu.
        // MỘT lượt duyệt response của phiên thi, tránh N lần gọi repository.
        var responsesByPaperItem = assessmentItemAnswerRepository.findBySessionId(candidateResult.getSessionId())
            .stream()
            .filter(item -> item.getPaperItemId() != null)
            .collect(Collectors.toMap(
                item -> item.getPaperItemId(), Function.identity(), (left, right) -> left,
                LinkedHashMap::new));   // giữ thứ tự response cho phần tự điền

        final LinkedHashSet<UUID> distinctPaperItemIds;
        if (selected == null || selected.isEmpty()) {
            // Không chọn câu nào = phúc khảo toàn bài. Vẫn ghi assessment_result_appeal_items cho
            // từng câu: màn đối chiếu của quản trị trường lấy baselineScores + turns từ chính
            // các dòng đó, bỏ đi là mất dữ liệu so sánh.
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

        var now = Instant.now();
        var appeal = new AssessmentResultAppeal(
            command.candidateResultId(),
            currentUserId,
            command.reason(),
            now,
            AssessmentAppealStatus.PENDING,
            candidateResult.getTotalScore(),
            null,
            null,
            null,
            command.notes(),
            null,
            null,
            null,
            null,
            null
        );
        var saved = assessmentResultAppealRepository.save(appeal);

        // Id của đơn chỉ có sau khi INSERT trả về, nên phần thi phải lưu ở bước sau.
        assessmentResultAppealItemRepository.saveAll(distinctPaperItemIds.stream()
            .map(paperItemId -> new AssessmentResultAppealItem(
                saved.getId(), paperItemId, responsesByPaperItem.get(paperItemId).getId(), null))
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
                open.complete(AssessmentGradingOutcome.DECLINED,
                    "Bài chuyển sang phúc khảo theo đơn của học sinh.", now);
                assessmentGradingAssignmentRepository.save(open);
            });

        var before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        assessmentCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(before, candidateResult,
            AssessmentResultStatusChangeSource.SYSTEM, currentUserId, "Học sinh nộp đơn phúc khảo.");

        return saved.getId();
    }
}
