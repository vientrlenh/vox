package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.dto.AppealTaskDetailInfo;
import com.sep.vox.application.query.dto.AppealTaskInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Read side cho phúc khảo. Toàn bộ đều là join xuyên aggregate
 * (appeal x candidate_result x users x exams x paper_items x evaluations), nên đi
 * lối query repository thay vì domain repository.
 *
 * <p>Phân trang 0-based, đồng bộ với {@code JpaSchoolUserQueryRepository}.
 */
public interface ExamAppealQueryRepository {

    PageResult<AppealSummaryInfo> searchAppeals(UUID schoolId, String status, String keyword, int page, int size);

    AppealStatsInfo countByStatus(UUID schoolId);

    Optional<AppealDetailInfo> findDetailById(UUID appealId, UUID schoolId);

    PageResult<AppealTaskInfo> findTasksByReviewerId(UUID reviewerId, String status, int page, int size);

    Optional<AppealTaskDetailInfo> findTaskDetail(UUID appealId, UUID reviewerId);

    List<AppealReviewerLiteInfo> findAssignableReviewers(UUID schoolId, String keyword);
}
