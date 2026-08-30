package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamStatus;

/**
 * Một dòng của danh sách bài thi phía học sinh, đã lọc/sắp/phân trang xong ở SQL.
 *
 * <p>Gom sẵn ba aggregate (candidate × exam × schedule) vì use case cần cả ba để dựng response:
 * {@code exam} cho tên/mô tả/số lượt, {@code schedule} cho giờ thi và thời lượng, {@code candidate}
 * cho trạng thái vào phòng. Kéo riêng từng aggregate rồi ghép trong bộ nhớ chính là thứ vừa được
 * bỏ đi -- xem {@code StudentExamQueryRepository}.
 *
 * <p>{@code derivedStatus} và {@code examDate} là hai giá trị SUY RA, được tính ngay trong câu
 * truy vấn: chúng vừa là điều kiện lọc vừa là khoá sắp xếp, nên không thể tính sau khi phân trang.
 */
public record StudentExamRowInfo(
    UUID candidateId,
    ExamCandidateStatus candidateStatus,
    Instant blockedAt,
    UUID assignedPaperId,
    UUID examId,
    String examName,
    String examDescription,
    String examKind,
    ExamStatus examStatus,
    boolean requiresOtp,
    Integer maxAttempt,
    UUID scheduleId,
    Instant scheduleStartDate,
    Instant scheduleEndDate,
    Instant examDate,
    String derivedStatus
) {
}
