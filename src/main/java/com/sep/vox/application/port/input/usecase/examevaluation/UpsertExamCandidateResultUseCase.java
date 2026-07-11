package com.sep.vox.application.port.input.usecase.examevaluation;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;

@Service
public class UpsertExamCandidateResultUseCase {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionResultCalculator examSessionResultCalculator;

    public UpsertExamCandidateResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionResultCalculator examSessionResultCalculator) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionResultCalculator = examSessionResultCalculator;
    }

    @Transactional
    public ExamCandidateResult execute(UUID sessionId) {
        var calculated = examSessionResultCalculator.calculate(sessionId);
        var existing = examCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        var result = existing == null ? new ExamCandidateResult() : existing;
        var now = OffsetDateTime.now();

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
        result.setStatus(ExamCandidateResultStatus.PENDING_REVIEW);
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        return examCandidateResultRepository.save(result);
    }
}
