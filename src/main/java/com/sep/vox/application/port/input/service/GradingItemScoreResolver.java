package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.service.exam.RubricItemScoreFormula;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Validate + quy đổi điểm tiêu chí thành điểm phần thi.
 *
 * <p>Tách riêng vì {@code RegradeResultUseCase} và {@code PreviewGradingUseCase}
 * BẮT BUỘC đi qua đúng một đường: nếu preview và submit tính khác nhau dù chỉ ở
 * chỗ làm tròn, con số giáo viên thấy trước khi bấm Nộp sẽ không phải con số học
 * sinh nhận — đó chính là lý do endpoint preview tồn tại.
 */
@Service
public class GradingItemScoreResolver {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;

    public GradingItemScoreResolver(
            ExamItemResponseRepository examItemResponseRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
    }

    /** Một phần thi đã chấm xong, sẵn sàng ghi xuống hoặc đem đi tính thử. */
    public record ResolvedItem(
        UUID paperItemId,
        UUID responseId,
        BigDecimal itemScore,
        String feedbackSummary,
        List<SubmitGradingCommand.CriterionScoreItem> criterionScores,
        // RubricCriterion tuong ung criterionScores -- dung de bao HumanGradingSubmittedEvent
        // (suy nhan diem yeu tu feedbackSummary), khong tham gia tinh diem.
        List<RubricCriterion> criteria
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
        var paperItemIds = items.stream().map(itemGrade -> itemGrade.paperItemId()).toList();
        if (paperItemIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Thiếu phần thi cần chấm.");
        }
        if (new HashSet<>(paperItemIds).size() != paperItemIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng phần thi.");
        }

        var responsesByPaperItemId = examItemResponseRepository
            .findBySessionId(context.candidateResult().getSessionId()).stream()
            .collect(Collectors.toMap(res -> res.getPaperItemId(), Function.identity(),
                (left, right) -> left));
        if (enforceFullCoverage && !new HashSet<>(paperItemIds).containsAll(responsesByPaperItemId.keySet())) {
            throw new IllegalArgumentException(
                "Phải chấm đủ tất cả " + responsesByPaperItemId.size() + " phần thi của bài.");
        }
        var criteria = rubricCriterionRepository
            .findByRubricVersionId(context.candidateResult().getRubricVersionId()).stream()
            .collect(Collectors.toMap(criterion -> criterion.getId(), Function.identity(), (left, right) -> left));
        var rubricVersion = rubricVersionRepository.findById(context.candidateResult().getRubricVersionId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rubric version của bài thi."));

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
                itemScore(item.criterionScores(), criteria, rubricVersion.getTotalScoreMethod(),
                    rubricVersion.getScoringScaleMin(), rubricVersion.getScoringScaleMax()),
                item.feedbackSummary(),
                item.criterionScores(),
                item.criterionScores().stream()
                    .map(score -> criteria.get(score.rubricCriterionId()))
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList()
            ));
        }
        return resolved;
    }

    /**
     * Kiểm tra điểm giáo viên nhập rồi giao phép tính cho {@link RubricItemScoreFormula} -- công
     * thức DÙNG CHUNG với đường AI chấm ({@code RecordExamAttemptEvaluationUseCase}).
     *
     * <p>Trước 2026-08-11 đây là bản chép tay thứ hai của cùng công thức, và hai bản đã trôi lệch:
     * nhánh dự phòng (rubric không khai trọng số) bên này chia cho số tiêu chí NỘP LÊN, bên kia
     * chia cho số tiêu chí KHỚP được rubric. Nay cả hai cùng chia cho số tiêu chí đã phân giải.
     *
     * <p>Phần giữ riêng ở đây là KIỂM TRA đầu vào, và nó cố ý nghiêm hơn đường AI: người đang nhập
     * tay thì phải bị chặn ngay khi thiếu tiêu chí bắt buộc, chấm trùng, hoặc điểm lọt ra ngoài
     * khoảng của tiêu chí.
     */
    private BigDecimal itemScore(
            List<SubmitGradingCommand.CriterionScoreItem> scores,
            Map<UUID, RubricCriterion> criteria,
            RubricTotalScoreMethod totalScoreMethod,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("Phải chấm điểm cho các tiêu chí.");
        }
        var submittedIds = scores.stream()
            .map(item -> item.rubricCriterionId()).toList();
        if (submittedIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Thiếu tiêu chí chấm điểm.");
        }
        if (new HashSet<>(submittedIds).size() != submittedIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng tiêu chí.");
        }
        var missingRequired = criteria.values().stream()
            .filter(criterion -> criterion.isRequired())
            .filter(criterion -> !submittedIds.contains(criterion.getId()))
            .map(criterion -> criterion.getName())
            .toList();
        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException(
                "Phải chấm đủ các tiêu chí bắt buộc: " + String.join(", ", missingRequired) + ".");
        }

        var scored = new ArrayList<RubricItemScoreFormula.ScoredCriterion>();
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
            scored.add(new RubricItemScoreFormula.ScoredCriterion(item.score(), criterion.getWeight()));
        }

        return RubricItemScoreFormula.compute(
            scored, totalScoreMethod, scoringScaleMin, scoringScaleMax
        );
    }
}
