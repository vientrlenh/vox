package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Read side cho chấm tay. Toàn bộ đều là join xuyên aggregate
 * (assignment × candidate_result × session × exam × users × evaluations), nên đi lối
 * query repository thay vì domain repository.
 *
 * <p>Sau rework <strong>không</strong> còn ràng buộc cứng "chỉ PENDING_REVIEW": admin
 * điều phối cả bốn vòng nên phải thấy bài ở mọi trạng thái. Phạm vi được thu hẹp bằng
 * {@link GradingAssignmentFilter}, không bằng hằng số ẩn trong repository.
 *
 * <p>Phân trang 0-based, đồng bộ với {@code JpaExamAppealQueryRepository}.
 */
public interface ExamGradingQueryRepository {

    /** Bảng điều phối của school admin: mọi bài trong phạm vi lọc, gồm cả bài chưa gán. */
    PageResult<GradingAssignmentRowInfo> searchAssignments(GradingAssignmentFilter filter, int page, int size);

    GradingStatsInfo stats(UUID schoolId, UUID examId, UUID scheduleId);

    /** Hàng đợi của giáo viên — một danh sách cho cả bốn vòng. Ẩn danh. */
    PageResult<GradingTaskInfo> findTasksByTeacherId(
            UUID teacherId, String status, String roundType, int page, int size);

    PageResult<GradingTaskInfo> findTasksByTeacherIdAndExamId(
            UUID teacherId, UUID examId, String status, String roundType, int page, int size);

    /** Màn chấm. Trả empty nếu người gọi không phải giáo viên được gán bài này. */
    Optional<GradingTaskDetailInfo> findTaskDetail(UUID assignmentId, UUID teacherId);

    List<AssignableTeacherInfo> findAssignableTeachers(UUID schoolId, String keyword);

    /**
     * Trong tập {@code userIds}, những ai là giáo viên ACTIVE thuộc {@code schoolId}.
     * Dùng để validate cả nhóm gán tay / auto-assign trong một query, thay cho việc
     * gọi {@code isTeacherOfSchool} lặp từng người (N+1).
     */
    Set<UUID> findTeacherIdsInSchool(UUID schoolId, Collection<UUID> userIds);

    /**
     * Số phân công ĐANG MỞ mỗi giáo viên đang giữ, không phân biệt vòng. Auto-assign
     * lấy đây làm điểm xuất phát để lần chạy thứ hai không dồn hết vào người đầu.
     */
    Map<UUID, Long> assignedLoadByTeacherIds(Collection<UUID> teacherIds);

    /**
     * Bài đủ điều kiện cho một vòng chấm và CHƯA có phân công đang mở.
     *
     * <p>Trạng thái hợp lệ do {@code GradingRoundPolicy} quyết, truyền vào đây — luật
     * nghiệp vụ không nằm trong SQL. Bài đang có đơn phúc khảo mở bị loại ở mọi vòng
     * khác {@code APPEAL}: hai luồng cùng ghi điểm một bài là nguồn của review BE-4.
     */
    List<UUID> findAssignableResultIds(
            UUID schoolId, UUID examId, UUID scheduleId, Collection<String> resultStatuses);

    /**
     * Tín hiệu rủi ro của một tập bài, một query cho cả tập. Xếp hạng làm ở
     * {@code GradingSampleSelector} chứ không ở SQL.
     */
    List<GradingRiskInfo> findRiskInfos(Collection<UUID> candidateResultIds);

    /**
     * Những giáo viên đã từng ghi {@code ExamItemEvaluation} engine HUMAN cho bài này
     * — tức đã ra phán quyết điểm riêng và không còn vô tư để chấm phúc khảo.
     *
     * <p>Người chỉ {@code UPHOLD} (xác nhận điểm cũ, không ghi bản mới) KHÔNG nằm
     * trong tập này: họ chưa có phán quyết riêng nào để phải tự bảo vệ.
     */
    Set<UUID> findTeacherIdsWithHumanEvaluation(UUID candidateResultId);

    /**
     * Ảnh chụp tình trạng chấm của một kỳ thi, để admin quyết trước khi chốt sổ.
     * Đây là lối thoát cho tình trạng một bài treo chặn cả kỳ thi (review BE-5).
     */
    BulkFinalizePreviewInfo previewBulkFinalize(UUID schoolId, UUID examId);

    /** Bảng điểm đầy đủ của một kỳ thi để xuất CSV. Không phân trang — xuất là xuất hết. */
    List<ExamScoreRowInfo> findScoreRows(UUID schoolId, UUID examId, UUID scheduleId);
}