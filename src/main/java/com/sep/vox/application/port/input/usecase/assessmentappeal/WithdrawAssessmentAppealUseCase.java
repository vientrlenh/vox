package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * A student withdraws an appeal while it is still {@code PENDING}.
 *
 * <p>The submission goes back to {@code RELEASED} and <em>the appeal round is refunded</em>: the quota counts
 * the appeals that are {@code PUBLISHED}, and a withdrawn appeal was never graded, so it does not burn a round.
 *
 * <p>Withdrawal is only allowed in {@code PENDING}. From {@code APPROVED} on, someone has already been assigned
 * (and may have read the submission), so withdrawing then wastes the school's effort -- to stop it, the admin
 * rejects the appeal through the admin path, with a recorded reason.
 */
@Service
@RequiredArgsConstructor
public class WithdrawAssessmentAppealUseCase implements IUseCase<UUID, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(appealId);
        assessmentAppealAccessService.authorizeOwningStudent(context, currentUserId);

        AssessmentResultAppeal appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalArgumentException("An appeal can only be withdrawn while it is pending.");
        }

        Instant now = Instant.now();
        appeal.setStatus(AssessmentAppealStatus.WITHDRAWN);
        appeal.setWithdrawnAt(now);
        appeal.setResolvedAt(now);
        appeal.setResolvedBy(currentUserId);
        assessmentResultAppealRepository.save(appeal);
        releaseResultIfStillAppealed(context.candidateResult(), currentUserId);

        return appeal.getId();
    }

    /**
     * Returns the result to RELEASED only while it really sits in an appeal status. A result that moved on (FINAL,
     * PASSED/FAILED, INVALID...) must not be pulled back by a withdrawal, otherwise one student action would
     * reopen a closed result.
     */
    private void releaseResultIfStillAppealed(AssessmentCandidateResult candidateResult, UUID currentUserId) {
        AssessmentCandidateResultStatus status = candidateResult.getStatus();
        if (status == AssessmentCandidateResultStatus.APPEALED || status == AssessmentCandidateResultStatus.RE_GRADING) {
            ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(candidateResult);
            candidateResult.setStatus(AssessmentCandidateResultStatus.RELEASED);
            assessmentCandidateResultRepository.save(candidateResult);
            resultStatusHistoryRecorder.record(
                before, candidateResult, ResultStatusChangeSource.SYSTEM, currentUserId,
                "The student withdrew the appeal.");
        }
    }
}
