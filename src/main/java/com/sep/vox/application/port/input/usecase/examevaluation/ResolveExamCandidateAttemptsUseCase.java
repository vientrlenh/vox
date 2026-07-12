package com.sep.vox.application.port.input.usecase.examevaluation;

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

import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.dto.ExamCandidateAttempts;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class ResolveExamCandidateAttemptsUseCase {

    private final ExamCandidateAttemptsQueryRepository examCandidateAttemptsQueryRepository;
    private final ExamRepository examRepository;

    public ResolveExamCandidateAttemptsUseCase(
            ExamCandidateAttemptsQueryRepository examCandidateAttemptsQueryRepository,
            ExamRepository examRepository) {
        this.examCandidateAttemptsQueryRepository = examCandidateAttemptsQueryRepository;
        this.examRepository = examRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, ExamCandidateAttempts> executeBatch(Collection<UUID> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Map.of();
        }

        var rows = examCandidateAttemptsQueryRepository.findByCandidateIds(candidateIds);
        var rowsByCandidateId = rows.stream().collect(Collectors.groupingBy(ExamAttemptSummary::candidateId));

        var examIds = rows.stream().map(ExamAttemptSummary::examId).distinct().toList();
        var examsById = examRepository.findByIdIn(examIds).stream()
            .collect(Collectors.toMap(exam -> exam.getId(), Function.identity(), (left, right) -> left));

        var out = new HashMap<UUID, ExamCandidateAttempts>();
        for (var candidateId : candidateIds) {
            var candidateRows = rowsByCandidateId.getOrDefault(candidateId, List.of());
            var attempts = candidateRows.stream()
                .sorted(Comparator.comparing(ExamAttemptSummary::startedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

            var exam = candidateRows.isEmpty() ? null : examsById.get(candidateRows.get(0).examId());
            var method = exam == null || exam.getResultDecisionMethod() == null
                ? ResultDecisionMethod.LATEST
                : exam.getResultDecisionMethod();
            out.put(candidateId, resolveOfficial(attempts, method));
        }
        return out;
    }

    private ExamCandidateAttempts resolveOfficial(List<ExamAttemptSummary> attempts, ResultDecisionMethod method) {
        var graded = attempts.stream()
            .filter(attempt -> attempt.totalScore() != null)
            .toList();

        if (method == ResultDecisionMethod.AVERAGE) {
            if (graded.isEmpty()) {
                return new ExamCandidateAttempts(attempts, null, null);
            }
            var average = graded.stream()
                .map(ExamAttemptSummary::totalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(graded.size()), 2, RoundingMode.HALF_UP);
            return new ExamCandidateAttempts(attempts, graded.get(0), average);
        }

        ExamAttemptSummary official;
        switch (method) {
            case HIGHEST -> official = graded.stream()
                .max(Comparator.comparing(ExamAttemptSummary::totalScore))
                .orElse(null);
            case LOWEST -> official = graded.stream()
                .min(Comparator.comparing(ExamAttemptSummary::totalScore))
                .orElse(null);
            case FIRST -> official = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
            case LATEST -> official = attempts.isEmpty() ? null : attempts.get(0);
            default -> official = attempts.isEmpty() ? null : attempts.get(0);
        }

        return new ExamCandidateAttempts(
            attempts,
            official,
            official == null ? null : official.totalScore()
        );
    }
}
