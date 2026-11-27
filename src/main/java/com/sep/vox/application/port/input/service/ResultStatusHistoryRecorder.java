package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentResultStatusHistoryRepository;

/**
 * The ONLY audit write point for every status change / score change of an attempt.
 *
 * <p>Having two write points means losing the audit: a score dispute would land exactly where nothing is recorded.
 * Every use case that touches {@link AssessmentCandidateResult#setStatus} or changes the total score
 * has to go through here.
 *
 * <p>Usage: take {@link #snapshot(AssessmentCandidateResult)} <em>before</em> editing,
 * call {@link #record} once the new result exists.
 */
@Service
@RequiredArgsConstructor
public class ResultStatusHistoryRecorder {

    private final AssessmentResultStatusHistoryRepository assessmentResultStatusHistoryRepository;

    /** Snapshot of the status + score before the action. */
    public record Snapshot(UUID candidateResultId, AssessmentCandidateResultStatus status, BigDecimal totalScore) {
    }

    public Snapshot snapshot(AssessmentCandidateResult result) {
        if (result == null) {
            return null;
        }
        return new Snapshot(result.getId(), result.getStatus(), result.getTotalScore());
    }

    /**
     * Writes a history row if anything actually changed (status or score).
     *
     * <p>Returns {@code null} when nothing changed — a "keep as is" action in the post-review
     * round creates no junk row, but the fact that the teacher confirmed still lives in
     * the {@code outcome} of the assignment row.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentResultStatusHistory record(Snapshot before, AssessmentCandidateResult after,
            ResultStatusChangeSource source, UUID actorId, String reason) {
        if (before == null || after == null) {
            return null;
        }
        boolean statusChanged = before.status() != after.getStatus();
        boolean scoreChanged = !equalScores(before.totalScore(), after.getTotalScore());
        if (!statusChanged && !scoreChanged) {
            return null;
        }
        return assessmentResultStatusHistoryRepository.save(AssessmentResultStatusHistory.transition(
                after.getId(), before.status(), after.getStatus(), before.totalScore(), after.getTotalScore(),
                source, actorId, reason, Instant.now()));
    }

    /**
     * Writes directly, even when nothing changed. Used for decisions that must leave a
     * trace even though the status stays still — for example REMEDIATION keeping INVALID
     * (confirming a violation) or a teacher returning an assignment.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentResultStatusHistory recordAlways(Snapshot before, AssessmentCandidateResult after,
            ResultStatusChangeSource source, UUID actorId, String reason) {
        if (before == null || after == null) {
            return null;
        }
        return assessmentResultStatusHistoryRepository.save(AssessmentResultStatusHistory.transition(
                after.getId(), before.status(), after.getStatus(), before.totalScore(), after.getTotalScore(),
                source, actorId, reason, Instant.now()));
    }

    /** Bulk write — closing out a whole assessment must not spawn N single inserts. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<AssessmentResultStatusHistory> recordAll(List<AssessmentResultStatusHistory> histories) {
        return assessmentResultStatusHistoryRepository.saveAll(histories);
    }

    /**
     * Compares scores by VALUE, not by {@code equals}: {@code BigDecimal("7.0")} and
     * {@code BigDecimal("7.00")} are not equal yet are the same score — using
     * equals here would spawn a ghost audit row on every recomputation.
     */
    private boolean equalScores(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return Objects.equals(left, right);
        }
        return left.compareTo(right) == 0;
    }
}
