package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * "AI chấm lệch bao nhiêu", đo bằng chính dữ liệu hậu kiểm đã có — không phải thu
 * thập gì thêm.
 *
 * <p>Mẫu số là các vòng {@code SPOT_CHECK} đã hoàn thành: giáo viên đã thật sự đọc
 * lại bài và ra kết luận. Vòng {@code INITIAL} không tính vì ở đó AI vốn chưa được
 * coi là chốt.
 *
 * @param upheld      số bài giáo viên xác nhận điểm AI đúng
 * @param regraded    số bài giáo viên phải sửa điểm
 * @param invalidated số bài hoá ra là vi phạm
 * @param averageDelta lệch tuyệt đối trung bình giữa điểm lúc giao và điểm sau khi
 *                     chấm lại; chỉ tính trên các bài {@code REGRADED}
 */
public record AiQualityReportInfo(
    int reviewed,
    int upheld,
    int regraded,
    int invalidated,
    BigDecimal regradeRate,
    BigDecimal averageDelta,
    BigDecimal maxDelta,
    List<ByTeacher> byTeacher
) {
    public record ByTeacher(
        UUID teacherId,
        String teacherName,
        int reviewed,
        int regraded,
        BigDecimal averageDelta
    ) {
    }
}
