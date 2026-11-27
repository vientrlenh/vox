package com.sep.vox.application.response.input.dashboard;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.domain.common.PageResult;

/**
 * The "schools needing attention" page: the counts of all four buckets, plus the list of the open bucket.
 *
 * <p>The four counts come in the SAME response, and from the VERY count the overview page uses --
 * not recounted with another query. This is the easiest thing to break on this screen: the card on the overview page
 * says 5, the user clicks it, and if the number here were built with a different filter they would see 9.
 *
 * @param counts the counts per bucket, in the same semantic order as {@link SchoolRiskBucket}; do NOT add them up
 */
public record SchoolsAtRiskResponse(
    SchoolRiskBucket bucket,
    BucketCounts counts,
    PageResult<SchoolAtRiskDto> schools
) {

    public record BucketCounts(
        long expiringSoon,
        long lapsed,
        long suspended,
        long inDebt
    ) {
    }
}
