package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;

/**
 * Reads the OPERATIONAL health of the platform from the {@code assessment_sessions} table, for the system admin dashboard.
 *
 * <p>It is a REPORT, not the write gateway of any aggregate -- in the same mold as
 * {@link SchoolAiCostQueryRepository} and {@link QuestionBankStatsQueryRepository}: it gathers many
 * sessions into numbers that correspond to no entity and has no write path back.
 *
 * <p>The source of truth is the column {@code assessment_sessions.status}, where {@code GRADING_FAILED} means the AI's grading
 * failed -- see {@code AssessmentSessionStatus}. It is not derived from {@code assessment_item_evaluations}: that table only has
 * a record once grading has SUCCEEDED, so counting there makes every failed grading invisible.
 */
public interface PlatformOperationalHealthQueryRepository {

    /** A snapshot at the moment of the call: sessions in progress, assessments with running sessions, and the grading queue. */
    LiveSessionCountsDto countLiveSessions();

    /**
     * The number of sessions graded / failed grading per calendar day, returning only days that HAVE data -- the
     * caller fills empty days with 0 itself.
     *
     * <p>Grouped by {@code submitted_at} and not {@code started_at}: a late-night session submitted on the next
     * day has its grading result belonging to the submission day, and that is also the only timestamp that every
     * graded session is certain to have.
     *
     * @param from the start, INCLUSIVE
     * @param to   the end, EXCLUSIVE -- two adjacent ranges would count a session that lands exactly on the boundary
     *             twice if {@code <=} were used; the same convention as {@code schoolBalanceSummary}
     * @param zone the time zone used to cut calendar days
     */
    List<GradingOutcomeBucketDto> findGradingOutcomeByDay(Instant from, Instant to, ZoneId zone);
}
