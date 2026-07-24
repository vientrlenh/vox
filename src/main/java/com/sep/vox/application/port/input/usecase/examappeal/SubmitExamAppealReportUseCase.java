package com.sep.vox.application.port.input.usecase.examappeal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealReviewerItem;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.ExamAppealReviewerItemRepository;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

/**
 * Giám khảo nộp báo cáo chấm lại cho toàn bộ phần thi của đơn trong một lần.
 *
 * <p>Nộp trọn gói là cố ý: "đã nộp" trở thành đúng theo cấu trúc thay vì phải đếm,
 * và ràng buộc "tập phần thi nộp phải khớp tập phần thi của đơn" gói gọn trong một
 * transaction. Không có báo cáo dở dang để phải hoà giải về sau.
 */
@Service
public class SubmitExamAppealReportUseCase implements IUseCase<SubmitExamAppealReportCommand, UUID> {

    /**
     * exam_item_evaluations.graded_by_model is NOT NULL and meant for the AI model
     * name; a human panel has no model, so the engine name stands in.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamResultAppealItemRepository examResultAppealItemRepository;
    private final ExamAppealReviewerRepository examAppealReviewerRepository;
    private final ExamAppealReviewerItemRepository examAppealReviewerItemRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public SubmitExamAppealReportUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamResultAppealItemRepository examResultAppealItemRepository,
            ExamAppealReviewerRepository examAppealReviewerRepository,
            ExamAppealReviewerItemRepository examAppealReviewerItemRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examResultAppealItemRepository = examResultAppealItemRepository;
        this.examAppealReviewerRepository = examAppealReviewerRepository;
        this.examAppealReviewerItemRepository = examAppealReviewerItemRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional
    public UUID execute(SubmitExamAppealReportCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        var appeal = context.appeal();

        if (appeal.getStatus() != ExamAppealStatus.GRADING) {
            throw new IllegalStateException("Chỉ có thể nộp báo cáo khi đơn phúc khảo đang được chấm lại.");
        }

        var reviewer = examAppealReviewerRepository
            .findByAppealIdAndReviewerId(command.appealId(), currentUserId)
            .orElseThrow(() -> new NotFoundException("Bạn không được phân công chấm lại đơn phúc khảo này."));
        if (reviewer.getStatus() == ExamAppealReviewerStatus.SUBMITTED) {
            throw new IllegalStateException("Bạn đã nộp báo cáo chấm lại cho đơn phúc khảo này.");
        }

        var appealItems = examResultAppealItemRepository.findByAppealId(command.appealId()).stream()
            .collect(Collectors.toMap(item -> item.getId(), Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
        List<SubmitExamAppealReportCommand.ItemReport> reports = command.items() == null
            ? new ArrayList<>() : command.items();
        validateItemCoverage(reports, appealItems.keySet());

        var criteria = rubricCriterionRepository
            .findByRubricVersionId(context.candidateResult().getRubricVersionId()).stream()
            .collect(Collectors.toMap(criterion -> criterion.getId(), Function.identity(), (left, right) -> left));

        // Chấm xong toàn bộ phần thi rồi mới ghi — một phần lỗi không được để lại
        // báo cáo nửa vời của các phần trước đó.
        var suggestedScores = new LinkedHashMap<UUID, BigDecimal>();
        for (var report : reports) {
            suggestedScores.put(report.appealItemId(), validateAndAverage(report.scores(), criteria));
        }

        var now = OffsetDateTime.now();
        var criterionScores = new ArrayList<ExamItemCriterionScore>();
        var reviewerItems = new ArrayList<ExamAppealReviewerItem>();
        for (var report : reports) {
            var appealItem = appealItems.get(report.appealItemId());
            var suggestedScore = suggestedScores.get(report.appealItemId());

            // UNDER_REVIEW keeps this report out of ExamSessionResultCalculator until the
            // appeal is published, so the candidate never sees an unpublished re-grade.
            var savedEvaluation = examItemEvaluationRepository.save(new ExamItemEvaluation(
                appealItem.getResponseId(),
                appealItem.getPaperItemId(),
                ExamEvaluationEngineType.HUMAN,
                HUMAN_GRADER,
                null,
                currentUserId,
                suggestedScore,
                suggestedScore,
                null,
                false,
                null,
                false,
                false,
                null,
                null,
                report.note(),
                null,
                null,
                ExamItemEvaluationStatus.UNDER_REVIEW,
                now
            ));

            report.scores().forEach(item -> criterionScores.add(new ExamItemCriterionScore(
                savedEvaluation.getId(),
                item.criterionId(),
                item.score(),
                item.score(),
                item.rationale()
            )));
            reviewerItems.add(new ExamAppealReviewerItem(
                reviewer.getId(),
                appealItem.getId(),
                savedEvaluation.getId(),
                suggestedScore,
                report.note()
            ));
        }
        examItemCriterionScoreRepository.saveAll(criterionScores);
        examAppealReviewerItemRepository.saveAll(reviewerItems);

        reviewer.setStatus(ExamAppealReviewerStatus.SUBMITTED);
        reviewer.setSubmittedAt(now);
        examAppealReviewerRepository.save(reviewer);

        var allSubmitted = examAppealReviewerRepository.findByAppealId(command.appealId()).stream()
            .allMatch(item -> item.getStatus() == ExamAppealReviewerStatus.SUBMITTED);
        if (allSubmitted) {
            appeal.setStatus(ExamAppealStatus.COMPARING);
            examResultAppealRepository.save(appeal);
        }

        return appeal.getId();
    }

    private void validateItemCoverage(
            List<SubmitExamAppealReportCommand.ItemReport> reports, Set<UUID> appealItemIds) {
        if (reports.isEmpty()) {
            throw new IllegalArgumentException("Phải chấm điểm cho tất cả phần thi được phúc khảo.");
        }
        var submittedIds = reports.stream()
            .map(report -> report.appealItemId()).toList();
        if (new HashSet<>(submittedIds).size() != submittedIds.size()) {
            throw new IllegalArgumentException("Không được chấm trùng phần thi.");
        }
        if (!appealItemIds.containsAll(submittedIds)) {
            throw new IllegalArgumentException("Phần thi không thuộc đơn phúc khảo này.");
        }
        if (submittedIds.size() != appealItemIds.size()) {
            throw new IllegalArgumentException(
                "Phải chấm đủ " + appealItemIds.size() + " phần thi của đơn phúc khảo.");
        }
    }

    private BigDecimal validateAndAverage(
            List<SubmitExamAppealReportCommand.CriterionScoreItem> scores,
            Map<UUID, RubricCriterion> criteria) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("Phải chấm điểm cho các tiêu chí.");
        }
        if (new HashSet<>(scores.stream()
                .map(score -> score.criterionId()).toList())
                .size() != scores.size()) {
            throw new IllegalArgumentException("Không được chấm trùng tiêu chí.");
        }
        if (scores.size() != criteria.size()) {
            throw new IllegalArgumentException("Phải chấm đủ " + criteria.size() + " tiêu chí của rubric.");
        }

        var total = BigDecimal.ZERO;
        for (var item : scores) {
            var criterion = criteria.get(item.criterionId());
            if (criterion == null) {
                throw new IllegalArgumentException("Tiêu chí không thuộc rubric của bài thi này.");
            }
            if (item.score() == null) {
                throw new IllegalArgumentException("Phải chấm điểm cho tiêu chí " + criterion.getName() + ".");
            }
            if (item.score().compareTo(criterion.getMinScore()) < 0
                    || item.score().compareTo(criterion.getMaxScore()) > 0) {
                throw new IllegalArgumentException("Điểm tiêu chí " + criterion.getName() + " phải nằm trong khoảng "
                    + criterion.getMinScore() + " - " + criterion.getMaxScore() + ".");
            }
            total = total.add(item.score());
        }
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
