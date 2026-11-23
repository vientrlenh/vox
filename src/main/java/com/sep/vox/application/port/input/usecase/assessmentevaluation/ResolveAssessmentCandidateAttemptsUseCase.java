package com.sep.vox.application.port.input.usecase.assessmentevaluation;

import com.sep.vox.domain.model.assessment.Assessment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.AssessmentAttemptSummary;
import com.sep.vox.application.query.dto.AssessmentCandidateAttempts;
import com.sep.vox.application.query.repository.AssessmentCandidateAttemptsQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class ResolveAssessmentCandidateAttemptsUseCase {

    private final AssessmentCandidateAttemptsQueryRepository assessmentCandidateAttemptsQueryRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    public Map<UUID, AssessmentCandidateAttempts> executeBatch(Collection<UUID> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Map.of();
        }

        // Includes soft-deleted attempts too: this is the path of the assessment result screen, which is already restricted to the school
        // administrator / council chair (ViewAssessmentCandidatesUseCase), and they need to see a deleted attempt together with the
        // reason. The score of a deleted attempt MUST NOT be picked as the official score -- see countsTowardOfficialScore.
        List<AssessmentAttemptSummary> rows = assessmentCandidateAttemptsQueryRepository.findByCandidateIdsIncludingDeleted(candidateIds);
        Map<UUID, List<AssessmentAttemptSummary>> rowsByCandidateId = rows.stream().collect(Collectors.groupingBy((AssessmentAttemptSummary row) -> row.candidateId()));

        List<UUID> assessmentIds = rows.stream().map(row -> row.assessmentId()).distinct().toList();
        Map<UUID, Assessment> assessmentsById = assessmentRepository.findByIdIn(assessmentIds).stream()
            .collect(Collectors.toMap(assessment -> assessment.getId(), Function.identity(), (left, right) -> left));

        Map<UUID, AssessmentCandidateAttempts> out = new HashMap<UUID, AssessmentCandidateAttempts>();
        for (UUID candidateId : candidateIds) {
            List<AssessmentAttemptSummary> candidateRows = rowsByCandidateId.getOrDefault(candidateId, List.of());
            List<AssessmentAttemptSummary> attempts = candidateRows.stream()
                .sorted(Comparator.comparing((AssessmentAttemptSummary row) -> row.startedAt(), Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

            Assessment assessment = candidateRows.isEmpty() ? null : assessmentsById.get(candidateRows.get(0).assessmentId());
            ResultDecisionMethod method = assessment == null || assessment.getResultDecisionMethod() == null
                ? ResultDecisionMethod.LATEST
                : assessment.getResultDecisionMethod();
            out.put(candidateId, resolveOfficial(attempts, method));
        }
        return out;
    }

    private AssessmentCandidateAttempts resolveOfficial(List<AssessmentAttemptSummary> attempts, ResultDecisionMethod method) {
        List<AssessmentAttemptSummary> eligible = attempts.stream()
            .filter(this::countsTowardOfficialScore)
            .toList();

        if (method == ResultDecisionMethod.AVERAGE) {
            if (eligible.isEmpty()) {
                return new AssessmentCandidateAttempts(attempts, null, null);
            }
            BigDecimal average = eligible.stream()
                .map(attempt -> attempt.totalScore())
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right))
                .divide(BigDecimal.valueOf(eligible.size()), 2, RoundingMode.HALF_UP);
            return new AssessmentCandidateAttempts(attempts, eligible.get(0), average);
        }

        AssessmentAttemptSummary official;
        switch (method) {
            case HIGHEST -> official = eligible.stream()
                .max(Comparator.comparing((AssessmentAttemptSummary attempt) -> attempt.totalScore()))
                .orElse(null);
            case LOWEST -> official = eligible.stream()
                .min(Comparator.comparing((AssessmentAttemptSummary attempt) -> attempt.totalScore()))
                .orElse(null);
            case FIRST -> official = eligible.isEmpty() ? null : eligible.get(eligible.size() - 1);
            case LATEST -> official = eligible.isEmpty() ? null : eligible.get(0);
            default -> official = eligible.isEmpty() ? null : eligible.get(0);
        }

        return new AssessmentCandidateAttempts(
            attempts,
            official,
            official == null ? null : official.totalScore()
        );
    }

    private boolean countsTowardOfficialScore(AssessmentAttemptSummary attempt) {
        if (AssessmentCandidateStatus.isNonScorable(attempt.candidateStatus())) {
            return false;
        }
        if (attempt.totalScore() == null || attempt.resultStatus() == null) {
            return false;
        }
        return switch (attempt.resultStatus()) {
            case FINAL, RELEASED, PASSED, FAILED -> true;
            case PENDING_REVIEW -> !attempt.flagged();
            // DELETED: phiên đã xoá mềm tuyệt đối không được chọn làm điểm chính thức.
            case INVALID, RETAKE_REQUIRED, APPEALED, RE_GRADING, DELETED -> false;
        };
    }
}
