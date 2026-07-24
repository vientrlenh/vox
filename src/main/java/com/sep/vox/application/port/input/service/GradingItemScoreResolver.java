package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

/**
 * Validate + quy đổi điểm tiêu chí thành điểm phần thi.
 *
 * <p>Tách riêng vì {@code SubmitGradingUseCase} và {@code PreviewGradingUseCase}
 * BẮT BUỘC đi qua đúng một đường: nếu preview và submit tính khác nhau dù chỉ ở
 * chỗ làm tròn, con số giáo viên thấy trước khi bấm Nộp sẽ không phải con số học
 * sinh nhận — đó chính là lý do endpoint preview tồn tại.
 */
@Service
public class GradingItemScoreResolver {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final RubricCriterionRepository rubricCriterionRepository;

    public GradingItemScoreResolver(
            ExamItemResponseRepository examItemResponseRepository,
            RubricCriterionRepository rubricCriterionRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
    }

    /** Một phần thi đã chấm xong, sẵn sàng ghi xuống hoặc đem đi tính thử. */
    public record ResolvedItem(
        UUID paperItemId,
        UUID responseId,
        BigDecimal itemScore,
        String feedbackSummary,
        List<SubmitGradingCommand.CriterionScoreItem> criterionScores
    ) {
    }

    /**
     * @param enforceFullCoverage khi true (luồng nộp thật): bắt buộc phủ đủ MỌI phần
     *        thi của bài. Preview để false vì cố tình chấm dở để xem tổng chạy dần.
     *        Chốt COMPLETED chỉ an toàn khi đã phủ đủ — nộp thiếu phần rồi chốt sẽ
     *        khóa cứng các phần chưa chấm (không gỡ được bài đã COMPLETED).
     */
    @Transactional(readOnly = true)
    public List<ResolvedItem> resolve(
            GradingContext context, SubmitGradingCommand command, boolean enforceFullCoverage) {
        var items = command.items() == null ? List.<SubmitGradingCommand.ItemGrade>of() : command.items();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Phải chấm điểm cho ít nhất một phần thi.");
        }
        var paperItemIds = items.stream().map(SubmitGradingCommand.ItemGrade::paperItemId).toList();
        if (paperItemIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Thiếu phần thi cần chấm.");
        }
        if (new HashSet<>(paperItemIds).size() != paperItemIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng phần thi.");
        }

        var responsesByPaperItemId = examItemResponseRepository
            .findBySessionId(context.candidateResult().getSessionId()).stream()
            .collect(Collectors.toMap(ExamItemResponse::getPaperItemId, Function.identity(),
                (left, right) -> left));
        if (enforceFullCoverage && !new HashSet<>(paperItemIds).containsAll(responsesByPaperItemId.keySet())) {
            throw new IllegalArgumentException(
                "Phải chấm đủ tất cả " + responsesByPaperItemId.size() + " phần thi của bài.");
        }
        var criteria = rubricCriterionRepository
            .findByRubricVersionId(context.candidateResult().getRubricVersionId()).stream()
            .collect(Collectors.toMap(RubricCriterion::getId, Function.identity(), (left, right) -> left));

        // Chấm xong toàn bộ rồi mới trả về — một phần lỗi không được để lại điểm
        // nửa vời của các phần trước đó.
        var resolved = new ArrayList<ResolvedItem>();
        for (var item : items) {
            var response = responsesByPaperItemId.get(item.paperItemId());
            if (response == null) {
                throw new IllegalArgumentException("Phần thi không thuộc bài thi này.");
            }
            resolved.add(new ResolvedItem(
                item.paperItemId(),
                response.getId(),
                weightedItemScore(item.criterionScores(), criteria),
                item.feedbackSummary(),
                item.criterionScores()
            ));
        }
        return resolved;
    }

    /**
     * Điểm phần = Σ(điểm × trọng số tiêu chí) / Σ(trọng số) — KHÔNG phải trung bình
     * cộng. Trọng số nằm ở {@link RubricCriterion#getWeight()} và có thật (phát âm
     * .25, trôi chảy .20...), bỏ qua nó là chấm sai.
     *
     * <p>Rubric không khai trọng số (tổng bằng 0) thì lùi về trung bình cộng, giống
     * fallback của luồng AI — thà đúng-xấp-xỉ còn hơn sập về 0.
     */
    private BigDecimal weightedItemScore(
            List<SubmitGradingCommand.CriterionScoreItem> scores,
            Map<UUID, RubricCriterion> criteria) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("Phải chấm điểm cho các tiêu chí.");
        }
        var submittedIds = scores.stream()
            .map(SubmitGradingCommand.CriterionScoreItem::rubricCriterionId).toList();
        if (submittedIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Thiếu tiêu chí chấm điểm.");
        }
        if (new HashSet<>(submittedIds).size() != submittedIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng tiêu chí.");
        }
        var missingRequired = criteria.values().stream()
            .filter(RubricCriterion::isRequired)
            .filter(criterion -> !submittedIds.contains(criterion.getId()))
            .map(RubricCriterion::getName)
            .toList();
        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException(
                "Phải chấm đủ các tiêu chí bắt buộc: " + String.join(", ", missingRequired) + ".");
        }

        var weightedSum = BigDecimal.ZERO;
        var weightSum = BigDecimal.ZERO;
        var plainSum = BigDecimal.ZERO;
        for (var item : scores) {
            var criterion = criteria.get(item.rubricCriterionId());
            if (criterion == null) {
                throw new IllegalArgumentException("Tiêu chí không thuộc rubric của bài thi này.");
            }
            if (item.score() == null) {
                throw new IllegalArgumentException("Phải chấm điểm cho tiêu chí " + criterion.getName() + ".");
            }
            if (item.score().compareTo(criterion.getMinScore()) < 0
                    || item.score().compareTo(criterion.getMaxScore()) > 0) {
                throw new IllegalArgumentException("Điểm tiêu chí " + criterion.getName()
                    + " phải nằm trong khoảng " + criterion.getMinScore() + " - " + criterion.getMaxScore() + ".");
            }
            plainSum = plainSum.add(item.score());
            if (criterion.getWeight() != null) {
                weightedSum = weightedSum.add(item.score().multiply(criterion.getWeight()));
                weightSum = weightSum.add(criterion.getWeight());
            }
        }

        if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
            return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
        }
        return plainSum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
