package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Read side cho chấm tay. Toàn bộ đều là join xuyên aggregate
 * (assignment x candidate_result x session x exam x users x evaluations), nên đi
 * lối query repository thay vì domain repository.
 *
 * <p>Phân trang 0-based, đồng bộ với {@code JpaExamAppealQueryRepository}.
 */
public interface ExamGradingQueryRepository {

    /** Bảng phân công của school admin: mọi bài PENDING_REVIEW, đã gán hay chưa. */
    PageResult<GradingAssignmentRowInfo> searchAssignments(
        UUID schoolId, UUID examId, UUID scheduleId, UUID teacherId, String status, String keyword,
        int page, int size);

    GradingStatsInfo countByStatus(UUID schoolId, UUID examId, UUID scheduleId);

    /** Hàng đợi của giáo viên. Ẩn danh — query này không đụng tới bảng học sinh. */
    PageResult<GradingTaskInfo> findTasksByTeacherId(UUID teacherId, String status, int page, int size);

    /** Màn chấm. Trả empty nếu người gọi không phải giáo viên được gán bài này. */
    Optional<GradingTaskDetailInfo> findTaskDetail(UUID assignmentId, UUID teacherId);

    /**
     * Màn chấm cho nhà trường: theo candidateResultId, không cần phân công (LEFT
     * JOIN), chỉ cần bài thuộc đúng trường. Cho phép nhà trường xem/chấm bất kỳ bài
     * PENDING_REVIEW nào của trường mình, kể cả chưa có ai được gán.
     */
    Optional<GradingTaskDetailInfo> findTaskDetailBySchool(UUID candidateResultId, UUID schoolId);

    List<AssignableTeacherInfo> findAssignableTeachers(UUID schoolId, String keyword);

    /**
     * Trong tập {@code userIds}, những ai là giáo viên ACTIVE thuộc {@code schoolId}.
     * Dùng để validate cả nhóm gán tay / auto-assign trong một query, thay cho việc
     * gọi {@code isTeacherOfSchool} lặp từng người (N+1).
     */
    Set<UUID> findTeacherIdsInSchool(UUID schoolId, Collection<UUID> userIds);

    /**
     * Số bài ASSIGNED mỗi giáo viên đang giữ. Auto-assign lấy đây làm điểm xuất
     * phát để lần chạy thứ hai không dồn hết vào người đầu danh sách.
     */
    Map<UUID, Long> assignedLoadByTeacherIds(Collection<UUID> teacherIds);

    /**
     * Bài đang PENDING_REVIEW và CHƯA có phân công, cho auto-assign. Lọc theo
     * exam hoặc schedule; bỏ qua bài đã gán để chạy lại nhiều lần vẫn an toàn.
     */
    List<UUID> findUnassignedPendingReviewResultIds(UUID schoolId, UUID examId, UUID scheduleId);
}
