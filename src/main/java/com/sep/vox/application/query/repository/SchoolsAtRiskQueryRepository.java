package com.sep.vox.application.query.repository;

import java.time.Instant;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.domain.common.PageResult;

/**
 * The list of schools in one "needs attention" bucket -- where the four rows of the card of the same name on the
 * system overview page land.
 *
 * <p>Does NOT reuse the existing "Schools &amp; plans" screen for this, even though it also filters by status: that
 * screen filters by subscription ROW while the card counts by SCHOOL. A school whose 2024 period has ended and whose 2025 period is
 * running would be in that screen's {@code status=EXPIRED} list but does NOT belong to the "expired" bucket
 * -- click a card that says 5 and see 9 rows, and no number is trustworthy any more.
 *
 * <p>So the predicate here is a WORD-FOR-WORD copy of
 * {@link PlatformBusinessHealthQueryRepository#countSchoolSubscriptionHealth}: collapse to one row per
 * school first, and only then assign buckets.
 */
public interface SchoolsAtRiskQueryRepository {

    /**
     * @param now             the moment used to decide which subscription period is covering -- pass the same value that was used to
     *                        count, otherwise a period that expires between the two calls will make the count and the
     *                        list disagree
     * @param expiringThrough the expiring-soon warning threshold, INCLUSIVE; only used for
     *                        {@link SchoolRiskBucket#EXPIRING_SOON}
     */
    PageResult<SchoolAtRiskDto> findByBucket(
        SchoolRiskBucket bucket, Instant now, Instant expiringThrough, String keyword, int page, int size);
}
