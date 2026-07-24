package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppealReviewerInfo(
    UUID reviewerId,
    String reviewerName,
    String status,
    boolean done,
    OffsetDateTime assignedAt,
    OffsetDateTime submittedAt,
    /** Trung bình điểm đề xuất của các phần thi; null khi giám khảo chưa nộp. */
    BigDecimal suggestedScore,
    /** Rỗng khi giám khảo chưa nộp báo cáo. */
    List<AppealReviewerItemInfo> items
) {
}
