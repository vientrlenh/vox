package com.sep.vox.application.port.input.usecase.examevaluation;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class UpsertExamCandidateResultUseCase {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamSessionResultCalculator examSessionResultCalculator;

    public UpsertExamCandidateResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamSessionResultCalculator examSessionResultCalculator) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examSessionResultCalculator = examSessionResultCalculator;
    }

    /**
     * G.2: blockedAt (chưa dỡ) -> INVALID; session.flagged (nghi vấn, chờ ReviewFlaggedExamResultUseCase)
     * hoặc có item cần human review -> PENDING_REVIEW; còn lại (AI tự tin, không bị kicked,
     * không bị nghi vấn) -> RELEASED ngay.
     */
    @Transactional
    public ExamCandidateResult execute(UUID sessionId) {
        return execute(sessionId, null);
    }

    private ExamCandidateResultStatus resolveDefaultStatus(UUID sessionId, UUID candidateId, boolean anyRequiresHumanReview) {
        var candidate = examCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        if (candidate.getBlockedAt() != null) {
            return ExamCandidateResultStatus.INVALID;
        }
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        if (session.isFlagged() || anyRequiresHumanReview) {
            return ExamCandidateResultStatus.PENDING_REVIEW;
        }
        return ExamCandidateResultStatus.RELEASED;
    }

    /**
     * Recalculates the session result and stores it with an explicit status.
     * The appeal flow uses this to land on FINAL after publishing a re-grade;
     * grading (single-argument overload, status=null) resolves the status itself
     * via {@link #resolveDefaultStatus}.
     */
    @Transactional
    public ExamCandidateResult execute(UUID sessionId, ExamCandidateResultStatus status) {
        var calculated = examSessionResultCalculator.calculate(sessionId);
        var existing = examCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        var result = existing == null ? new ExamCandidateResult() : existing;
        var now = OffsetDateTime.now();
        var resolvedStatus = status != null
            ? status
            : resolveDefaultStatus(sessionId, calculated.candidateId(), calculated.anyRequiresHumanReview());

        result.setExamId(calculated.examId());
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
        if (resolvedStatus == ExamCandidateResultStatus.RELEASED && result.getReleasedAt() == null) {
            result.setReleasedAt(now);
        }
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        return examCandidateResultRepository.save(result);
    }
}
