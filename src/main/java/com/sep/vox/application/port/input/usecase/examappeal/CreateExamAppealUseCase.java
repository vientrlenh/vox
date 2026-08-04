package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

@Service
public class CreateExamAppealUseCase implements IUseCase<CreateExamAppealCommand, UUID> {

    /**
     * Số vòng phúc khảo tối đa cho một kết quả. Công bố phúc khảo trả kết quả về
     * RELEASED nên trạng thái không còn tự chặn vòng sau — hạn mức này thay vai trò đó.
     */
    static final int MAX_APPEAL_ROUNDS = 2;

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamResultAppealItemRepository examResultAppealItemRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    public CreateExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamResultAppealItemRepository examResultAppealItemRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamAppealAccessService examAppealAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examResultAppealItemRepository = examResultAppealItemRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
    }

    @Override
    @Transactional
    public UUID execute(CreateExamAppealCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.loadByCandidateResultId(command.candidateResultId());
        examAppealAccessService.authorizeOwningStudent(context, currentUserId);

        var candidateResult = context.candidateResult();
        if (candidateResult.getStatus() != ExamCandidateResultStatus.RELEASED) {
            throw new IllegalStateException("Chỉ có thể phúc khảo kết quả đã được công bố.");
        }
        if (examResultAppealRepository.existsOpenByCandidateResultId(command.candidateResultId())) {
            throw new DuplicatedException("Đã có đơn phúc khảo đang được xử lý cho kết quả này.");
        }
        if (examResultAppealRepository.countPublishedByCandidateResultId(command.candidateResultId())
                >= MAX_APPEAL_ROUNDS) {
            throw new IllegalStateException(
                "Mỗi kết quả chỉ được phúc khảo tối đa " + MAX_APPEAL_ROUNDS + " lần.");
        }
        // Trùng phần thi là lỗi của client, phát hiện trước khi chạm DB.
        var requested = command.paperItemIds();
        LinkedHashSet<UUID> selected = requested == null ? null : new LinkedHashSet<>(requested);
        if (selected != null && selected.size() != requested.size()) {
            throw new IllegalArgumentException("Không được chọn trùng phần thi.");
        }

        // Điểm chấm lại được ghi theo response, nên phải chốt response ngay từ đầu.
        // MỘT lượt duyệt response của phiên thi, tránh N lần gọi repository.
        var responsesByPaperItem = examItemResponseRepository.findBySessionId(candidateResult.getSessionId())
            .stream()
            .filter(item -> item.getPaperItemId() != null)
            .collect(Collectors.toMap(
                item -> item.getPaperItemId(), Function.identity(), (left, right) -> left,
                LinkedHashMap::new));   // giữ thứ tự response cho phần tự điền

        final LinkedHashSet<UUID> distinctPaperItemIds;
        if (selected == null || selected.isEmpty()) {
            // Không chọn câu nào = phúc khảo toàn bài. Vẫn ghi exam_result_appeal_items cho
            // từng câu: màn đối chiếu của quản trị trường lấy baselineScores + turns từ chính
            // các dòng đó, bỏ đi là mất dữ liệu so sánh.
            if (responsesByPaperItem.isEmpty()) {
                throw new NotFoundException("Bài thi này không có câu trả lời nào để phúc khảo.");
            }
            distinctPaperItemIds = new LinkedHashSet<>(responsesByPaperItem.keySet());
        } else {
            distinctPaperItemIds = selected;
            if (!responsesByPaperItem.keySet().containsAll(distinctPaperItemIds)) {
                throw new NotFoundException("Không tìm thấy câu trả lời của phần thi cần phúc khảo.");
            }
        }

        var now = Instant.now();
        var appeal = new ExamResultAppeal(
            command.candidateResultId(),
            currentUserId,
            command.reason(),
            now,
            ExamAppealStatus.PENDING,
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
        var saved = examResultAppealRepository.save(appeal);

        // Id của đơn chỉ có sau khi INSERT trả về, nên phần thi phải lưu ở bước sau.
        examResultAppealItemRepository.saveAll(distinctPaperItemIds.stream()
            .map(paperItemId -> new ExamResultAppealItem(
                saved.getId(), paperItemId, responsesByPaperItem.get(paperItemId).getId(), null))
            .toList());

        // Đóng vòng chấm đang mở (thường là hậu kiểm) trước khi kéo bài sang APPEALED.
        // Bỏ qua bước này chính là review BE-4: bài rời khỏi trạng thái mà vòng đó xử
        // lý được, phân công treo vĩnh viễn trong hàng đợi của giáo viên và
        // active_result_id vẫn giữ chỗ nên không giao được người chấm phúc khảo.
        //
        // Đóng bằng DECLINED chứ không phải UPHELD: giáo viên chưa ra quyết định nào,
        // ghi UPHELD là bịa ra một phán quyết mà họ không hề đưa.
        examGradingAssignmentRepository.findOpenByCandidateResultId(command.candidateResultId())
            .ifPresent(open -> {
                open.complete(GradingOutcome.DECLINED,
                    "Bài chuyển sang phúc khảo theo đơn của học sinh.", now);
                examGradingAssignmentRepository.save(open);
            });

        var before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        examCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(before, candidateResult,
            ResultStatusChangeSource.SYSTEM, currentUserId, "Học sinh nộp đơn phúc khảo.");

        return saved.getId();
    }
}
