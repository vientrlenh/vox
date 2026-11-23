package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAiEvaluationContextResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemCriterionScoreResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationTurnResponse;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.repository.AssessmentItemCriterionScoreRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentItemResponseEvaluationUseCase
        implements IUseCase<ViewAssessmentItemResponseEvaluationQuery, AssessmentItemEvaluationDetailsResponse> {

    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentItemCriterionScoreRepository assessmentItemCriterionScoreRepository;
    private final AssessmentItemEvaluationTurnRepository assessmentItemEvaluationTurnRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final AssessmentResultAccessService assessmentResultAccessService;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    public AssessmentItemEvaluationDetailsResponse execute(ViewAssessmentItemResponseEvaluationQuery input) {
        AssessmentResultAccessService.ResponseAccess access = assessmentResultAccessService.requireCandidateVisibleResponse(input.answerId());
        AssessmentItemEvaluation evaluation = assessmentItemEvaluationRepository.findLatestByResponseId(input.answerId())
            .orElse(null);
        // Nobody has graded this question yet -- the speech recording must still be returned.
        //
        // This used to be orElseThrow(NotFoundException). An attempt that was forcibly ended and then cleared of the violation
        // never had any evaluation, so the Result page could get neither the audio nor the transcript
        // even though assessment_item_response_turns was intact -- while the grading screen shows everything, because it LEFT JOINs
        // to the evaluation instead of demanding one. Two pages read the same data with two opposite
        // assumptions; this pulls the result page onto the assumption of the grading screen.
        //
        // The score and criteria are left empty because they really do not exist yet. The speech recording is not a score --
        // it is evidence, and there is no reason to make anyone wait until grading is done to see it.
        if (evaluation == null) {
            return ungradedDetails(access.response());
        }

        // The version in force is the manual grading => all AI evidence sits in the AI version that was SUPERSEDED
        // and has to be looked up separately. If the version in force is already the AI one it is used as is -- no extra query.
        AssessmentItemEvaluation aiEvaluation = evaluation.getEngineType() == AssessmentEvaluationEngineType.HUMAN
            ? assessmentItemEvaluationRepository.findLatestAiByResponseId(input.answerId()).orElse(null)
            : evaluation;

        // criteria still follow the version in force: after a re-grade these are the TEACHER's score and rationale,
        // as designed. Only the context is taken from the AI version.
        List<AssessmentItemCriterionScoreResponse> criteria = assessmentItemCriterionScoreRepository.findByEvaluationId(evaluation.getId()).stream()
            .map(item -> {
                RubricCriterion criterion = rubricCriterionRepository.findById(item.getRubricCriterionId()).orElse(null);
                return new AssessmentItemCriterionScoreResponse(
                    item.getId(),
                    item.getRubricCriterionId(),
                    criterion == null ? null : criterion.getCode(),
                    criterion == null ? null : criterion.getName(),
                    criterion == null ? null : criterion.getMinScore(),
                    criterion == null ? null : criterion.getMaxScore(),
                    item.getRawScore(),
                    item.getFinalScore(),
                    item.getRationale()
                );
            })
            .toList();
        // Turns only exist on the AI version. Reading them from the version in force loses the audio/transcript/question
        // text right after the first re-grade -- which is exactly the bug being fixed.
        UUID turnSourceId = (aiEvaluation == null ? evaluation : aiEvaluation).getId();
        List<AssessmentItemEvaluationTurnResponse> turns = assessmentItemEvaluationTurnRepository.findByEvaluationId(turnSourceId).stream()
            .map(item -> new AssessmentItemEvaluationTurnResponse(
                item.getId(),
                item.getTurnOrder(),
                item.getTurnType(),
                item.getPromptText(),
                item.getAudioUrl(),
                item.getTranscript(),
                item.getWordCount(),
                item.getDurationSeconds(),
                item.getAsrConfidence(),
                item.getPronunciationOverallJson(),
                item.getWordFeedbackJson()
            ))
            .toList();
        // No AI version -> fall back to the candidate's ORIGINAL spoken turns.
        //
        // An attempt forcibly ended midway was never graded by the AI, and an attempt graded by a teacher by hand
        // only produces a HUMAN version -- and HUMAN has no turns. Both land here, and the
        // Result page showed a blank question: no audio, no transcript, no prompt, even though
        // assessment_item_response_turns was intact (measured 2026-08-17, session 01a00e64).
        //
        // The three fields specific to the AI version (asrConfidence, pronunciationOverall, wordFeedback) are left
        // null: the original table does not have them, and inventing a value is worse than leaving it empty. Same handling
        // as JpaAssessmentGradingQueryRepository#turnsForItem on the grading screen.
        if (turns.isEmpty()) {
            turns = assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(evaluation.getResponseId()).stream()
                .map(item -> new AssessmentItemEvaluationTurnResponse(
                    item.getId(),
                    item.getTurnOrder(),
                    item.getTurnType(),
                    item.getPromptText(),
                    item.getAudioUrl(),
                    item.getTranscript(),
                    item.getWordCount(),
                    item.getDurationSeconds(),
                    null,
                    null,
                    null
                ))
                .toList();
        }

        return new AssessmentItemEvaluationDetailsResponse(
            evaluation.getId(),
            evaluation.getResponseId(),
            evaluation.getPaperItemId(),
            evaluation.getEngineType(),
            evaluation.getGradedByModel(),
            evaluation.getPromptVersion(),
            evaluation.getRawItemScore(),
            evaluation.getItemScore(),
            evaluation.getOverallConfidence(),
            evaluation.isRequiresHumanReview(),
            evaluation.getReviewReasonCode(),
            evaluation.isMarkedInvalid(),
            evaluation.isRequiresRetake(),
            evaluation.getStatus(),
            evaluation.getEvaluatedAt(),
            evaluation.getFeedbackSummary(),
            jsonSerializationPort.toJson(evaluation.getSignals()),
            evaluation.getValidityJson(),
            evaluation.getSuggestionsJson(),
            criteria,
            turns,
            toAiContext(aiEvaluation)
        );
    }

    /**
     * An answer that has no grading at all: only the speech recording exists, no score.
     *
     * <p>A null {@code id} tells the client "not graded yet" without having to infer it from a missing
     * score -- a valid score of 0 is also "missing" in that sense. The three fields specific to the AI version
     * (asrConfidence, pronunciationOverall, wordFeedback) are null too: the original table does not have them, and
     * inventing a value is worse than leaving it empty.
     */
    private AssessmentItemEvaluationDetailsResponse ungradedDetails(
            com.sep.vox.domain.model.assessment.AssessmentItemResponse response) {
        List<AssessmentItemEvaluationTurnResponse> turns = assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(response.getId()).stream()
            .map(item -> new AssessmentItemEvaluationTurnResponse(
                item.getId(),
                item.getTurnOrder(),
                item.getTurnType(),
                item.getPromptText(),
                item.getAudioUrl(),
                item.getTranscript(),
                item.getWordCount(),
                item.getDurationSeconds(),
                null,
                null,
                null
            ))
            .toList();
        return new AssessmentItemEvaluationDetailsResponse(
            null,
            response.getId(),
            response.getPaperItemId(),
            null, null, null,
            null, null, null,
            false, null, false, false,
            null, null, null, null, null, null,
            List.of(),
            turns,
            null
        );
    }

    private AssessmentItemAiEvaluationContextResponse toAiContext(AssessmentItemEvaluation aiEvaluation) {
        if (aiEvaluation == null) {
            return null;
        }
        return new AssessmentItemAiEvaluationContextResponse(
            aiEvaluation.getId(),
            aiEvaluation.getEngineType(),
            aiEvaluation.getGradedByModel(),
            aiEvaluation.getPromptVersion(),
            aiEvaluation.getOverallConfidence(),
            aiEvaluation.isRequiresHumanReview(),
            aiEvaluation.getReviewReasonCode(),
            aiEvaluation.isMarkedInvalid(),
            aiEvaluation.isRequiresRetake(),
            aiEvaluation.getEvaluatedAt(),
            aiEvaluation.getFeedbackSummary(),
            jsonSerializationPort.toJson(aiEvaluation.getSignals()),
            aiEvaluation.getValidityJson(),
            aiEvaluation.getSuggestionsJson()
        );
    }
}
