package com.sep.vox.application.projection.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;

/**
 * Một dòng của danh sách bài thi phía học sinh, đã lọc/sắp/phân trang xong ở SQL.
 *
 * <p>Gom sẵn ba aggregate (candidate × assessment × schedule) vì use case cần cả ba để dựng response:
 * {@code assessment} cho tên/mô tả/số lượt, {@code schedule} cho giờ thi và thời lượng, {@code candidate}
 * cho trạng thái vào phòng. Kéo riêng từng aggregate rồi ghép trong bộ nhớ chính là thứ vừa được
 * bỏ đi -- xem {@code StudentAssessmentQueryRepository}.
 *
 * <p>{@code derivedStatus} và {@code assessmentDate} là hai giá trị SUY RA, được tính ngay trong câu
 * truy vấn: chúng vừa là điều kiện lọc vừa là khoá sắp xếp, nên không thể tính sau khi phân trang.
 */
public record StudentAssessmentRowInfo(
    UUID candidateId,
    AssessmentCandidateStatus candidateStatus,
    Instant blockedAt,
    UUID assignedPaperId,
    UUID assessmentId,
    String assessmentName,
    String assessmentDescription,
    String assessmentKind,
    AssessmentStatus assessmentStatus,
    boolean requiresOtp,
    Integer maxAttempt,
    UUID scheduleId,
    Instant scheduleStartDate,
    Instant scheduleEndDate,
    Instant assessmentDate,
    String derivedStatus
) {
}
