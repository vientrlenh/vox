package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;

/**
 * The read side for appeals. All of it is cross-aggregate joins
 * (appeal x candidate_result x users x assessments x paper_items x evaluations), so it goes the
 * query repository route instead of the domain repository.
 *
 * <p>After the rework there is NO separate grader queue any more: the appeal round is an
 * {@code assessment_grading_assignments} row like the other three rounds, so the teacher sees it in
 * {@code myGradingTasks} and grades it in {@code gradingTaskDetail}. This repository now only
 * serves the coordination screen of the school admin.
 *
 * <p>0-based paging, in line with {@code JpaSchoolUserQueryRepository}.
 */
public interface AssessmentAppealQueryRepository {

    /** {@code assessmentId} null = the whole school (the school admin's screen); with a value = one class test. */
    PageResult<AppealSummaryInfo> searchAppeals(
        UUID schoolId, UUID assessmentId, AssessmentAppealStatus status, String keyword, int page, int size);

    PageResult<AppealSummaryInfo> searchAppealsByStudentId(
        UUID studentId, AssessmentAppealStatus status, int page, int size);

    AppealStatsInfo countByStatus(UUID schoolId);

    Optional<AppealDetailInfo> findDetailById(UUID appealId, UUID schoolId);

    Optional<AppealDetailInfo> findStudentDetailById(UUID appealId, UUID studentId);

    /**
     * Appeal-grading candidates for ONE specific appeal, with the conflict-of-interest flag.
     *
     * <p>Needs {@code appealId} and not only {@code schoolId} like the old version: the conflict is
     * computed against <em>the submission of that appeal</em>, not against the school.
     */
    List<AppealReviewerLiteInfo> findAssignableReviewers(UUID schoolId, UUID appealId, String keyword);
}
