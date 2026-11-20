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
            result.add(new ResultStatusHistoryInfo(
                row.get(0, UUID.class),
                row.get(1, UUID.class),
                row.get(2, String.class),
                row.get(3, String.class),
                row.get(4, BigDecimal.class),
                row.get(5, BigDecimal.class),
                row.get(6, String.class),
                row.get(7, UUID.class),
                row.get(8, String.class),
                row.get(9, String.class),
                row.get(10, Instant.class)
            ));
        }
        return result;
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
        List<Tuple> rows = em.createQuery("""
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

        int upheld = 0;
        int regraded = 0;
        int invalidated = 0;
        BigDecimal deltaSum = BigDecimal.ZERO;
        int deltaCount = 0;
        BigDecimal maxDelta = BigDecimal.ZERO;
        Map<UUID, TeacherAccumulator> perTeacher = new LinkedHashMap<UUID, TeacherAccumulator>();

        for (Tuple row : rows) {
            String outcome = row.get(2, String.class);
            // Skipped from the very start, not only removed from the total denominator: a teacher who returned the attempt
            // (or had it revoked after the deadline) has not read it and cannot say anything about the AI's
            // quality. Counting them in byTeacher.reviewed makes the sum of byTeacher > reviewed, and
            // makes someone who has not read look exactly like someone who reviewed afterwards and concluded the AI was right.
            if (outcome == null || GradingOutcome.DECLINED.name().equals(outcome)) {
                continue;
            }
            UUID teacherId = row.get(0, UUID.class);
            BigDecimal delta = absoluteDelta(row.get(3, BigDecimal.class), row.get(4, BigDecimal.class));

            TeacherAccumulator accumulator = perTeacher.computeIfAbsent(
                teacherId, ignored -> new TeacherAccumulator(row.get(1, String.class)));
            accumulator.reviewed++;

            if (GradingOutcome.UPHELD.name().equals(outcome)) {
                upheld++;
            } else if (GradingOutcome.REGRADED.name().equals(outcome)) {
                regraded++;
                accumulator.regraded++;
                if (delta != null) {
                    deltaSum = deltaSum.add(delta);
                    deltaCount++;
                    accumulator.deltaSum = accumulator.deltaSum.add(delta);
                    accumulator.deltaCount++;
                    if (delta.compareTo(maxDelta) > 0) {
                        maxDelta = delta;
                    }
                }
            } else if (GradingOutcome.INVALIDATED.name().equals(outcome)) {
                invalidated++;
            }
        }

        // The denominator skips returned rounds (DECLINED): a teacher who did not read the attempt
        // cannot say anything about the AI's quality.
        int reviewed = upheld + regraded + invalidated;
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
            upheld,
            regraded,
            invalidated,
            ratio(regraded, reviewed),
            average(deltaSum, deltaCount),
            deltaCount == 0 ? null : maxDelta,
            byTeacher
        );
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

    /** Tỉ lệ theo phần trăm, 2 chữ số — FE hiển thị thẳng, không tự chia lại. */
    private BigDecimal ratio(int part, int total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_UP);
    }
}
