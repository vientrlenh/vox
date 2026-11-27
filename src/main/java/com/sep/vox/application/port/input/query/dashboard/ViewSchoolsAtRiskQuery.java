package com.sep.vox.application.port.input.query.dashboard;

import com.sep.vox.application.query.dto.SchoolRiskBucket;

/**
 * @param bucket  the open bucket; required -- this page always sits inside exactly one bucket
 * @param keyword search by school name or code; empty = no filter
 */
public record ViewSchoolsAtRiskQuery(
    SchoolRiskBucket bucket,
    String keyword,
    int page,
    int size
) {
}
