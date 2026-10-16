package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Xuất bảng điểm; phạm vi luôn khoá trong trường của người gọi.
 *
 * <p>Mang ĐÚNG bộ lọc của bảng điều phối
 * ({@link com.sep.vox.application.projection.dto.GradingAssignmentFilter}) trừ {@code schoolId} —
 * trường được giải quyết phía server từ phiên đăng nhập, nhận từ client là mở đường xuất
 * bảng điểm của trường khác. Người dùng lọc trên màn rồi bấm xuất thì file phải là đúng
 * cái họ đang nhìn, nên hai bên buộc phải cùng một bộ trường.
 *
 * @param assessmentKind {@code CENTRALIZED} / {@code CLASS_TEST}; null được hiểu là
 *                 {@code CENTRALIZED} qua {@link com.sep.vox.domain.service.assessment.GradingScopeKind}
 */
public record ExportAssessmentScoresQuery(
    UUID assessmentId,
    UUID scheduleId,
    UUID teacherId,
    String resultStatus,
    String roundType,
    String assignmentStatus,
    boolean unassignedOnly,
    boolean overdueOnly,
    Boolean hasOpenAppeal,
    String keyword,
    String assessmentKind
) {

    /** Chỉ có phạm vi, không lọc gì thêm — dùng cho test và cho lời gọi đơn giản. */
    public static ExportAssessmentScoresQuery scopedTo(UUID assessmentId, UUID scheduleId, String assessmentKind) {
        return new ExportAssessmentScoresQuery(
            assessmentId, scheduleId, null, null, null, null, false, false, null, null, assessmentKind);
    }
}
