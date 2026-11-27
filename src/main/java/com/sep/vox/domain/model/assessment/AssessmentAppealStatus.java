package com.sep.vox.domain.model.assessment;

/**
 * PENDING -&gt; APPROVED -&gt; GRADING -&gt; PUBLISHED
 * PENDING -&gt; REJECTED
 * PENDING -&gt; WITHDRAWN (the student withdraws, the appeal allowance is refunded)
 *
 * <p>{@code COMPARING} was dropped together with the admin publish step: there is only ONE appeal grader left,
 * so there is nothing to "compare" between reports, and the person with enough information to decide is the one
 * who just graded. When the teacher submits the score, it goes live right away.
 *
 * <p>{@code WITHDRAWN} is its own status and not a {@code withdrawn_at} flag on a PENDING appeal: a withdrawn
 * appeal must stop blocking new ones, and every "is there an open appeal" check reads {@code status}.
 *
 * <p>⚠️ The CHECK constraint on the {@code status} column must be changed with hand-written SQL —
 * {@code ddl-auto} cannot alter it. See
 * {@code AI/doc/migration/2026-07-26-grading-rework.sql}.
 */
public enum AssessmentAppealStatus {
    PENDING,    // the student submitted the appeal, awaiting review
    APPROVED,   // the school admin approved it, awaiting assignment of a grader
    GRADING,    // assigned, the teacher is re-grading
    PUBLISHED,  // the teacher submitted the score -> published right away
    REJECTED,   // the admin rejected it at the review step
    WITHDRAWN   // the student withdrew while the appeal was still PENDING
}
