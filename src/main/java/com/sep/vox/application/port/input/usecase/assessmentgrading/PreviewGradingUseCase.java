package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator;
import com.sep.vox.application.response.input.assessmentgrading.GradingPreviewResponse;

/**
 * Computes a trial total score for the set of scores the teacher is entering, writing NOTHING.
 *
 * <p>It exists because the backend's total formula is a two-level weighted one
 * ({@code section = Σ(itemScore × paperItem.weight)}, then
 * {@code total = Σ(section × section.weight) / Σ(section.weight)}, rounded to 2 decimal
 * places) -- the frontend cannot reproduce it exactly, and the calculator must never be changed to
 * match the frontend because it is shared with the AI flow and the appeal flow.
 *
 * <p>It is a POST but read-only: the body is too large for a GraphQL query. {@code readOnly}
 * is the last barrier -- validation and score conversion share a path with
 * {@code RegradeResultUseCase}, so the total here equals exactly the total at submission.
 */
@Service
@RequiredArgsConstructor
public class PreviewGradingUseCase implements IUseCase<SubmitGradingCommand, GradingPreviewResponse> {

    private final AssessmentSessionResultCalculator assessmentSessionResultCalculator;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingItemScoreResolver gradingItemScoreResolver;

    @Override
    @Transactional(readOnly = true)
    public GradingPreviewResponse execute(SubmitGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.load(command.assignmentId());
        assessmentGradingAccessService.authorizeAssignedTeacher(context, currentUserId);

        // Preview allows partial grading so the total can be watched as it grows -- it does NOT require full coverage like /grade.
        List<GradingItemScoreResolver.ResolvedItem> resolvedItems = gradingItemScoreResolver.resolve(context, command, false);
        Map<UUID, BigDecimal> overrides = new LinkedHashMap<UUID, BigDecimal>();
        resolvedItems.forEach(item -> overrides.put(item.responseId(), item.itemScore()));

        AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult previewed = assessmentSessionResultCalculator.preview(
            context.candidateResult().getSessionId(), overrides);

        return new GradingPreviewResponse(
            previewed.totalScore(),
            previewed.resultBandName(),
            previewed.items().stream()
                .map(item -> new GradingPreviewResponse.ItemScore(item.paperItemId(), item.itemScore()))
                .toList()
        );
    }
}
