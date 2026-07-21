package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.OffsetDateTime;
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
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

@Service
public class CreateExamAppealUseCase implements IUseCase<CreateExamAppealCommand, UUID> {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamResultAppealItemRepository examResultAppealItemRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public CreateExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamResultAppealItemRepository examResultAppealItemRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examResultAppealItemRepository = examResultAppealItemRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examAppealAccessService = examAppealAccessService;
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
        var paperItemIds = command.paperItemIds();
        if (paperItemIds == null || paperItemIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một phần thi cần phúc khảo.");
        }
        var distinctPaperItemIds = new LinkedHashSet<>(paperItemIds);
        if (distinctPaperItemIds.size() != paperItemIds.size()) {
            throw new IllegalArgumentException("Không được chọn trùng phần thi.");
        }

        // Điểm chấm lại được ghi theo response, nên phải chốt response ngay từ đầu.
        // MỘT lượt duyệt response của phiên thi, tránh N lần gọi repository.
        var responsesByPaperItem = examItemResponseRepository.findBySessionId(candidateResult.getSessionId())
            .stream()
            .filter(item -> item.getPaperItemId() != null)
            .collect(Collectors.toMap(
                ExamItemResponse::getPaperItemId, Function.identity(), (left, right) -> left));
        if (!responsesByPaperItem.keySet().containsAll(distinctPaperItemIds)) {
            throw new NotFoundException("Không tìm thấy câu trả lời của phần thi cần phúc khảo.");
        }

        var now = OffsetDateTime.now();
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
            null
        );
        var saved = examResultAppealRepository.save(appeal);

        // Id của đơn chỉ có sau khi INSERT trả về, nên phần thi phải lưu ở bước sau.
        examResultAppealItemRepository.saveAll(distinctPaperItemIds.stream()
            .map(paperItemId -> new ExamResultAppealItem(
                saved.getId(), paperItemId, responsesByPaperItem.get(paperItemId).getId(), null))
            .toList());

        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        examCandidateResultRepository.save(candidateResult);

        return saved.getId();
    }
}
