package com.sep.vox.infrastructure.persistence.query;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;
import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaPlatformOperationalHealthQueryRepository implements PlatformOperationalHealthQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * One table scan for all three numbers thanks to Postgres {@code COUNT(...) FILTER (WHERE ...)},
     * instead of three separate COUNT statements. The outermost WHERE clause is still needed: it lets Postgres skip every
     * finished session (GRADED / EXPIRED / GRADING_FAILED), which makes up more and more of the table over time.
     */
    @Override
    public LiveSessionCountsDto countLiveSessions() {
        Object[] row = (Object[]) em.createNativeQuery("""
            SELECT
                COUNT(*) FILTER (WHERE status = 'IN_PROGRESS'),
                COUNT(DISTINCT assessment_id) FILTER (WHERE status = 'IN_PROGRESS'),
                COUNT(*) FILTER (WHERE status IN ('SUBMITTED', 'GRADING'))
            FROM assessment_sessions
            WHERE status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADING')
            """)
            .getSingleResult();

        return new LiveSessionCountsDto(toLong(row[0]), toLong(row[1]), toLong(row[2]));
    }

    /**
     * Groups by day in the DB instead of loading sessions to count in Java: the number of sessions only grows over time,
     * while the result is always at most a few dozen rows.
     *
     * <p>{@code submitted_at AT TIME ZONE :zone} converts the timestamptz to the local time of the zone passed
     * in before truncating the day -- not a bare {@code date_trunc} like
     * {@code JpaSchoolAiCostQueryRepository}, because a bare truncation cuts by UTC.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GradingOutcomeBucketDto> findGradingOutcomeByDay(Instant from, Instant to, ZoneId zone) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                CAST(date_trunc('day', submitted_at AT TIME ZONE CAST(:zone AS varchar)) AS date) AS day,
                COUNT(*) FILTER (WHERE status = 'GRADED'),
                COUNT(*) FILTER (WHERE status = 'GRADING_FAILED')
            FROM assessment_sessions
            WHERE status IN ('GRADED', 'GRADING_FAILED')
              AND submitted_at >= :fromInstant
              AND submitted_at < :toInstant
            GROUP BY day
            ORDER BY day ASC
            """)
            .setParameter("zone", zone.getId())
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getResultList();

        return rows.stream()
            .map(row -> new GradingOutcomeBucketDto(toLocalDate(row[0]), toLong(row[1]), toLong(row[2])))
            .toList();
    }

    /**
     * The Postgres COUNT arriving here can be a {@code Long} or a {@code BigInteger} depending on the
     * driver/Hibernate version, so it is forced through {@link Number} instead of a direct cast -- the same kind of trap that caused
     * a ClassCastException in {@code JpaSchoolAiCostQueryRepository}.
     */
    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /** The {@code date} column arrives as a {@link LocalDate} or a {@link Date} depending on the version. */
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException("Cannot read a date column of type " + value.getClass());
    }
}
