package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppealSummaryInfo(
    UUID id,
    String studentName,
    String className,
    String examName,
    /** Nhãn các phần thi được phúc khảo; FE tự quyết cách hiển thị (chip, "+2 more"…). */
    List<String> partLabels,
    BigDecimal originalScore,
    String status,
    Instant requestedAt,
    Instant deadline,
    /** Người chấm duy nhất; null khi chưa phân công. */
    String reviewerName,
    /** ASSIGNED | COMPLETED; null khi chưa phân công. */
    String reviewerStatus,
    boolean overdue
) {
}
