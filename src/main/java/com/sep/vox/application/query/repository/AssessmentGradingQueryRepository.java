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

    /** Bảng điều phối của school admin: mọi bài trong phạm vi lọc, gồm cả bài chưa gán. */
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
     * Những kỳ thi giáo viên này ĐANG hoặc ĐÃ có phân công chấm, kèm số bài mỗi kỳ.
     * Nguồn của bộ lọc kỳ thi trên hàng đợi giáo viên.
     *
     * <p>Không dùng lại lối đọc kỳ thi của trường: điều kiện hiển thị ở đó là system
     * admin / school admin / thành viên kỳ thi / kỳ thi đã đóng, mà giáo viên được giao
     * chấm không rơi vào ô nào — quyền của họ đến từ chính dòng phân công, nên phạm vi
     * dropdown cũng phải suy ra từ đó.
     *
     * <p>Không phân trang: tập này đóng theo số phân công của MỘT người, và dropdown
     * phân trang thì không tự dựng được nhãn cho mục đang chọn nếu nó rơi sang trang sau.
     */
    List<GradingAssessmentOptionInfo> findAssessmentsWithTasksByTeacherId(UUID teacherId, String assessmentKind);

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
     *
     * <p>CHỈ kỳ thi tập trung. Auto-assign là hành động của nhà trường, mà nhà trường
     * không phân công bài kiểm tra trên lớp — để lọt vào đây thì mỗi lần chạy đều gắp
     * lên một mớ bài rồi bị {@code rejectClassTestCoordination} chặn ở tầng dưới.
     */
    List<UUID> findAssignableResultIds(
        UUID schoolId, UUID assessmentId, UUID scheduleId, Collection<String> resultStatuses);

    /**
     * Tín hiệu rủi ro của một tập bài, một query cho cả tập. Xếp hạng làm ở
     * {@code GradingSampleSelector} chứ không ở SQL.
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
     * Ảnh chụp tình trạng chấm của một kỳ thi, để admin quyết trước khi chốt sổ.
     * Đây là lối thoát cho tình trạng một bài treo chặn cả kỳ thi (review BE-5).
     */
    BulkFinalizePreviewInfo previewBulkFinalize(UUID schoolId, UUID assessmentId);

    /**
     * Bảng điểm để xuất file. Không phân trang — xuất là xuất hết, nên người gọi phải
     * tự chốt phạm vi (kỳ thi hoặc ca thi) trước khi vào đây.
     *
     * <p>Nhận cùng {@link GradingAssignmentFilter} với {@link #searchAssignments} chứ
     * không phải ba tham số rời: file xuất ra phải là đúng cái người dùng đang nhìn trên
     * bảng, mà cách duy nhất bảo đảm điều đó là hai bên dùng chung một bộ vị ngữ.
     */
    List<AssessmentScoreRowInfo> findScoreRows(GradingAssignmentFilter filter);
}