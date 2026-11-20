package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.query.dto.StudentAssessmentRowInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * The read side for the student's assessment list. A cross-aggregate join
 * (candidate x assessment x schedule), so it goes the query repository route instead of the domain repository.
 *
 * <p>Why paging has to be here and not in the use case: the previous version loaded ALL candidates of the
 * student, then every related assessment and schedule, filtered/sorted in memory and only then did {@code skip/limit}.
 * The cost of opening the screen was proportional to that student's lifetime number of assessments, while the screen only shows 20
 * rows. Pushed down to SQL, the filter, the sort key and the {@code COUNT} all run on indexes.
 *
 * <p>1-BASED paging: the first page is 1, matching the project-wide convention (see
 * {@code PagingConventionTests}). The conversion to JPA's 0-based counting lives in the adapter.
 */
public interface StudentAssessmentQueryRepository {

    /**
     * @param assessmentKind {@code CENTRALIZED} / {@code CLASS_TEST}; {@code null} = both.
     * @param derivedStatus  {@code upcoming} / {@code in_progress} / {@code completed};
     *                       {@code null} = no filter. This is the status DERIVED from the assessment time, not
     *                       the {@code status} column of the assessment.
     * @param now            the moment used to derive the status -- passed in so a single call
     *                       has exactly one "now", avoiding a mismatch between the count query and the row query.
     */
    PageResult<StudentAssessmentRowInfo> findMyAssessments(
        UUID studentId,
        String assessmentKind,
        String derivedStatus,
        boolean sortDescending,
        int page,
        int size,
        Instant now);
}
