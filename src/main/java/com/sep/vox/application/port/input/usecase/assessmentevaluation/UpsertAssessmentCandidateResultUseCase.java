package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
public class UpsertAssessmentCandidateResultUseCase {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentSessionResultCalculator assessmentSessionResultCalculator;

    /**
     * G.2: blockedAt (not yet lifted) -> INVALID; session.flagged (suspicious, waiting for ReviewFlaggedAssessmentResultUseCase)
     * or having an item that needs human review -> PENDING_REVIEW; the rest (AI is confident, not kicked,
     * not suspicious) -> RELEASED right away.
     */
    @Transactional
    public AssessmentCandidateResult execute(UUID sessionId) {
        return execute(sessionId, null);
    }

    private AssessmentCandidateResultStatus resolveDefaultStatus(UUID sessionId, UUID candidateId, boolean anyRequiresHumanReview) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        if (candidate.getBlockedAt() != null) {
            return AssessmentCandidateResultStatus.INVALID;
        }
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        if (session.isFlagged() || anyRequiresHumanReview) {
            return AssessmentCandidateResultStatus.PENDING_REVIEW;
        }
        return AssessmentCandidateResultStatus.RELEASED;
    }

    /**
     * Recalculates the session result and stores it with an explicit status.
     * The appeal flow uses this to land back on RELEASED after publishing a re-grade
     * (so the result stays appealable within its round limit); grading uses the
     * single-argument overload and stays on PENDING_REVIEW.
     */
    @Transactional
    public AssessmentCandidateResult execute(UUID sessionId, AssessmentCandidateResultStatus status) {
        AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult calculated = assessmentSessionResultCalculator.calculate(sessionId);
        AssessmentCandidateResult existing = assessmentCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        AssessmentCandidateResult result = existing == null ? new AssessmentCandidateResult() : existing;
        Instant now = Instant.now();
        AssessmentCandidateResultStatus resolvedStatus = status != null
            ? status
            : resolveDefaultStatus(sessionId, calculated.candidateId(), calculated.anyRequiresHumanReview());

        result.setAssessmentId(calculated.assessmentId());
        result.setCandidateId(calculated.candidateId());
        result.setSessionId(calculated.sessionId());
        result.setAssessmentPolicyId(calculated.policy().getId());
        result.setPolicyVersion(calculated.policy().getVersion());
        result.setRubricVersionId(calculated.policy().getRubricVersionId());
        result.setFrameworkVersionId(calculated.policy().getFrameworkVersionId());
        result.setTargetFrameworkBandId(calculated.policy().getTargetFrameworkBandId());
        result.setRubricResultBandId(calculated.rubricResultBand() == null ? null : calculated.rubricResultBand().getId());
        result.setTotalScore(calculated.totalScore());
        result.setStatus(resolvedStatus);
        if (resolvedStatus == AssessmentCandidateResultStatus.RELEASED && result.getReleasedAt() == null) {
            result.setReleasedAt(now);
        }
        return assessmentCandidateResultRepository.save(result);
    }
}
