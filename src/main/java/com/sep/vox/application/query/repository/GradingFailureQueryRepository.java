package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Reads the AI-failed assessment sessions, ACROSS assessments and ACROSS schools.
 *
 * <p>There is no existing path to ask this question: the current results screen starts from ONE assessment
 * ({@code AssessmentResultsListPage} reads {@code assessmentId} from the URL and then filters on the client), so it cannot
 * answer "how many sessions are broken across the whole system, and for what causes".
 *
 * <p>It is a REPORT, in the same mold as {@link PlatformOperationalHealthQueryRepository} -- gathered here
 * instead of adding a pile of {@code findBy...} to {@code AssessmentSessionRepository}, which is the business gateway
 * of the assessment session and not a place to serve a table.
 *
 * <p>EVERY function here uses ONE SHARED predicate: {@code status = 'GRADING_FAILED'} and {@code submitted_at}
 * in the half-open range {@code [from, to)} -- exactly the predicate that
 * {@link PlatformOperationalHealthQueryRepository#findGradingOutcomeByDay} counts for the card on the
 * overview page. Differ in one detail and two screens side by side show two different numbers.
 */
public interface GradingFailureQueryRepository {

    GradingFailureTotalsDto countTotals(Instant from, Instant to);

    /**
     * Groups by failure signature, the largest group first.
     *
     * @param limit the cap on the number of groups returned. There is a cap because the premise "one incident = one group"
     *              depends on whether message normalization works; if the grading service produces very varied
     *              messages, the number of groups can swell to the number of sessions, and the page must not
     *              pull in a thousand rows just to show the first ten.
     */
    List<GradingFailureGroupDto> findGroups(Instant from, Instant to, int limit);

    /**
     * The sessions in ONE group.
     *
     * @param signature the group's signature; {@code null} selects exactly the "unknown cause" group and does not
     *                  mean "no filter" -- compared with {@code IS NOT DISTINCT FROM} so null matches null
     */
    PageResult<GradingFailureSessionDto> findSessions(
        Instant from, Instant to, String signature, int page, int size);
}
