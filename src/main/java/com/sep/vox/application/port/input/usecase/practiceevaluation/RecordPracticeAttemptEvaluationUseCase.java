package com.sep.vox.application.port.input.usecase.practiceevaluation;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator.ConfidenceMode;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.personalization.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemResponseRepository;

@Service
public class RecordPracticeAttemptEvaluationUseCase implements IUseCase<RecordPracticeAttemptEvaluationCommand, Void> {

    private final PracticeItemEvaluationRepository evaluationRepository;
    private final PracticeCriterionScoreRepository criterionScoreRepository;
    private final PracticeItemResponseRepository responseRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ConfidenceReviewCalculator confidenceReviewCalculator;

    public RecordPracticeAttemptEvaluationUseCase(
            PracticeItemEvaluationRepository evaluationRepository,
            PracticeCriterionScoreRepository criterionScoreRepository,
            PracticeItemResponseRepository responseRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ConfidenceReviewCalculator confidenceReviewCalculator) {
        this.evaluationRepository = evaluationRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.responseRepository = responseRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.confidenceReviewCalculator = confidenceReviewCalculator;
    }

    @Override
    @Transactional
    public Void execute(RecordPracticeAttemptEvaluationCommand input) {
        var confidenceDecision = confidenceReviewCalculator.compute(
            input.confidenceCase(),
            input.audioQuality(),
            ConfidenceMode.PRACTICE,
            input.codeSwitchingRatio(),
            input.wordCount() < 35
        );
        var markedInvalid = !input.validForScoring() || confidenceDecision.requiresHumanReview();
        var itemScore = input.criteria().stream()
            .mapToDouble(criterion -> criterion.score() == null ? 0 : criterion.score())
            .average()
            .orElse(0);
        var evaluatedAt = input.evaluatedAt() == null || input.evaluatedAt().isBlank()
            ? OffsetDateTime.now()
            : OffsetDateTime.parse(input.evaluatedAt());

        var evaluationId = evaluationRepository.upsert(
            input.practiceResponseId(),
            itemScore,
            markedInvalid,
            evaluatedAt
        );

        var rubricVersionId = responseRepository.findRubricVersionIdByResponseId(input.practiceResponseId());
        for (var criterion : input.criteria()) {
            var rubricCriterion = rubricCriterionRepository
                .findByRubricVersionIdAndCodeIgnoreCase(rubricVersionId, criterion.criterionCode())
                .orElse(null);
            if (rubricCriterion == null) {
                continue;
            }
            criterionScoreRepository.upsert(
                evaluationId,
                rubricCriterion.getId(),
                criterion.score() == null ? 0 : criterion.score(),
                criterion.matchedBandCode()
            );
        }
        return null;
    }
}
