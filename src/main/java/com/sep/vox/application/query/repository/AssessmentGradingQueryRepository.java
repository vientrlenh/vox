package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingAssessmentOptionInfo;
import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * The read side for manual grading. All of it is cross-aggregate joins
 * (assignment x candidate_result x session x assessment x users x evaluations), so it goes the
 * query repository route instead of the domain repository.
 *
 * <p>After the rework there is <strong>no</strong> longer a hard constraint "only PENDING_REVIEW": the admin
 * coordinates all four rounds so they must see attempts in every status. The scope is narrowed by
 * {@link GradingAssignmentFilter}, not by a constant hidden in the repository.
 *
 * <p>0-based paging, in line with {@code JpaAssessmentAppealQueryRepository}.
 */
public interface AssessmentGradingQueryRepository {

    /** The school admin's dispatch table: every submission in the filter scope, including unassigned ones. */
    PageResult<GradingAssignmentRowInfo> searchAssignments(GradingAssignmentFilter filter, int page, int size);

    /** @param assessmentKind {@code CENTRALIZED} / {@code CLASS_TEST}; {@code null} = both. */
    GradingStatsInfo stats(UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind);

    /**
     * The teacher's queue -- one list for all four rounds. Anonymous.
     *
     * <p>{@code assessmentKind} is mandatory in business terms even though the type allows null: the queue of
     * centralized assessments and the queue of class tests are two separate screens, and mixing the two
     * kinds into one list is a bug that was fixed, not a feature.
     */
    PageResult<GradingTaskInfo> findTasksByTeacherId(
        UUID teacherId, String assessmentKind, String status, String roundType, int page, int size);

    PageResult<GradingTaskInfo> findTasksByTeacherIdAndAssessmentId(
        UUID teacherId, UUID assessmentId, String assessmentKind, String status, String roundType, int page, int size);

    /**
     * The assessments in which this teacher CURRENTLY has or HAS HAD a grading assignment, with the
     * number of submissions per assessment. The source of the assessment filter on the teacher queue.
     *
     * <p>Does not reuse the school's assessment read path: the visibility conditions there are system
     * admin / school admin / assessment member / closed assessment, and a teacher assigned to grade
     * falls into none of those -- their permission comes from the assignment row itself, so the scope
     * of the dropdown must be derived from it too.
     *
     * <p>Not paged: this set is bounded by the number of assignments of ONE person, and a paged dropdown
     * cannot build the label of the selected entry if it falls on a later page.
     */
    List<GradingAssessmentOptionInfo> findAssessmentsWithTasksByTeacherId(UUID teacherId, String assessmentKind);

    /** The grading screen. Returns empty if the caller is not the teacher assigned to this submission. */
    Optional<GradingTaskDetailInfo> findTaskDetail(UUID assignmentId, UUID teacherId);

    List<AssignableTeacherInfo> findAssignableTeachers(UUID schoolId, String keyword);

    /**
     * Within {@code userIds}, those who are ACTIVE teachers of {@code schoolId}.
     * Used to validate a whole group of manual / auto-assign in one query, instead of calling
     * {@code isTeacherOfSchool} once per person (N+1).
     */
    Set<UUID> findTeacherIdsInSchool(UUID schoolId, Collection<UUID> userIds);

    /**
     * The number of OPEN assignments each teacher currently holds, regardless of round. Auto-assign
     * uses this as the starting point so that a second run does not pile everything onto the first person.
     */
    Map<UUID, Long> assignedLoadByTeacherIds(Collection<UUID> teacherIds);

    /**
     * Submissions eligible for a grading round that have NO open assignment yet.
     *
     * <p>The valid statuses are decided by {@code GradingRoundPolicy} and passed in -- the business
     * rule does not live in SQL. A submission with an open appeal is excluded in every round other than
     * {@code APPEAL}: two flows writing scores to one submission is the source of review BE-4.
     *
     * <p>ONLY centralized assessments. Auto-assign is an action of the school, and the school does not
     * assign in-class tests -- if they leaked in here, every run would pick up a batch of submissions
     * and then be blocked by {@code rejectClassTestCoordination} in the lower layer.
     */
    List<UUID> findAssignableResultIds(
        UUID schoolId, UUID assessmentId, UUID scheduleId, Collection<String> resultStatuses);

    /**
     * The risk signals of a set of submissions, one query for the whole set. Ranking is done in
     * {@code GradingSampleSelector}, not in SQL.
     */
    List<GradingRiskInfo> findRiskInfos(Collection<UUID> candidateResultIds);

    /**
     * The teachers who have ever written an {@code AssessmentItemEvaluation} with engine HUMAN for this attempt
     * -- i.e. who have already given their own score verdict and are no longer impartial for an appeal grading.
     *
     * <p>A person who only {@code UPHOLD}s (confirms the old score, without writing a new version) is NOT in this
     * set: they have no verdict of their own to defend.
     */
    Set<UUID> findTeacherIdsWithHumanEvaluation(UUID candidateResultId);

    /**
     * A snapshot of the grading state of an assessment, for the admin to decide before closing out.
     * This is the way out of the situation where one stuck submission blocks the whole assessment (review BE-5).
     */
    BulkFinalizePreviewInfo previewBulkFinalize(UUID schoolId, UUID assessmentId);

    /**
     * The score sheet for file export. Not paged -- an export exports everything, so the caller must
     * fix the scope (assessment or schedule) itself before getting here.
     *
     * <p>Takes the same {@link GradingAssignmentFilter} as {@link #searchAssignments} rather than three
     * loose parameters: the exported file must be exactly what the user is looking at in the table, and
     * the only way to guarantee that is for both sides to share one set of predicates.
     */
    List<AssessmentScoreRowInfo> findScoreRows(GradingAssignmentFilter filter);
}