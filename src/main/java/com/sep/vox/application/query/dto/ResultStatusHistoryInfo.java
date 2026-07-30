package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Một mốc trong dòng thời gian điểm của một bài.
 *
 * <p>{@code actorName} là {@code null} khi hệ thống tự đổi (AI chấm xong, job chốt sổ)
 * — FE hiển thị "Hệ thống", không phải để trống.
 */
public record ResultStatusHistoryInfo(
    UUID id,
    UUID candidateResultId,
    String fromStatus,
    String toStatus,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter,
    String source,
    UUID actorId,
    String actorName,
    String reason,
    Instant createdAt
) {
}
