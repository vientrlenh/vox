package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssessmentAwaitingPublishDto;
import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Two questions about the SAME set of submissions: "how many submissions in the whole school still have no score" and
 * "which assessment is about to publish while still having empty submissions".
 *
 * <p>Put in one port because the two must run on the same predicate. Split into two places, the total card says 37
 * while the assessment cards add up to 41, and nobody knows which side is right.
 *
 * <p>The scope is CENTRALIZED assessments. In-class tests are coordinated by the teacher who wrote them and have their own screen
 * ({@code classTestGradingStats}); merging them in would make the number on the overview page disagree
 * with the dispatch table it links to.
 */
public interface SchoolWorkloadQueryRepository {

    /**
     * @param now the moment used to judge overdue. Passed in rather than taking {@code Instant.now()} itself: the two queries of
     *            this port must use the SAME moment, otherwise one submission could be both "within the deadline" on the
     *            total card and "overdue" on the assessment card in the same page load.
     */
    SchoolUnscoredWorkloadDto countUnscored(UUID schoolId, Instant now);

    /** The assessment with the most empty submissions comes first -- it is the one that does the most damage when publish is clicked. */
    List<AssessmentAwaitingPublishDto> findAssessmentsAwaitingPublish(UUID schoolId, Instant now, int limit);

    /**
     * The list of AI-failed attempts that nobody has handled yet -- the landing place of the line of the same name on the overview page.
     *
     * <p>Runs on EXACTLY the two AI groups of {@link #countUnscored}, so the number on the card and the number of rows here always
     * match. It does not take {@code now}: the two AI groups do not depend on any moment in time (only the assignment
     * group considers overdue), so adding a parameter here would wrongly imply that it has an effect.
     *
     * @param assessmentId narrows to one assessment; null is every assessment.
     * @param retryLeft    true = only attempts with AI retries left, false = only attempts that have run out, null = no filter.
     *                     This is the most valuable filter on this screen: the two groups lead to two very different
     *                     actions, and mixing them means every row has to be read separately to know which
     *                     button can be pressed.
     */
    PageResult<SchoolGradingFailureDto> findUnhandledAiFailures(
        UUID schoolId, UUID assessmentId, Boolean retryLeft, int page, int size);

    /**
     * The number of submissions in the two quota groups, with the assessment filter ALREADY applied but the quota filter NOT yet applied.
     *
     * <p>These two numbers sit on the filter buttons themselves, so they must predict the result of the click correctly:
     * taking the school-wide total while the list is filtered to one assessment would give a button that says 9 but shows
     * only 3 rows when clicked.
     *
     * @return {@code [submissions with AI attempts left, submissions out of AI attempts]}
     */
    int[] countAiFailuresByAllowance(UUID schoolId, UUID assessmentId);
}
