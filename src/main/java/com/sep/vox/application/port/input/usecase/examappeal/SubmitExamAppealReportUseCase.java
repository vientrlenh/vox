package com.sep.vox.application.port.input.usecase.examappeal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
public class SubmitExamAppealReportUseCase implements IUseCase<SubmitExamAppealReportCommand, UUID> {

    /**
     * exam_item_evaluations.graded_by_model is NOT NULL and meant for the AI model
     * name; a human panel has no model, so the engine name stands in.
     */
    private static final String HUMAN_GRADER = "HUMAN";

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamAppealReviewerRepository examAppealReviewerRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public SubmitExamAppealReportUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamAppealReviewerRepository examAppealReviewerRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examAppealReviewerRepository = examAppealReviewerRepository;
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

        var criteria = rubricCriterionRepository
            .findByRubricVersionId(context.candidateResult().getRubricVersionId()).stream()
            .collect(Collectors.toMap(RubricCriterion::getId, Function.identity(), (left, right) -> left));
        var scores = command.scores() == null ? new ArrayList<SubmitExamAppealReportCommand.CriterionScoreItem>()
            : command.scores();
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("Phải chấm điểm cho các tiêu chí.");
        }
        if (new HashSet<>(scores.stream().map(SubmitExamAppealReportCommand.CriterionScoreItem::criterionId).toList())
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
        var suggestedScore = total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        var now = OffsetDateTime.now();
        // UNDER_REVIEW keeps this report out of ExamSessionResultCalculator until the
        // appeal is published, so the candidate never sees an unpublished re-grade.
        var evaluation = new ExamItemEvaluation(
            appeal.getResponseId(),
            appeal.getPaperItemId(),
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
            command.note(),
            null,
            null,
            ExamItemEvaluationStatus.UNDER_REVIEW,
            now
        );
        var savedEvaluation = examItemEvaluationRepository.save(evaluation);

        var criterionScores = scores.stream()
            .map(item -> new ExamItemCriterionScore(
                savedEvaluation.getId(),
                item.criterionId(),
                item.score(),
                item.score(),
                item.rationale()
            ))
            .toList();
        examItemCriterionScoreRepository.saveAll(criterionScores);

        reviewer.setStatus(ExamAppealReviewerStatus.SUBMITTED);
        reviewer.setSubmittedAt(now);
        reviewer.setNote(command.note());
        reviewer.setSuggestedScore(suggestedScore);
        reviewer.setEvaluationId(savedEvaluation.getId());
        examAppealReviewerRepository.save(reviewer);

        var allSubmitted = examAppealReviewerRepository.findByAppealId(command.appealId()).stream()
            .allMatch(item -> item.getStatus() == ExamAppealReviewerStatus.SUBMITTED);
        if (allSubmitted) {
            appeal.setStatus(ExamAppealStatus.COMPARING);
            examResultAppealRepository.save(appeal);
        }

        return savedEvaluation.getId();
    }
}
