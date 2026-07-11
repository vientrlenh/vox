package com.sep.vox.application.port.input.usecase.examevaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

@Service
public class ResolveExamCandidateAttemptsUseCase {

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final RubricResultBandRepository rubricResultBandRepository;

    public ResolveExamCandidateAttemptsUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            RubricResultBandRepository rubricResultBandRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, ExamCandidateAttempts> executeBatch(Collection<UUID> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Map.of();
        }

        var sessions = examSessionRepository.findAllByCandidateIdIn(candidateIds);
        var sessionsByCandidateId = sessions.stream()
            .collect(Collectors.groupingBy(ExamSession::getCandidateId));

        var sessionIds = sessions.stream().map(ExamSession::getId).toList();
        var resultBySessionId = examCandidateResultRepository.findBySessionIdIn(sessionIds).stream()
            .collect(Collectors.toMap(ExamCandidateResult::getSessionId, Function.identity(), (left, right) -> left));

        var examIds = sessions.stream().map(ExamSession::getExamId).distinct().toList();
        var examsById = examRepository.findByIdIn(examIds).stream()
            .collect(Collectors.toMap(exam -> exam.getId(), Function.identity(), (left, right) -> left));

        var rubricBandIds = resultBySessionId.values().stream()
            .map(ExamCandidateResult::getRubricResultBandId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        var rubricBandsById = rubricResultBandRepository.findByIdIn(rubricBandIds).stream()
            .collect(Collectors.toMap(band -> band.getId(), Function.identity(), (left, right) -> left));

        var out = new HashMap<UUID, ExamCandidateAttempts>();
        for (var candidateId : candidateIds) {
            var candidateSessions = sessionsByCandidateId.getOrDefault(candidateId, List.of());
            var attempts = candidateSessions.stream()
                .sorted(Comparator.comparing(ExamSession::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(session -> toSummary(session, resultBySessionId.get(session.getId()), rubricBandsById))
                .toList();

            var exam = candidateSessions.isEmpty() ? null : examsById.get(candidateSessions.get(0).getExamId());
            var method = exam == null || exam.getResultDecisionMethod() == null
                ? ResultDecisionMethod.LATEST
                : exam.getResultDecisionMethod();
            out.put(candidateId, resolveOfficial(attempts, method));
        }
        return out;
    }

    private ExamAttemptSummary toSummary(
            ExamSession session,
            ExamCandidateResult result,
            Map<UUID, com.sep.vox.domain.model.rubric.RubricResultBand> rubricBandsById) {
        var rubricBand = result == null || result.getRubricResultBandId() == null
            ? null
            : rubricBandsById.get(result.getRubricResultBandId());
        return new ExamAttemptSummary(
            session.getId(),
            session.getStartedAt(),
            session.getSubmittedAt(),
            session.getStatus(),
            result == null ? null : result.getTotalScore(),
            result == null ? null : result.getRubricResultBandId(),
            rubricBand == null ? null : rubricBand.getCode(),
            rubricBand == null ? null : rubricBand.getName()
        );
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

    public record ExamAttemptSummary(
        UUID sessionId,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt,
        ExamSessionStatus status,
        BigDecimal totalScore,
        UUID rubricResultBandId,
        String rubricResultBandCode,
        String rubricResultBandName
    ) {
    }

    public record ExamCandidateAttempts(
        List<ExamAttemptSummary> attempts,
        ExamAttemptSummary officialAttempt,
        BigDecimal officialScore
    ) {
    }
}
