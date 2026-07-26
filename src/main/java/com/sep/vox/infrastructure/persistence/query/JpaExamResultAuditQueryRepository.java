package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.ExamResultAuditQueryRepository;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaExamResultAuditQueryRepository implements ExamResultAuditQueryRepository {

    private static final int SCALE = 2;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ResultStatusHistoryInfo> findHistory(UUID candidateResultId) {
        var rows = em.createQuery("""
            SELECT h.id, h.candidateResultId, h.fromStatus, h.toStatus, h.scoreBefore, h.scoreAfter,
                   h.source, h.actorId, u.fullName, h.reason, h.createdAt
            FROM ExamResultStatusHistoryJpaEntity h
            LEFT JOIN UserJpaEntity u ON u.id = h.actorId
            WHERE h.candidateResultId = :candidateResultId
            ORDER BY h.createdAt ASC
        """, Tuple.class)
            .setParameter("candidateResultId", candidateResultId)
            .getResultList();

        var result = new ArrayList<ResultStatusHistoryInfo>();
        for (var row : rows) {
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
                row.get(10, OffsetDateTime.class)
            ));
        }
        return result;
    }

    /**
     * Một query lấy mọi vòng hậu kiểm đã xong, rồi tổng hợp trong Java.
     *
     * <p>Cố ý không gom nhóm trong SQL: độ lệch phải tính trên {@code scoreBefore} của
     * dòng phân công so với {@code totalScore} <em>hiện tại</em> của bài, và biểu thức
     * đó cộng thêm trung bình/lớn nhất/tách theo giáo viên sẽ thành một câu SQL không
     * ai đọc lại được. Số dòng ở đây là số bài đã hậu kiểm của một kỳ thi — nhỏ.
     */
    @Override
    public AiQualityReportInfo aiQualityReport(UUID schoolId, UUID examId) {
        var rows = em.createQuery("""
            SELECT ga.teacherId, t.fullName, ga.outcome, ga.scoreBefore, cr.totalScore
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE e.schoolId = :schoolId
            AND (:examId IS NULL OR cr.examId = :examId)
            AND ga.roundType = :spotCheck
            AND ga.status = :completed
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("spotCheck", GradingRoundType.SPOT_CHECK.name())
            .setParameter("completed", GradingAssignmentStatus.COMPLETED.name())
            .getResultList();

        var upheld = 0;
        var regraded = 0;
        var invalidated = 0;
        var deltaSum = BigDecimal.ZERO;
        var deltaCount = 0;
        var maxDelta = BigDecimal.ZERO;
        var perTeacher = new LinkedHashMap<UUID, TeacherAccumulator>();

        for (var row : rows) {
            var teacherId = row.get(0, UUID.class);
            var outcome = row.get(2, String.class);
            var delta = absoluteDelta(row.get(3, BigDecimal.class), row.get(4, BigDecimal.class));

            var accumulator = perTeacher.computeIfAbsent(
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

        // Mẫu số bỏ qua các vòng bị trả lại (DECLINED): giáo viên không đọc bài thì
        // không nói được gì về chất lượng AI.
        var reviewed = upheld + regraded + invalidated;
        var byTeacher = new ArrayList<AiQualityReportInfo.ByTeacher>();
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
