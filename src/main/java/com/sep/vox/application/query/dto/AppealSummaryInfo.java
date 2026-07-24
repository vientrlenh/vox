package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    OffsetDateTime requestedAt,
    OffsetDateTime deadline,
    int reviewerCount,
    int doneCount,
    boolean overdue
) {
}
