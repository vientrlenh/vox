package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Kỳ thi tập trung (CENTRALIZED) gần với thời điểm hiện tại nhất của trường — dùng cho panel tổng
 * quan trên dashboard. {@code null} nếu trường chưa có kỳ thi tập trung nào (còn hiệu lực).
 */
public record NearestCentralizedExamDto(
    UUID examId,
    String code,
    String name,
    String status,
    Instant openAt,
    Instant closeAt,
    long totalCandidates,
    long absentCandidates
) {
}
