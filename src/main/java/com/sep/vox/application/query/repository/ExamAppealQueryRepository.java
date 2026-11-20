package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Read side cho phúc khảo. Toàn bộ đều là join xuyên aggregate
 * (appeal × candidate_result × users × exams × paper_items × evaluations), nên đi
 * lối query repository thay vì domain repository.
 *
 * <p>Sau rework KHÔNG còn hàng đợi riêng của giám khảo: vòng phúc khảo là một dòng
 * {@code exam_grading_assignments} như ba vòng kia, nên giáo viên xem nó ở
 * {@code myGradingTasks} và chấm ở {@code gradingTaskDetail}. Repository này chỉ còn
 * phục vụ màn điều phối của school admin.
 *
 * <p>Phân trang 0-based, đồng bộ với {@code JpaSchoolUserQueryRepository}.
 */
public interface ExamAppealQueryRepository {

    /** {@code examId} null = toàn trường (màn của school admin); có giá trị = một bài kiểm tra. */
    PageResult<AppealSummaryInfo> searchAppeals(
        UUID schoolId, UUID examId, String status, String keyword, int page, int size);

    PageResult<AppealSummaryInfo> searchAppealsByStudentId(
        UUID studentId, String status, int page, int size);

    AppealStatsInfo countByStatus(UUID schoolId);

    Optional<AppealDetailInfo> findDetailById(UUID appealId, UUID schoolId);

    Optional<AppealDetailInfo> findStudentDetailById(UUID appealId, UUID studentId);

    /**
     * Ứng viên chấm phúc khảo cho MỘT đơn cụ thể, kèm cờ xung đột lợi ích.
     *
     * <p>Cần {@code appealId} chứ không chỉ {@code schoolId} như bản cũ: xung đột được
     * tính theo <em>bài thi của đơn đó</em>, không phải theo trường.
     */
    List<AppealReviewerLiteInfo> findAssignableReviewers(UUID schoolId, UUID appealId, String keyword);
}
