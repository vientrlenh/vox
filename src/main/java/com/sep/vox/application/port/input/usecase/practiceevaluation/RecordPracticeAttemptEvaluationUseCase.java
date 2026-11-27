package com.sep.vox.application.port.input.usecase.practiceevaluation;

import com.sep.vox.application.port.input.command.practiceevaluation.PracticeCriterionScoreInput;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordPracticeAttemptEvaluationUseCase implements IUseCase<RecordPracticeAttemptEvaluationCommand, Void> {

    private final PracticeItemEvaluationRepository evaluationRepository;
    private final PracticeCriterionScoreRepository criterionScoreRepository;
    private final PracticeItemResponseRepository responseRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    @Override
    @Transactional
    public Void execute(RecordPracticeAttemptEvaluationCommand input) {
        UUID evaluationId = upsertItemEvaluation(input);
        upsertCriterionScores(evaluationId, input);

        // Same transaction as the evaluation upsert above, so the status and the evaluation can NEVER
        // disagree. With two separate writes there would be moments when a graded row still carries
        // the GRADING label, i.e. the data would lie to anyone reading the table.
        responseRepository.markGraded(input.practiceResponseId());
        refreshSessionScore(input.practiceResponseId());
        return null;
    }

    private UUID upsertItemEvaluation(RecordPracticeAttemptEvaluationCommand input) {
        boolean markedInvalid = !input.validForScoring();
        double itemScore = input.criteria().stream()
            .mapToDouble(criterion -> {
                Double score = criterion.score();
                return score == null ? 0 : score;
            })
            .average()
            .orElse(0);
        Instant evaluatedAt = input.evaluatedAt() == null || input.evaluatedAt().isBlank()
            ? Instant.now()
            : Instant.parse(input.evaluatedAt());

        return evaluationRepository.upsert(
            input.practiceResponseId(),
            itemScore,
            markedInvalid,
            evaluatedAt
        );
    }

    private void upsertCriterionScores(UUID evaluationId, RecordPracticeAttemptEvaluationCommand input) {
        for (PracticeCriterionScoreInput criterion : input.criteria()) {
            String code = criterion.criterionCode() == null
                ? ""
                : criterion.criterionCode().trim().toUpperCase(Locale.ROOT);
            if (code.isBlank()) {
                log.warn(
                    "Skipping a criterion without a code in practice grading result {}",
                    input.practiceResponseId()
                );
                continue;
            }
            Double score = criterion.score();
            criterionScoreRepository.upsertByCode(
                evaluationId,
                code,
                score == null ? 0 : score
            );
        }
    }

    private void refreshSessionScore(UUID responseId) {
        try {
            UUID sessionId = responseRepository.findSessionIdByResponseId(responseId);
            if (sessionId == null) {
                return;
            }
            // ONE UPDATE statement, not loading the entity and saving it back -- see the long note at
            // SpringDataPracticeSessionRepository.refreshOverallScore. In short: save() overwrites the whole row, so it
            // could wipe graded_seconds that a parallel submission just added, and it holds the row lock until the end
            // of this long transaction, making claim() queue up.
            practiceSessionRepository.refreshOverallScore(sessionId);
        } catch (RuntimeException exception) {
            // The graded score is the primary data; recomputing the session score is derived, and the next grading
            // makes up for it. Do not let it bring down the writing of the grading result.
            log.warn("Could not recalculate the session score from response {}.", responseId, exception);
        }
    }

}
