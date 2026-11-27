package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaAssessmentResultAuditQueryRepository implements AssessmentResultAuditQueryRepository {

    private static final int SCALE = 2;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ResultStatusHistoryInfo> findHistory(UUID candidateResultId) {
        List<Tuple> rows = em.createQuery("""
            SELECT h.id, h.candidateResultId, h.fromStatus, h.toStatus, h.scoreBefore, h.scoreAfter,
                   h.source, h.actorId, u.fullName, h.reason, h.createdAt
            FROM AssessmentResultStatusHistoryJpaEntity h
            LEFT JOIN UserJpaEntity u ON u.id = h.actorId
            WHERE h.candidateResultId = :candidateResultId
            ORDER BY h.createdAt ASC
        """, Tuple.class)
            .setParameter("candidateResultId", candidateResultId)
            .getResultList();

        List<ResultStatusHistoryInfo> result = new ArrayList<ResultStatusHistoryInfo>();
        for (Tuple row : rows) {
            result.add(toStatusHistory(row));
        }
        return result;
    }

    private ResultStatusHistoryInfo toStatusHistory(Tuple row) {
        String fromStatus = row.get(2, String.class);
        return new ResultStatusHistoryInfo(
            row.get(0, UUID.class),
            row.get(1, UUID.class),
            fromStatus == null ? null : AssessmentCandidateResultStatus.valueOf(fromStatus),
            AssessmentCandidateResultStatus.valueOf(row.get(3, String.class)),
            row.get(4, BigDecimal.class),
            row.get(5, BigDecimal.class),
            row.get(6, String.class),
            row.get(7, UUID.class),
            row.get(8, String.class),
            row.get(9, String.class),
            row.get(10, Instant.class)
        );
    }

    /**
     * One query fetches every finished post-review round, then aggregates in Java.
     *
     * <p>Deliberately not grouped in SQL: the deviation must be computed on the assignment row's {@code scoreBefore}
     * against the attempt's <em>current</em> {@code totalScore}, and that expression plus
     * average/max/split by teacher would become an SQL statement nobody can re-read.
     * The number of rows here is the number of post-reviewed attempts of one assessment — small.
     */
    @Override
    public AiQualityReportInfo aiQualityReport(UUID schoolId, UUID assessmentId) {
        List<Tuple> rows = findCompletedSpotCheckRows(schoolId, assessmentId);
        QualityTotals totals = new QualityTotals();
        Map<UUID, TeacherAccumulator> perTeacher = new LinkedHashMap<UUID, TeacherAccumulator>();
        for (Tuple row : rows) {
            accumulateOutcome(row, totals, perTeacher);
        }
        return toAiQualityReport(totals, perTeacher);
    }

    private List<Tuple> findCompletedSpotCheckRows(UUID schoolId, UUID assessmentId) {
        return em.createQuery("""
            SELECT ga.teacherId, t.fullName, ga.outcome, ga.scoreBefore, cr.totalScore
            FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE e.schoolId = :schoolId
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND ga.roundType = :spotCheck
            AND ga.status = :completed
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("spotCheck", GradingRoundType.SPOT_CHECK.name())
            .setParameter("completed", GradingAssignmentStatus.COMPLETED.name())
            .getResultList();
    }

    /**
     * A returned (DECLINED) or outcome-less round is skipped from the very start, not only removed from the total
     * denominator: a teacher who returned the attempt (or had it revoked after the deadline) has not read it and cannot
     * say anything about the AI's quality. Counting them in byTeacher.reviewed makes the sum of byTeacher > reviewed, and
     * makes someone who has not read look exactly like someone who reviewed afterwards and concluded the AI was right.
     */
    private void accumulateOutcome(Tuple row, QualityTotals totals, Map<UUID, TeacherAccumulator> perTeacher) {
        String outcome = row.get(2, String.class);
        if (outcome == null || GradingOutcome.DECLINED.name().equals(outcome)) {
            return;
        }
        BigDecimal delta = absoluteDelta(row.get(3, BigDecimal.class), row.get(4, BigDecimal.class));
        TeacherAccumulator accumulator = perTeacher.computeIfAbsent(
            row.get(0, UUID.class), ignored -> new TeacherAccumulator(row.get(1, String.class)));
        accumulator.reviewed++;

        if (GradingOutcome.UPHELD.name().equals(outcome)) {
            totals.upheld++;
        } else if (GradingOutcome.REGRADED.name().equals(outcome)) {
            totals.regraded++;
            accumulator.regraded++;
            recordDelta(totals, accumulator, delta);
        } else if (GradingOutcome.INVALIDATED.name().equals(outcome)) {
            totals.invalidated++;
        }
    }

    private void recordDelta(QualityTotals totals, TeacherAccumulator accumulator, BigDecimal delta) {
        if (delta == null) {
            return;
        }
        totals.deltaSum = totals.deltaSum.add(delta);
        totals.deltaCount++;
        accumulator.deltaSum = accumulator.deltaSum.add(delta);
        accumulator.deltaCount++;
        if (delta.compareTo(totals.maxDelta) > 0) {
            totals.maxDelta = delta;
        }
    }

    private AiQualityReportInfo toAiQualityReport(QualityTotals totals, Map<UUID, TeacherAccumulator> perTeacher) {
        // The denominator skips returned rounds (DECLINED): a teacher who did not read the attempt
        // cannot say anything about the AI's quality.
        int reviewed = totals.upheld + totals.regraded + totals.invalidated;
        List<AiQualityReportInfo.ByTeacher> byTeacher = new ArrayList<AiQualityReportInfo.ByTeacher>();
        perTeacher.forEach((teacherId, accumulator) -> byTeacher.add(new AiQualityReportInfo.ByTeacher(
            teacherId,
            accumulator.teacherName,
            accumulator.reviewed,
            accumulator.regraded,
            average(accumulator.deltaSum, accumulator.deltaCount)
        )));

        return new AiQualityReportInfo(
            reviewed,
            totals.upheld,
            totals.regraded,
            totals.invalidated,
            ratio(totals.regraded, reviewed),
            average(totals.deltaSum, totals.deltaCount),
            totals.deltaCount == 0 ? null : totals.maxDelta,
            byTeacher
        );
    }

    private static final class QualityTotals {
        private int upheld;
        private int regraded;
        private int invalidated;
        private BigDecimal deltaSum = BigDecimal.ZERO;
        private int deltaCount;
        private BigDecimal maxDelta = BigDecimal.ZERO;
    }

    private static final class TeacherAccumulator {
        private final String teacherName;
        private int reviewed;
        private int regraded;
        private BigDecimal deltaSum = BigDecimal.ZERO;
        private int deltaCount;

        private TeacherAccumulator(String teacherName) {
            this.teacherName = teacherName;
        }
    }

    private BigDecimal absoluteDelta(BigDecimal before, BigDecimal after) {
        if (before == null || after == null) {
            return null;
        }
        return after.subtract(before).abs();
    }

    private BigDecimal average(BigDecimal sum, int count) {
        return count == 0 ? null : sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
    }

    /** Percentage with 2 decimal places -- the FE shows it as is and does not divide again. */
    private BigDecimal ratio(int part, int total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_UP);
    }
}
