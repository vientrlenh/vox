package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class UpsertAssessmentCandidateResultUseCase {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentSessionResultCalculator assessmentSessionResultCalculator;

    public UpsertAssessmentCandidateResultUseCase(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentSessionResultCalculator assessmentSessionResultCalculator) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentSessionResultCalculator = assessmentSessionResultCalculator;
    }

    /**
     * G.2: blockedAt (chưa dỡ) -> INVALID; session.flagged (nghi vấn, chờ ReviewFlaggedAssessmentResultUseCase)
     * hoặc có item cần human review -> PENDING_REVIEW; còn lại (AI tự tin, không bị kicked,
     * không bị nghi vấn) -> RELEASED ngay.
     */
    @Transactional
    public AssessmentCandidateResult execute(UUID sessionId) {
        return execute(sessionId, null);
    }

    private AssessmentCandidateResultStatus resolveDefaultStatus(UUID sessionId, UUID candidateId, boolean anyRequiresHumanReview) {
        var candidate = assessmentCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh"));
        if (candidate.getBlockedAt() != null) {
            return AssessmentCandidateResultStatus.INVALID;
        }
        var session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
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
        var calculated = assessmentSessionResultCalculator.calculate(sessionId);
        var existing = assessmentCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        var result = existing == null ? new AssessmentCandidateResult() : existing;
        var now = Instant.now();
        var resolvedStatus = status != null
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
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        return assessmentCandidateResultRepository.save(result);
    }
}
