package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentappeal.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealItemRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentAppealUseCase implements IUseCase<CreateAssessmentAppealCommand, UUID> {

    /**
     * Maximum number of appeal rounds for one result. Publishing an appeal returns the result to
     * RELEASED, so the status no longer blocks a later round by itself; this limit takes over that role.
     */
    static final int MAX_APPEAL_ROUNDS = 2;

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentResultAppealItemRepository assessmentResultAppealItemRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    @Override
    @Transactional
    public UUID execute(CreateAssessmentAppealCommand command) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.loadByCandidateResultId(command.candidateResultId());
        assessmentAppealAccessService.authorizeOwningStudent(context, currentUserId);
        AssessmentCandidateResult candidateResult = context.candidateResult();
        requireAppealable(command, candidateResult);
        LinkedHashSet<UUID> selected = requireDistinctPaperItems(command.paperItemIds());
        Map<UUID, AssessmentItemResponse> responsesByPaperItem = responsesByPaperItem(candidateResult);
        LinkedHashSet<UUID> distinctPaperItemIds = resolveAppealedPaperItemIds(selected, responsesByPaperItem);

        Instant now = Instant.now();
        AssessmentResultAppeal appeal = AssessmentResultAppeal.requested(command.candidateResultId(), currentUserId,
                command.reason(), now, candidateResult.getTotalScore(), command.notes());
        AssessmentResultAppeal saved = assessmentResultAppealRepository.save(appeal);
        saveAppealItems(saved, distinctPaperItemIds, responsesByPaperItem);
        closeOpenGradingAssignment(command.candidateResultId(), now);
        moveResultToAppealed(candidateResult, currentUserId);
        return saved.getId();
    }

    private void requireAppealable(CreateAssessmentAppealCommand command, AssessmentCandidateResult candidateResult) {
        if (candidateResult.getStatus() != AssessmentCandidateResultStatus.RELEASED) {
            throw new IllegalArgumentException("Only a published result can be appealed.");
        }
        if (assessmentResultAppealRepository.existsOpenByCandidateResultId(command.candidateResultId())) {
            throw new ResourceDuplicatedException("An appeal is already being processed for this result.");
        }
        if (assessmentResultAppealRepository.countPublishedByCandidateResultId(command.candidateResultId())
                >= MAX_APPEAL_ROUNDS) {
            throw new IllegalArgumentException(
                "Each result can be appealed at most " + MAX_APPEAL_ROUNDS + " times.");
        }
    }

    /** A duplicated paper item is a client error, detected before touching the database. */
    private LinkedHashSet<UUID> requireDistinctPaperItems(List<UUID> requested) {
        if (requested == null) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<UUID> selected = new LinkedHashSet<>(requested);
        if (selected.size() != requested.size()) {
            throw new IllegalArgumentException("The same assessment part must not be selected twice.");
        }
        return selected;
    }

    /**
     * The re-grading score is written per response, so the responses are fixed up front. One pass over the
     * responses of the session avoids N repository calls.
     */
    private Map<UUID, AssessmentItemResponse> responsesByPaperItem(AssessmentCandidateResult candidateResult) {
        return assessmentItemResponseRepository.findBySessionId(candidateResult.getSessionId())
            .stream()
            .filter(item -> item.getPaperItemId() != null)
            .collect(Collectors.toMap(
                item -> item.getPaperItemId(), Function.identity(), (left, right) -> left,
                LinkedHashMap::new));   // keeps the response order for the free-form part
    }

    private LinkedHashSet<UUID> resolveAppealedPaperItemIds(
        LinkedHashSet<UUID> selected, Map<UUID, AssessmentItemResponse> responsesByPaperItem) {
        if (selected.isEmpty()) {
            // Selecting no question = appeal the whole attempt. It still writes assessment_result_appeal_items for
            // each question: the comparison screen of the school administrator takes baselineScores + turns from exactly
            // those rows, and dropping them would lose the comparison data.
            if (responsesByPaperItem.isEmpty()) {
                throw new ResourceNotFoundException("This submission has no answer to appeal.");
            }
            return new LinkedHashSet<>(responsesByPaperItem.keySet());
        }
        if (!responsesByPaperItem.keySet().containsAll(selected)) {
            throw new ResourceNotFoundException("The answer of the assessment part to appeal was not found.");
        }
        return selected;
    }

    /** The appeal id exists only after the INSERT returns, so the appealed paper items are saved in a later step. */
    private void saveAppealItems(
        AssessmentResultAppeal saved,
        LinkedHashSet<UUID> distinctPaperItemIds,
        Map<UUID, AssessmentItemResponse> responsesByPaperItem) {
        assessmentResultAppealItemRepository.saveAll(distinctPaperItemIds.stream()
            .map(paperItemId -> AssessmentResultAppealItem.forResponse(
                saved.getId(), paperItemId, responsesByPaperItem.get(paperItemId).getId()))
            .toList());
    }

    /**
     * Closes the open grading round (usually a post-check) before the result moves to APPEALED. Skipping this was
     * review BE-4: the result leaves the status that round can handle, the assignment hangs forever in the
     * teacher's queue and active_result_id keeps its slot, so no appeal reviewer can be assigned.
     *
     * <p>It is closed as DECLINED, not UPHELD: the teacher made no decision, and recording UPHELD would invent a
     * verdict they never gave.
     */
    private void closeOpenGradingAssignment(UUID candidateResultId, Instant now) {
        assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId)
            .ifPresent(open -> {
                open.complete(GradingOutcome.DECLINED,
                    "The submission moved to appeal at the student's request.", now);
                assessmentGradingAssignmentRepository.save(open);
            });
    }

    private void moveResultToAppealed(AssessmentCandidateResult candidateResult, UUID currentUserId) {
        ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);
        assessmentCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(before, candidateResult,
            ResultStatusChangeSource.SYSTEM, currentUserId, "The student filed an appeal.");
    }
}
