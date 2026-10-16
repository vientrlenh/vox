package com.sep.vox.application.port.input.usecase.assessmentevaluation;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.projection.dto.AssessmentAttemptSummary;
import com.sep.vox.application.projection.dto.AssessmentCandidateAttempts;
import com.sep.vox.application.projection.repository.AssessmentCandidateAttemptsQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultDecisionMethod;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class ResolveAssessmentCandidateAttemptsUseCase {

    private final AssessmentCandidateAttemptsQueryRepository assessmentCandidateAttemptsQueryRepository;
    private final AssessmentRepository assessmentRepository;

    public ResolveAssessmentCandidateAttemptsUseCase(
            AssessmentCandidateAttemptsQueryRepository assessmentCandidateAttemptsQueryRepository,
            AssessmentRepository assessmentRepository) {
        this.assessmentCandidateAttemptsQueryRepository = assessmentCandidateAttemptsQueryRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, AssessmentCandidateAttempts> executeBatch(Collection<UUID> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Map.of();
        }

        // Kèm cả lượt đã xoá mềm: đây là đường của màn kết quả kỳ thi, đã chặn quyền ở quản trị
        // trường / chủ tịch hội đồng (ViewAssessmentCandidatesUseCase), và họ cần thấy lượt bị xoá kèm lý
        // do. Điểm của lượt đã xoá KHÔNG được chọn làm điểm chính thức — xem countsTowardOfficialScore.
        var rows = assessmentCandidateAttemptsQueryRepository.findByCandidateIdsIncludingDeleted(candidateIds);
        var rowsByCandidateId = rows.stream().collect(Collectors.groupingBy((AssessmentAttemptSummary row) -> row.candidateId()));

        var assessmentIds = rows.stream().map(row -> row.assessmentId()).distinct().toList();
        var assessmentsById = assessmentRepository.findByIdIn(assessmentIds).stream()
            .collect(Collectors.toMap(assessment -> assessment.getId(), Function.identity(), (left, right) -> left));

        var out = new HashMap<UUID, AssessmentCandidateAttempts>();
        for (var candidateId : candidateIds) {
            var candidateRows = rowsByCandidateId.getOrDefault(candidateId, List.of());
            var attempts = candidateRows.stream()
                .sorted(Comparator.comparing((AssessmentAttemptSummary row) -> row.startedAt(), Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

            var assessment = candidateRows.isEmpty() ? null : assessmentsById.get(candidateRows.get(0).assessmentId());
            var method = assessment == null || assessment.getResultDecisionMethod() == null
                ? AssessmentResultDecisionMethod.LATEST
                : assessment.getResultDecisionMethod();
            out.put(candidateId, resolveOfficial(attempts, method));
        }
        return out;
    }

    private AssessmentCandidateAttempts resolveOfficial(List<AssessmentAttemptSummary> attempts, AssessmentResultDecisionMethod method) {
        var eligible = attempts.stream()
            .filter(this::countsTowardOfficialScore)
            .toList();

        if (method == AssessmentResultDecisionMethod.AVERAGE) {
            if (eligible.isEmpty()) {
                return new AssessmentCandidateAttempts(attempts, null, null);
            }
            var average = eligible.stream()
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
